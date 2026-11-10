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

    @Test fun pendingRefreshReusesPostIdWithoutAnotherUpload() = runTest {
        var posts = 0
        var gets = 0
        val repository = object : AnalysisResultRepository {
            override suspend fun analyzeResult(image: MultipartBody.Part): AnalysisResult {
                posts++
                throw AnalysisNotCompletedException(AnalysisExecutionStatus.QUEUED, "id-1", null)
            }
            override suspend fun findResult(analysisId: String): AnalysisResult {
                assertEquals("id-1", analysisId)
                gets++
                if (gets == 1) throw AnalysisNotCompletedException(AnalysisExecutionStatus.ANALYZING, analysisId, null)
                return result
            }
        }
        val vm = viewModel(repository)
        vm.analyze(part)
        runCurrent()
        assertEquals(AnalysisUiState.Pending("id-1", AnalysisExecutionStatus.QUEUED), vm.uiState.value)
        vm.refreshPending()
        runCurrent()
        assertEquals(AnalysisUiState.Pending("id-1", AnalysisExecutionStatus.ANALYZING), vm.uiState.value)
        vm.refreshPending()
        runCurrent()
        assertEquals(AnalysisUiState.Success(result), vm.uiState.value)
        assertEquals(1, posts)
        assertEquals(2, gets)
    }

    @Test fun failedLookupRetryStillUsesOriginalId() = runTest {
        var posts = 0
        var gets = 0
        val repository = object : AnalysisResultRepository {
            override suspend fun analyzeResult(image: MultipartBody.Part): AnalysisResult {
                posts++
                throw AnalysisNotCompletedException(AnalysisExecutionStatus.QUEUED, "id-1", null)
            }
            override suspend fun findResult(analysisId: String): AnalysisResult {
                assertEquals("id-1", analysisId)
                gets++
                if (gets == 1) throw ConnectException("offline")
                return result
            }
        }
        val vm = viewModel(repository)
        vm.analyze(part)
        runCurrent()
        vm.refreshPending()
        runCurrent()
        assertEquals(
            AnalysisUiState.Error(AnalysisErrorKind.CONNECTION, pendingAnalysisId = "id-1"),
            vm.uiState.value,
        )
        vm.refreshPending()
        runCurrent()
        assertEquals(AnalysisUiState.Success(result), vm.uiState.value)
        assertEquals(1, posts)
        assertEquals(2, gets)
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
            401 to "로그인이 필요합니다.",
            404 to "분석 결과를 찾을 수 없거나 만료되었습니다.",
            500 to "분석 서버 오류가 발생했습니다.",
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
    @Test fun initialCompletedPostLookupFailureRetainsIdForRetry() = runTest {
        var posts = 0
        var gets = 0
        val vm = viewModel(object : AnalysisResultRepository {
            override suspend fun analyzeResult(image: MultipartBody.Part): AnalysisResult {
                posts++
                throw com.dundueni.app.data.repository.AnalysisLookupException("id-1", ConnectException("offline"))
            }
            override suspend fun findResult(analysisId: String): AnalysisResult {
                gets++
                assertEquals("id-1", analysisId)
                return result
            }
        })
        vm.analyze(part); runCurrent()
        assertEquals(AnalysisUiState.Error(AnalysisErrorKind.CONNECTION, pendingAnalysisId = "id-1"), vm.uiState.value)
        vm.refreshPending(); runCurrent()
        assertEquals(AnalysisUiState.Success(result), vm.uiState.value)
        assertEquals(1, posts); assertEquals(1, gets)
    }

    @Test fun screenPartialSuccessKeepsCompletedSideAndRetriesOnlyPendingGet() = runTest {
        for (imageSucceeds in listOf(true, false)) {
            var imagePosts = 0
            var textPosts = 0
            val gets = mutableListOf<String>()
            var refreshing = false
            val text = result.copy(analysisId = "ocr-id", type = "TEXT", sourceType = "SCREEN_CROP",
                textDecision = "phishing", summary = "OCR GET summary", reasons = listOf("OCR GET reason"))
            val repository = object : AnalysisResultRepository {
                override suspend fun analyzeResult(image: MultipartBody.Part): AnalysisResult {
                    imagePosts++
                    if (!imageSucceeds) throw com.dundueni.app.data.repository.AnalysisLookupException("id-1", ConnectException("offline"))
                    return result
                }
                override suspend fun findResult(analysisId: String): AnalysisResult {
                    gets += analysisId
                    if (!refreshing && imageSucceeds) throw AnalysisNotCompletedException(AnalysisExecutionStatus.ANALYZING, analysisId, null)
                    return if (analysisId == "ocr-id") text else result
                }
            }
            val api = object : com.dundueni.app.data.remote.api.TextAnalysisApiService {
                override suspend fun analyzeText(request: com.dundueni.app.data.remote.api.TextAnalysisRequest): retrofit2.Response<com.dundueni.app.data.remote.api.TextAnalysisResponse> = error("Unexpected direct text")
                override suspend fun analyzeScreenCrop(file: MultipartBody.Part, type: okhttp3.RequestBody, sourceType: okhttp3.RequestBody): retrofit2.Response<com.dundueni.app.data.remote.api.TextAnalysisResponse> {
                    textPosts++
                    return retrofit2.Response.success(202, com.dundueni.app.data.remote.api.TextAnalysisResponse(
                        true, "ANALYSIS202", com.dundueni.app.data.remote.api.TextAnalysisResponseResult("ocr-id", "TEXT", "COMPLETED")))
                }
            }
            val vm = AnalysisViewModel(repository) { api }.also { store.put("screen", it) }
            vm.analyzePrepared(true) { part }; runCurrent()
            val partial = vm.uiState.value as AnalysisUiState.ScreenSuccess
            if (imageSucceeds) {
                assertEquals(result, partial.image); assertNull(partial.text)
                assertEquals("ocr-id", partial.textError?.pendingAnalysisId)
            } else {
                assertNull(partial.image); assertEquals("OCR GET summary", partial.text?.detail?.summary)
                assertEquals("id-1", partial.imageError?.pendingAnalysisId)
            }
            refreshing = true
            vm.refreshPending(); runCurrent()
            val complete = vm.uiState.value as AnalysisUiState.ScreenSuccess
            assertEquals(result, complete.image)
            assertEquals(text, complete.text?.detail)
            assertNull(complete.imageError); assertNull(complete.textError)
            assertEquals(1, imagePosts); assertEquals(1, textPosts)
            assertEquals(if (imageSucceeds) listOf("ocr-id", "ocr-id") else listOf("ocr-id", "id-1"), gets)
        }
    }

}
