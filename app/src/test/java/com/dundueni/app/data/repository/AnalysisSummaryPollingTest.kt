package com.dundueni.app.data.repository

import androidx.lifecycle.ViewModelStore
import com.dundueni.app.data.remote.api.AnalysisApiService
import com.dundueni.app.data.remote.api.AnalysisQueryApiService
import com.dundueni.app.data.remote.dto.AnalysisDetailDto
import com.dundueni.app.data.remote.dto.AnalysisDetailResponseDto
import com.dundueni.app.data.remote.dto.AnalysisSummaryDto
import com.dundueni.app.data.remote.dto.AnalysisSummaryResponseDto
import com.dundueni.app.data.remote.dto.NormalizedAnalysisResultDto
import com.dundueni.app.feature.analysis.AnalysisUiState
import com.dundueni.app.feature.analysis.AnalysisViewModel
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisSummaryPollingTest {
    private val id = "9dca5a1c-9736-43b1-9b7c-fad6b9ce7e72"

    @Test fun pendingCompletesAfterTwoSecondsWithoutAnotherAnalysisRequest() = runTest {
        val api = SummaryApi { if (it == 1) "PENDING" else "COMPLETED" }
        val repository = repository(api)
        val result = async { repository.findResult(id) }
        runCurrent()
        assertEquals(1, api.summaryCalls)
        advanceTimeBy(1_999)
        runCurrent()
        assertEquals(1, api.summaryCalls)
        advanceTimeBy(1)
        runCurrent()
        assertPreserved(result.await(), "Gemini 요약")
        advanceUntilIdle()
        assertEquals(2, api.summaryCalls)
        assertEquals(1, api.detailCalls)
        assertEquals(0, api.postCalls)
    }

    @Test fun pendingThenFailedKeepsAnalysisAndStops() = runTest {
        val api = SummaryApi { if (it == 1) "PENDING" else "FAILED" }
        val result = async { repository(api).findResult(id) }
        advanceUntilIdle()
        assertPreserved(result.await(), null)
        assertEquals(2, api.summaryCalls)
    }

    @Test fun pendingStopsAfterFiveRetries() = runTest {
        val api = SummaryApi { "PENDING" }
        val result = async { repository(api).findResult(id) }
        advanceUntilIdle()
        assertPreserved(result.await(), null)
        assertEquals(6, api.summaryCalls)
        assertEquals(10_000L, testScheduler.currentTime)
        assertEquals(1, api.detailCalls)
        assertEquals(0, api.postCalls)
    }

    @Test fun networkFailureDuringRetryKeepsAnalysisAndStops() = runTest {
        val api = SummaryApi { if (it == 1) "PENDING" else throw IOException("offline") }
        val result = async { repository(api).findResult(id) }
        advanceUntilIdle()
        assertPreserved(result.await(), null)
        assertEquals(2, api.summaryCalls)
    }

    @Test fun concurrentSameIdSharesPollingAndCancellingWaiterKeepsOwnerAlive() = runTest {
        val api = SummaryApi { if (it == 1) "PENDING" else "COMPLETED" }
        val repository = repository(api)
        val owner = async { repository.findResult(id) }
        runCurrent()
        val waiter = async { repository.findResult(id) }
        val otherWaiter = async { repository.findResult(id) }
        runCurrent()
        assertEquals(1, api.summaryCalls)
        waiter.cancel()
        advanceUntilIdle()
        assertPreserved(owner.await(), "Gemini 요약")
        assertEquals(owner.await(), otherWaiter.await())
        assertEquals(2, api.summaryCalls)
        assertEquals(0, api.postCalls)
    }

    @Test fun cancellationStopsPollingAndReleasesSameId() = runTest {
        val api = SummaryApi { if (it == 1) "PENDING" else "COMPLETED" }
        val repository = repository(api)
        val request = async { repository.findResult(id) }
        runCurrent()
        request.cancel()
        advanceUntilIdle()
        assertTrue(request.isCancelled)
        assertEquals(1, api.summaryCalls)
        assertPreserved(repository.findResult(id), "Gemini 요약")
        assertEquals(2, api.summaryCalls)
    }

    @Test fun clearingViewModelCancelsPendingSummary() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val api = SummaryApi { "PENDING" }
            val repository = repository(api)
            val vm = AnalysisViewModel(AnalysisResultRepository { repository.findResult(id) })
            store.put("analysis", vm)
            vm.analyze(MultipartBody.Part.createFormData("file", "image.png", byteArrayOf(1).toRequestBody()))
            runCurrent()
            assertEquals(1, api.summaryCalls)
            store.clear()
            advanceUntilIdle()
            assertEquals(1, api.summaryCalls)
            assertFalse(vm.uiState.value is AnalysisUiState.Error)
            assertEquals(0, api.postCalls)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    private fun TestScope.repository(api: SummaryApi) = AnalysisRepository(
        api, api, ioDispatcher = StandardTestDispatcher(testScheduler),
    )

    private fun assertPreserved(result: com.dundueni.app.data.model.AnalysisResult, summary: String?) {
        assertEquals(id, result.analysisId)
        assertEquals("COMPLETED", result.status)
        assertEquals("UNKNOWN", result.normalizedRiskLevel)
        assertEquals(listOf("기존 근거"), result.reasons)
        assertEquals(listOf("기존 안내"), result.recommendedActions)
        assertEquals(82.0, result.aiGenerationScore!!, 0.0)
        assertEquals(summary, result.summary)
    }

    private inner class SummaryApi(private val status: (Int) -> String) : AnalysisApiService, AnalysisQueryApiService {
        var summaryCalls = 0
        var detailCalls = 0
        var postCalls = 0

        override suspend fun analyzeImage(image: MultipartBody.Part, type: RequestBody): Response<ResponseBody> {
            postCalls++
            error("Summary polling must not submit analysis")
        }

        override suspend fun getAnalysis(analysisId: String): Response<AnalysisDetailResponseDto> {
            detailCalls++
            return Response.success(AnalysisDetailResponseDto(
                isSuccess = true, code = "ANALYSIS200",
                result = AnalysisDetailDto(
                    analysisId = analysisId, type = "IMAGE", status = "COMPLETED", aiGenerationScore = 82.0,
                    result = NormalizedAnalysisResultDto(
                        riskLevel = "UNKNOWN", reasons = listOf("기존 근거"), guidance = listOf("기존 안내"),
                    ),
                ),
            ))
        }

        override suspend fun getSummary(analysisId: String): Response<AnalysisSummaryResponseDto> {
            summaryCalls++
            return Response.success(AnalysisSummaryResponseDto(
                isSuccess = true, code = "ANALYSIS200",
                result = AnalysisSummaryDto(
                    analysisId = analysisId, summaryStatus = status(summaryCalls), summary = "Gemini 요약",
                ),
            ))
        }
    }
}
