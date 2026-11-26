package com.dundueni.app.data.repository

import com.dundueni.app.data.mapper.toDomain
import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.data.model.ImageActionGuide
import com.dundueni.app.data.model.ImageAnalysisSummary
import com.dundueni.app.data.model.ImageDetailedReason
import com.dundueni.app.data.remote.dto.AnalysisDetailDto
import com.dundueni.app.data.remote.dto.AnalysisImageModelsDto
import com.dundueni.app.data.remote.dto.AnalysisModelResultDto
import com.dundueni.app.data.remote.dto.AnalysisReasonDto
import com.dundueni.app.data.remote.dto.TextAnalysisTextResultDto
import com.dundueni.app.feature.analysis.AnalysisUiState
import com.dundueni.app.feature.analysis.toPresentedResult
import com.dundueni.app.feature.analysis.toScreenCropAnalysis
import com.dundueni.app.feature.analysisresult.ImageSummaryViewModel
import com.dundueni.app.feature.analysisresult.initialImageSummary
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

    @Test fun initialTerminalStatesNeverTriggerAnExtraLookup() = runTest {
        for (initial in listOf(completed, ImageSummaryResult.Failed, ImageSummaryResult.Unavailable)) {
            val repository = SummaryRepository { error("A terminal summary must not be fetched") }
            val model = ImageSummaryViewModel.forResult(UUID.randomUUID().toString(), repository, initial)
            model.refreshWhileVisible()
            advanceTimeBy(60_000)
            assertEquals(initial, model.uiState.value.result)
            assertFalse(model.uiState.value.checking)
            assertFalse(model.uiState.value.showRetry)
            assertTrue(repository.ids.isEmpty())
        }
    }

    @Test fun pendingWaitsFiveSecondsBetweenLookupsAndStopsAfterCompletion() = runTest {
        val repository = SummaryRepository { if (it == 1) ImageSummaryResult.Pending else completed }
        val model = pendingModel(repository)
        val visible = launch { model.refreshWhileVisible() }
        runCurrent()
        assertEquals(ImageSummaryResult.Pending, model.uiState.value.result)
        assertTrue(model.uiState.value.checking)
        advanceTimeBy(4_999)
        assertTrue(repository.ids.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(analysisId), repository.ids)
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(completed, model.uiState.value.result)
        assertTrue(visible.isCompleted)
        advanceTimeBy(60_000)
        assertEquals(List(2) { analysisId }, repository.ids)
    }

    @Test fun failedOrUnavailableResponseStopsPendingPollingAndDisablesRetry() = runTest {
        for (terminal in listOf(ImageSummaryResult.Failed, ImageSummaryResult.Unavailable)) {
            val repository = SummaryRepository { terminal }
            val model = ImageSummaryViewModel.forResult(
                UUID.randomUUID().toString(), repository, ImageSummaryResult.Pending,
            )
            val visible = launch { model.refreshWhileVisible() }
            advanceTimeBy(5_000)
            runCurrent()
            assertEquals(terminal, model.uiState.value.result)
            assertFalse(model.uiState.value.showRetry)
            assertTrue(visible.isCompleted)
            model.checkAgain()
            advanceTimeBy(60_000)
            assertEquals(1, repository.ids.size)
        }
    }

    @Test fun pendingStopsAfterTwelveAndOffersManualLookup() = runTest {
        val repository = SummaryRepository {
            when (it) {
                13 -> { delay(1_000); ImageSummaryResult.Pending }
                14 -> { delay(1_000); completed }
                else -> ImageSummaryResult.Pending
            }
        }
        val model = pendingModel(repository)
        val visible = launch { model.refreshWhileVisible() }
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(12, repository.ids.size)
        assertEquals(ImageSummaryResult.Pending, model.uiState.value.result)
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

    @Test fun leavingCancelsTheDelayAndAnInFlightLookupAndReopeningKeepsTheBudget() = runTest {
        var cancelled = false
        val repository = SummaryRepository {
            if (it == 2) {
                try {
                    delay(60_000)
                } finally {
                    cancelled = true
                }
            }
            ImageSummaryResult.Pending
        }
        val model = pendingModel(repository)
        val beforeFirstLookup = launch { model.refreshWhileVisible() }
        advanceTimeBy(1_000)
        beforeFirstLookup.cancel()
        runCurrent()
        advanceTimeBy(60_000)
        assertTrue(repository.ids.isEmpty())

        val visible = launch { model.refreshWhileVisible() }
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(2, repository.ids.size)
        visible.cancel()
        runCurrent()
        assertTrue(cancelled)
        assertFalse(model.uiState.value.lookupFailed)
        advanceTimeBy(60_000)
        assertEquals(2, repository.ids.size)

        val reopened = pendingModel(repository)
        val resumed = launch { reopened.refreshWhileVisible() }
        advanceTimeBy(50_000)
        runCurrent()
        assertEquals(12, repository.ids.size)
        assertTrue(reopened.uiState.value.showRetry)
        resumed.cancel()
        runCurrent()
        val afterExhaustion = pendingModel(repository)
        val reentered = launch { afterExhaustion.refreshWhileVisible() }
        advanceTimeBy(60_000)
        assertEquals(12, repository.ids.size)
        assertTrue(afterExhaustion.uiState.value.showRetry)
        reentered.cancel()
    }

    @Test fun concurrentObserversShareOneLookupAndReopeningUsesItsCompletedState() = runTest {
        val repository = SummaryRepository { completed }
        val first = pendingModel(repository)
        val second = pendingModel(repository)
        val firstVisit = launch { first.refreshWhileVisible() }
        val secondVisit = launch { second.refreshWhileVisible() }
        advanceTimeBy(5_000)
        runCurrent()
        assertTrue(firstVisit.isCompleted)
        assertTrue(secondVisit.isCompleted)
        assertEquals(listOf(analysisId), repository.ids)
        assertEquals(completed, second.uiState.value.result)
        pendingModel(repository).refreshWhileVisible()
        assertEquals(listOf(analysisId), repository.ids)
    }

    @Test fun lookupErrorPreservesPendingStateAndAllowsManualRecovery() = runTest {
        val repository = SummaryRepository { if (it <= 2) ImageSummaryResult.LookupError else completed }
        val model = pendingModel(repository)
        val visible = launch { model.refreshWhileVisible() }
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(ImageSummaryResult.Pending, model.uiState.value.result)
        assertTrue(model.uiState.value.lookupFailed)
        assertTrue(model.uiState.value.showRetry)
        advanceTimeBy(60_000)
        assertEquals(listOf(analysisId), repository.ids)

        model.checkAgain()
        runCurrent()
        assertEquals(2, repository.ids.size)
        assertTrue(model.uiState.value.showRetry)

        model.checkAgain()
        runCurrent()
        assertEquals(List(3) { analysisId }, repository.ids)
        assertEquals(completed, model.uiState.value.result)
        assertFalse(model.uiState.value.lookupFailed)
        assertFalse(model.uiState.value.showRetry)
        assertTrue(visible.isCompleted)
    }

    private fun pendingModel(repository: AnalysisResultRepository) =
        ImageSummaryViewModel.forResult(analysisId, repository, ImageSummaryResult.Pending)

    @Test fun imageAndScreenUseImageSummaryAndNeverUseOcrResult() {
        val image = AnalysisDetailDto(
            analysisId = "image-id", type = "IMAGE", status = "COMPLETED", summaryStatus = "PENDING",
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
        assertEquals(ImageSummaryResult.Pending, imageResult.initialImageSummary())
        val screenResult = checkNotNull(withImage.toPresentedResult(false, HistoryType.SCREEN))
        assertEquals("image-id", screenResult.id)
        assertEquals(ImageSummaryResult.Pending, screenResult.initialImageSummary())
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
        assertNull(withoutImage.toPresentedResult(false, HistoryType.SCREEN)?.initialImageSummary())
        assertEquals(ImageSummaryResult.Pending, imageResult.copy(summary = "이미 있는 요약").initialImageSummary())
        assertEquals(ImageSummaryResult.Unavailable, imageResult.copy(imageSummary = null).initialImageSummary())
        assertNull(imageResult.copy(historyType = HistoryType.PHONE).initialImageSummary())
        assertNull(imageResult.copy(historyType = HistoryType.TEXT).initialImageSummary())
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
        val completed = ImageSummaryResult.Completed(
            ImageAnalysisSummary(
                summaryStatus = "COMPLETED",
                summary = "Gemini 요약",
                detailedReasons = listOf(ImageDetailedReason("GENERATION", "생성 근거", "근거 설명")),
                actionGuides = listOf(ImageActionGuide("공식 연락처 확인", "공식 홈페이지에서 확인하세요.")),
                reassurance = "서두르지 않아도 됩니다.",
                summaryModel = "gemini",
                summarizedAt = "2026-11-25T06:30:00.000Z",
            ),
        )
    }
}
