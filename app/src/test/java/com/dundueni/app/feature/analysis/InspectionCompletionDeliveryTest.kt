package com.dundueni.app.feature.analysis

import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.feature.history.HistoryType
import org.junit.Assert.*
import org.junit.Test

class InspectionCompletionDeliveryTest {
    private fun detail(id: String, type: String = "IMAGE") = AnalysisResult(
        analysisId = id, type = type, status = "COMPLETED", aiGenerationScore = 82.0,
        aiRiskLevel = "HIGH", reasons = listOf("서버 근거 $id"), modelVersion = null,
        errorCode = null, createdAt = null, completedAt = null, expiresAt = null,
        recommendedActions = listOf("서버 안내 $id"), summary = "서버 요약 $id",
        normalizedRiskLevel = "CAUTION", sourceType = if (type == "TEXT") "SCREEN_CROP" else null,
    )

    @Test fun duplicateCompletionAfterDismissOrActivityRecreationDoesNotNotifyAgain() {
        val retainedDelivery = InspectionCompletionDelivery()
        val completed = AnalysisUiState.Success(detail("image-1"))
        val before = completed.toPresentedResult(false, HistoryType.IMAGE)
        assertTrue(retainedDelivery.claim("image:attempt-1"))
        // Closing only the window and collecting the retained success again keeps real data.
        assertFalse(retainedDelivery.claim("image:attempt-1"))
        assertEquals(before, completed.toPresentedResult(false, HistoryType.IMAGE))
        assertTrue(retainedDelivery.claim("image:attempt-2"))
    }

    @Test fun foregroundDeliveryDoesNotNotifyWhenUserLaterLeavesTheResult() {
        val delivery = InspectionCompletionDelivery()
        assertTrue(delivery.claim("completed-in-foreground"))
        assertFalse(delivery.claim("completed-in-foreground"))
        assertTrue(delivery.claim("a-different-inspection"))
    }

    @Test fun imageDestinationPreservesTheExistingPb05Mapping() {
        val server = detail("image-real-id")
        val destination = AnalysisUiState.Success(server).toPresentedResult(false, HistoryType.IMAGE)
        assertEquals(server.toPb05Result(false)?.copy(historyType = HistoryType.IMAGE), destination)
        assertEquals("image-real-id", destination?.id)
        assertEquals("서버 요약 image-real-id", destination?.summary)
    }

    @Test fun screenDestinationKeepsBothServerResultsAndCaptureType() {
        val image = detail("image-id")
        val text = detail("ocr-id", "TEXT")
        val state = AnalysisUiState.ScreenSuccess(image, ScreenCropAnalysis("ocr-id", "phishing", text), null, null)
        val destination = checkNotNull(state.toPresentedResult(false, HistoryType.SCREEN))
        assertEquals(HistoryType.SCREEN, destination.historyType)
        assertEquals("image-id", destination.id)
        assertEquals(listOf(image.toPb05Result(false)?.summary, text.toTextResult().summary).joinToString("\n"), destination.summary)
        assertEquals(image.toPb05Result(false)?.evidence.orEmpty().map { it.copy(title = "이미지 · ${it.title}") } +
            text.toTextResult().evidence.map { it.copy(title = "화면 문자 · ${it.title}") }, destination.evidence)
    }

    @Test fun screenWithNoTextSummaryKeepsOnlyTheImageSummaryAndTextEvidence() {
        val image = detail("image-id")
        val text = detail("ocr-id", "TEXT").copy(summary = null,
            textDecision = "phishing", textEvidence = listOf("문자에 수상한 링크가 있습니다."),
            textLimitations = listOf("OCR 일부를 읽지 못했습니다."))
        val state = AnalysisUiState.ScreenSuccess(image, ScreenCropAnalysis("ocr-id", "phishing", text), null, null)
        val destination = checkNotNull(state.toPresentedResult(false, HistoryType.SCREEN))
        assertEquals("서버 요약 image-id", destination.summary)
        assertEquals(listOf("이미지 · 분석 근거 1", "화면 문자 · 분석 근거 1"),
            destination.evidence.map { it.title })
        assertEquals(listOf("OCR 일부를 읽지 못했습니다."), destination.textLimitations)
    }

    @Test fun textOnlyCaptureUsesTheActualOcrResultAndLoadingHasNoDestination() {
        val text = detail("ocr-only-id", "TEXT")
        val state = AnalysisUiState.ScreenSuccess(null, ScreenCropAnalysis("ocr-only-id", "phishing", text), AnalysisUiState.Error(), null)
        val destination = checkNotNull(state.toPresentedResult(false, HistoryType.SCREEN))
        assertEquals("ocr-only-id", destination.id)
        assertEquals(HistoryType.SCREEN, destination.historyType)
        assertEquals(text.toTextResult().evidence.map { it.copy(title = "화면 문자 · ${it.title}") }, destination.evidence)
        assertNull(AnalysisUiState.Loading.toPresentedResult(false, HistoryType.IMAGE))
        assertNull(AnalysisUiState.Error().toPresentedResult(false, HistoryType.IMAGE))
    }
}
