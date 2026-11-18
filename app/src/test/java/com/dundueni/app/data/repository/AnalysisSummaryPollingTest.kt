package com.dundueni.app.data.repository

import com.dundueni.app.data.mapper.toDomain
import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.data.remote.dto.AnalysisDetailDto
import com.dundueni.app.data.remote.dto.AnalysisImageModelsDto
import com.dundueni.app.data.remote.dto.AnalysisModelResultDto
import com.dundueni.app.data.remote.dto.AnalysisReasonDto
import com.dundueni.app.data.remote.dto.TextAnalysisTextResultDto
import com.dundueni.app.feature.analysis.AnalysisUiState
import com.dundueni.app.feature.analysis.toPresentedResult
import com.dundueni.app.feature.analysis.toScreenCropAnalysis
import com.dundueni.app.feature.analysisresult.ImageSummaryViewModel
import com.dundueni.app.feature.analysisresult.imageSummaryLookupId
import com.dundueni.app.feature.history.HistoryType
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import okhttp3.MultipartBody
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisSummaryPollingTest {
    private val analysisId = UUID.randomUUID().toString()

    @Test fun unregisteredThenPendingThenCompletedPublishesSummaryAndStops() = runTest {
        val repository = SummaryRepository {
            when (it) {
                1, 2 -> ImageSummaryResult.Unavailable
                3 -> ImageSummaryResult.Pending
                else -> completed
            }
        }
        val model = ImageSummaryViewModel.forResult(analysisId, repository)
        val visible = launch { model.refreshWhileVisible() }
        runCurrent()
        assertEquals(ImageSummaryResult.Unavailable, model.uiState.value.result)
        assertFalse(model.uiState.value.showRetry)
        advanceTimeBy(14_999)
        runCurrent()
        assertEquals(3, repository.ids.size)
        assertEquals(ImageSummaryResult.Pending, model.uiState.value.result)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(completed, model.uiState.value.result)
        assertTrue(visible.isCompleted)
        advanceTimeBy(60_000)
        assertEquals(List(4) { analysisId }, repository.ids)
    }

    @Test fun unregisteredStopsAfterTwelveAndOffersManualLookup() = runTest {
        assertExhaustionAndManualLookup(ImageSummaryResult.Unavailable)
    }

    @Test fun pendingStopsAfterTwelveAndOffersManualLookup() = runTest {
        assertExhaustionAndManualLookup(ImageSummaryResult.Pending)
    }

    private suspend fun TestScope.assertExhaustionAndManualLookup(waiting: ImageSummaryResult) {
        val repository = SummaryRepository {
            when (it) {
                13 -> { delay(1_000); ImageSummaryResult.Unavailable }
                14 -> { delay(1_000); completed }
                else -> waiting
            }
        }
        val model = ImageSummaryViewModel.forResult(analysisId, repository)
        val visible = launch { model.refreshWhileVisible() }
        advanceTimeBy(55_000)
        runCurrent()
        assertEquals(12, repository.ids.size)
        assertEquals(waiting, model.uiState.value.result)
        assertTrue(model.uiState.value.showRetry)
        assertFalse(model.uiState.value.checking)
        advanceTimeBy(60_000)
        assertEquals(12, repository.ids.size)

        model.checkAgain()
        model.checkAgain()
        runCurrent()
        assertTrue(model.uiState.value.checking)
        assertEquals(13, repository.ids.size)
        advanceTimeBy(1_000)
        runCurrent()
        assertTrue(model.uiState.value.showRetry)
        assertFalse(model.uiState.value.checking)
        advanceTimeBy(60_000)
        assertEquals(13, repository.ids.size)

        model.checkAgain()
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(completed, model.uiState.value.result)
        assertFalse(model.uiState.value.showRetry)
        assertTrue(visible.isCompleted)
        assertEquals(14, repository.ids.size)
    }

    @Test fun leavingResultCancelsAnInFlightLookupAndResumeKeepsSpentAttempts() = runTest {
        var cancelled = false
        val repository = SummaryRepository {
            if (it == 2) {
                try {
                    delay(60_000)
                } finally {
                    cancelled = true
                }
            }
            if (it < 4) ImageSummaryResult.Unavailable else ImageSummaryResult.Pending
        }
        val model = ImageSummaryViewModel.forResult(analysisId, repository)
        val firstVisit = launch { model.refreshWhileVisible() }
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(2, repository.ids.size)
        firstVisit.cancel()
        runCurrent()
        assertTrue(cancelled)
        advanceTimeBy(60_000)
        assertEquals(2, repository.ids.size)
        val reopened = ImageSummaryViewModel.forResult(analysisId, repository)
        val resumed = launch { reopened.refreshWhileVisible() }
        advanceTimeBy(50_000)
        runCurrent()
        assertEquals(12, repository.ids.size)
        assertTrue(reopened.uiState.value.showRetry)
        resumed.cancel()
        runCurrent()
        val afterExhaustion = ImageSummaryViewModel.forResult(analysisId, repository)
        val reentered = launch { afterExhaustion.refreshWhileVisible() }
        advanceTimeBy(60_000)
        assertEquals(12, repository.ids.size)
        assertTrue(afterExhaustion.uiState.value.showRetry)
        reentered.cancel()
    }

    @Test fun failureIsTerminalAndDoesNotOfferRetry() = runTest {
        val repository = SummaryRepository { ImageSummaryResult.Failed }
        val model = ImageSummaryViewModel.forResult(analysisId, repository)
        val visible = launch { model.refreshWhileVisible() }
        runCurrent()
        assertEquals(ImageSummaryResult.Failed, model.uiState.value.result)
        assertFalse(model.uiState.value.showRetry)
        assertTrue(visible.isCompleted)
        model.checkAgain()
        advanceTimeBy(60_000)
        assertEquals(1, repository.ids.size)
    }

    @Test fun failedLookupAfterConfirmedPendingStopsAutoAndAllowsManualRecovery() = runTest {
        val repository = SummaryRepository {
            when (it) {
                1 -> ImageSummaryResult.Pending
                2 -> ImageSummaryResult.LookupError
                else -> completed
            }
        }
        val model = ImageSummaryViewModel.forResult(analysisId, repository)
        val visible = launch { model.refreshWhileVisible() }
        advanceTimeBy(5_000)
        runCurrent()
        assertTrue(model.uiState.value.lookupFailed)
        assertTrue(model.uiState.value.showRetry)
        advanceTimeBy(60_000)
        assertEquals(2, repository.ids.size)
        model.checkAgain()
        runCurrent()
        assertEquals(completed, model.uiState.value.result)
        assertTrue(visible.isCompleted)
    }

    @Test fun imageAndScreenUseImageIdAndNeverUseOcrResultId() {
        val image = AnalysisDetailDto(
            analysisId = "image-id", type = "IMAGE", status = "COMPLETED",
            models = AnalysisImageModelsDto(generation = AnalysisModelResultDto(
                status = "SUCCEEDED", decision = "AI_GENERATED_SUSPECTED", displayScore = 80.0,
                reasons = listOf(AnalysisReasonDto("이미지 근거")), limitations = listOf("이미지 한계"),
            )),
        ).toDomain()
        val text = AnalysisDetailDto(
            analysisId = "ocr-id", type = "TEXT", status = "COMPLETED",
            textResult = TextAnalysisTextResultDto(
                sourceType = "SCREEN_CROP", decision = "phishing", riskScore = 0.87,
                evidence = listOf("OCR 근거"), limitations = listOf("OCR 한계"),
            ),
        ).toDomain().toScreenCropAnalysis()
        val imageResult = checkNotNull(AnalysisUiState.Success(image).toPresentedResult(false, HistoryType.IMAGE))
        val withImage = AnalysisUiState.ScreenSuccess(image, text, null, null)
        val withoutImage = withImage.copy(image = null)
        assertEquals("image-id", imageResult.imageSummaryLookupId())
        val screenResult = checkNotNull(withImage.toPresentedResult(false, HistoryType.SCREEN))
        assertEquals("image-id", screenResult.imageSummaryLookupId())
        for (result in listOf(imageResult, screenResult)) {
            assertEquals("AI_GENERATED_SUSPECTED", result.imageGenerationDecision)
            assertEquals(80.0, result.imageGenerationDisplayScore!!, 0.0)
            assertEquals(listOf("이미지 근거"), result.imageGenerationDetails?.reasons)
            assertEquals(listOf("이미지 한계"), result.imageGenerationDetails?.limitations)
        }
        assertEquals("phishing", screenResult.textDecision)
        assertEquals(0.87, screenResult.textRiskScore!!, 0.0)
        assertEquals(listOf("OCR 근거"), screenResult.screenTextEvidence.map { it.description })
        assertEquals(listOf("OCR 한계"), screenResult.textLimitations)
        assertNull(withoutImage.toPresentedResult(false, HistoryType.SCREEN)?.imageSummaryLookupId())
        assertNull(imageResult.copy(summary = "이미 있는 요약").imageSummaryLookupId())
        assertNull(imageResult.copy(historyType = HistoryType.PHONE).imageSummaryLookupId())
    }

    private class SummaryRepository(private val response: suspend (Int) -> ImageSummaryResult) : AnalysisResultRepository {
        val ids = mutableListOf<String>()

        override suspend fun analyzeResult(image: MultipartBody.Part): AnalysisResult =
            error("Checking the summary must not start a new analysis")

        override suspend fun findImageSummary(analysisId: String): ImageSummaryResult {
            ids += analysisId
            return response(ids.size)
        }
    }

    private companion object {
        val completed = ImageSummaryResult.Completed("Gemini 요약")
    }
}
