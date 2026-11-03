package com.dundueni.app.feature.analysis

import androidx.lifecycle.ViewModelStore
import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.data.repository.AnalysisResultRepository
import com.dundueni.app.data.remote.api.AnalysisHttpException
import com.dundueni.app.data.remote.api.AnalysisProtocolException
import com.dundueni.app.data.repository.AnalysisFailedException
import com.dundueni.app.data.repository.AnalysisNotCompletedException
import com.dundueni.app.data.repository.AnalysisExecutionStatus
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val part = MultipartBody.Part.createFormData("image", "test.png", byteArrayOf(1).toRequestBody())
    private val result = AnalysisResult(
        analysisId = "id-1", type = "IMAGE", status = "COMPLETED",
        aiGenerationScore = 82.4, aiRiskLevel = "SERVER_DEFINED_VALUE",
        reasons = listOf("근거"), modelVersion = null, errorCode = null,
        createdAt = null, completedAt = null, expiresAt = null
    )

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun viewModel(repository: AnalysisResultRepository) = AnalysisViewModel(repository).also {
        store.put("analysis", it)
    }

    @Test fun idleLoadingSuccessAndDuplicateIgnored() = runTest {
        val pending = CompletableDeferred<AnalysisResult>()
        var calls = 0
        val vm = viewModel { image -> calls++; assertSame(part, image); pending.await() }
        val states = mutableListOf<AnalysisUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.toList(states) }
        assertEquals(AnalysisUiState.Idle, vm.uiState.value)
        vm.analyze(part)
        vm.analyze(part)
        runCurrent()
        assertEquals(AnalysisUiState.Loading, vm.uiState.value)
        vm.analyze(part)
        runCurrent()
        assertEquals(1, calls)
        pending.complete(result)
        runCurrent()
        assertEquals(listOf(AnalysisUiState.Idle, AnalysisUiState.Loading, AnalysisUiState.Success(result)), states)
        vm.analyze(part)
        runCurrent()
        assertEquals(2, calls)
    }

    @Test fun idleLoadingErrorAndRetrySuccess() = runTest {
        val pending = CompletableDeferred<AnalysisResult>()
        var calls = 0
        val vm = viewModel { if (++calls == 1) pending.await() else result }
        val states = mutableListOf<AnalysisUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.toList(states) }
        vm.analyze(part)
        runCurrent()
        pending.completeExceptionally(IOException("private server details"))
        runCurrent()
        assertEquals(listOf(AnalysisUiState.Idle, AnalysisUiState.Loading,
            AnalysisUiState.Error(AnalysisErrorKind.CONNECTION)), states)
        vm.analyze(part)
        runCurrent()
        assertEquals(AnalysisUiState.Success(result), vm.uiState.value)
    }

    @Test fun unexpectedExceptionProducesError() = runTest {
        val vm = viewModel { throw IllegalArgumentException("invalid response") }
        vm.analyze(part)
        runCurrent()
        assertEquals(AnalysisUiState.Error(), vm.uiState.value)
    }

    @Test fun preparationExceptionProducesErrorWithoutCallingRepository() = runTest {
        var calls = 0
        val vm = viewModel { calls++; result }
        val states = mutableListOf<AnalysisUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.toList(states) }
        vm.analyzePrepared { throw IllegalStateException("image preparation failed") }
        runCurrent()
        assertEquals(0, calls)
        assertEquals(listOf(AnalysisUiState.Idle, AnalysisUiState.Loading,
            AnalysisUiState.Error(AnalysisErrorKind.INPUT)), states)
    }

    @Test fun distinguishesFailuresWithoutExposingPrivateServerMessages() = runTest {
        val failures = listOf(
            ConnectException("private host") to AnalysisUiState.Error(AnalysisErrorKind.CONNECTION),
            SocketTimeoutException("private host") to AnalysisUiState.Error(AnalysisErrorKind.TIMEOUT),
            java.io.InterruptedIOException("call timeout") to AnalysisUiState.Error(AnalysisErrorKind.TIMEOUT),
            AnalysisHttpException(503, "INTERNAL", "private details") to AnalysisUiState.Error(AnalysisErrorKind.HTTP, 503),
            AnalysisProtocolException("private JSON") to AnalysisUiState.Error(AnalysisErrorKind.RESPONSE),
            AnalysisApiConfigurationException("private URL") to AnalysisUiState.Error(AnalysisErrorKind.CONFIGURATION),
            AnalysisFailedException("INTERNAL", "private details") to AnalysisUiState.Error(AnalysisErrorKind.SERVER),
        )
        for ((failure, expected) in failures) {
            val vm = viewModel { throw failure }
            vm.analyze(part)
            runCurrent()
            assertEquals(expected, vm.uiState.value)
            assertFalse(vm.uiState.value.toString().contains("private"))
        }
    }

    @Test fun queuedAndAnalyzingPreservePendingAndIgnoreRetry() = runTest {
        for (status in listOf(AnalysisExecutionStatus.QUEUED, AnalysisExecutionStatus.ANALYZING)) {
            var calls = 0
            var preparations = 0
            val vm = viewModel {
                calls++
                throw AnalysisNotCompletedException(status, "id-1", "private details")
            }
            val states = mutableListOf<AnalysisUiState>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.toList(states) }
            vm.analyze(part)
            runCurrent()
            val pending = AnalysisUiState.Pending("id-1", status)
            assertEquals(listOf(AnalysisUiState.Idle, AnalysisUiState.Loading, pending), states)
            val attemptId = vm.attemptId
            vm.analyze(part)
            vm.analyzePrepared { preparations++; part }
            runCurrent()
            assertEquals(1, calls)
            assertEquals(0, preparations)
            assertEquals(attemptId, vm.attemptId)
            assertEquals(pending, vm.uiState.value)
            assertFalse(vm.uiState.value.toString().contains("private"))
        }
    }

    @Test fun pendingWithoutValidAnalysisIdProducesResponseError() = runTest {
        for (status in listOf(AnalysisExecutionStatus.QUEUED, AnalysisExecutionStatus.ANALYZING)) {
            for (id in listOf(null, "", " \t\n")) {
                val vm = viewModel { throw AnalysisNotCompletedException(status, id, null) }
                vm.analyze(part)
                runCurrent()
                assertEquals(AnalysisUiState.Error(AnalysisErrorKind.RESPONSE), vm.uiState.value)
            }
        }
    }

    @Test fun httpErrorsUseSafeStatusSpecificMessagesAndFallback() = runTest {
        val messages = mapOf(
            400 to "잘못된 분석 요청입니다.",
            409 to "분석 요청이 충돌했습니다.",
            410 to "분석 세션이 만료되었습니다.",
            413 to "이미지 크기가 너무 큽니다.",
            415 to "지원하지 않는 이미지 형식입니다.",
            429 to "분석 이용량을 초과했습니다.",
            503 to "현재 분석 서버를 이용할 수 없습니다.",
            401 to AnalysisErrorKind.HTTP.message,
            500 to AnalysisErrorKind.HTTP.message,
        )
        for ((status, message) in messages) {
            val vm = viewModel { throw AnalysisHttpException(status, "INTERNAL", "private details") }
            vm.analyze(part)
            runCurrent()
            val error = vm.uiState.value as AnalysisUiState.Error
            assertEquals(AnalysisUiState.Error(AnalysisErrorKind.HTTP, status), error)
            assertEquals(message, error.message)
            assertFalse(error.message.contains("private"))
        }
        for (kind in AnalysisErrorKind.entries.filter { it != AnalysisErrorKind.HTTP }) {
            assertEquals(kind.message, AnalysisUiState.Error(kind).message)
        }
    }

    @Test fun unknownServerRiskValueIsPreservedAsSuccess() = runTest {
        val futureRiskResult = result.copy(aiRiskLevel = "FUTURE_SERVER_VALUE")
        val vm = viewModel { futureRiskResult }
        vm.analyze(part)
        runCurrent()
        assertEquals(AnalysisUiState.Success(futureRiskResult), vm.uiState.value)
    }

    @Test fun cancellationReturnsIdleAndAllowsRetry() = runTest {
        val pending = CompletableDeferred<AnalysisResult>()
        var calls = 0
        val vm = viewModel { if (++calls == 1) pending.await() else result }
        vm.analyze(part)
        runCurrent()
        pending.cancel()
        runCurrent()
        assertEquals(AnalysisUiState.Idle, vm.uiState.value)
        vm.analyze(part)
        runCurrent()
        assertEquals(AnalysisUiState.Success(result), vm.uiState.value)
    }

    @Test fun clearingViewModelCancelsRequestWithoutLateSuccess() = runTest {
        val pending = CompletableDeferred<AnalysisResult>()
        var cancelled = false
        val vm = viewModel { try { pending.await() } finally { cancelled = true } }
        vm.analyze(part)
        runCurrent()
        store.clear()
        runCurrent()
        assertTrue(cancelled)
        assertEquals(AnalysisUiState.Idle, vm.uiState.value)
        pending.complete(result)
        runCurrent()
        assertEquals(AnalysisUiState.Idle, vm.uiState.value)
    }
}
