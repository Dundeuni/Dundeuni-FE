package com.dundueni.app.feature.analysisresult

import com.dundueni.app.data.model.ExtractedPhoneNumber
import com.dundueni.app.data.model.ImageActionGuide
import com.dundueni.app.data.model.ImageAnalysisSummary
import com.dundueni.app.data.model.ImageDetailedReason
import com.dundueni.app.data.repository.ImageSummaryResult
import com.dundueni.app.feature.analysis.ScreenResultType
import com.dundueni.app.feature.history.HistoryType
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultImageExportTest {
    private val pendingImage = AnalysisResult(
        "image-id", RiskLevel.UNKNOWN, "", historyType = HistoryType.IMAGE,
        imageModelResult = true,
        imageOverallDecision = "INCONCLUSIVE",
        imageGenerationDecision = "AI_GENERATED",
        imageGenerationDisplayScore = 76.0,
        imageSummary = ImageAnalysisSummary(summaryStatus = "PENDING"),
    )

    @Test fun blocksImageAndCombinedSavesOnlyWhileTheirSummaryIsBeingChecked() {
        val pending = ImageSummaryUiState(ImageSummaryResult.Pending, checking = true)
        val exhausted = pending.copy(checking = false, showRetry = true)
        val error = exhausted.copy(result = ImageSummaryResult.LookupError, lookupFailed = true)
        val screen = pendingImage.copy(historyType = HistoryType.SCREEN)
        assertTrue(pendingImage.waitsForImageSummary(null, null))
        assertTrue(pendingImage.waitsForImageSummary(pending, null))
        assertTrue(screen.waitsForImageSummary(pending, null))
        assertTrue(screen.waitsForImageSummary(error.copy(checking = true), ScreenResultType.IMAGE))
        assertFalse(screen.waitsForImageSummary(pending, ScreenResultType.TEXT))
        for (type in listOf(HistoryType.TEXT, HistoryType.PHONE)) {
            assertFalse(pendingImage.copy(historyType = type).waitsForImageSummary(pending, null))
        }
        for (state in listOf(
            exhausted, error, pending.copy(result = ImageSummaryResult.Completed("완료")),
            pending.copy(result = ImageSummaryResult.Failed), pending.copy(result = null),
        )) {
            assertFalse(pendingImage.waitsForImageSummary(state, null))
        }
        assertFalse(pendingImage.copy(imageSummary = null).waitsForImageSummary(null, null))
    }

    @Test fun exportKeepsTheLatestCompletedSummaryAndTheOriginalModelScores() {
        val details = ImageAnalysisSummary(
            summaryStatus = "COMPLETED", summary = "최신 요약",
            detailedReasons = listOf(
                ImageDetailedReason("GENERATION", "생성 근거", "첫 설명"),
                ImageDetailedReason("MANIPULATION", "변조 근거", "둘째 설명"),
            ),
            actionGuides = listOf(ImageActionGuide("대응 방법", "공식 연락처로 확인하세요")),
            reassurance = "안심 조언", summaryModel = "internal-model",
        )
        val export = pendingImage.toImageExport(ImageSummaryUiState(ImageSummaryResult.Completed(details), checking = false))
        assertEquals(details.copy(summaryModel = null), export.imageSummary)
        assertEquals(pendingImage.imageOverallDecision, export.imageOverallDecision)
        assertEquals(pendingImage.imageGenerationDecision, export.imageGenerationDecision)
        assertEquals(pendingImage.imageGenerationDisplayScore, export.imageGenerationDisplayScore)
        assertNull(export.imageManipulationDisplayScore)
        assertEquals("PENDING", pendingImage.imageSummary?.summaryStatus)
    }

    @Test fun exportOmitsUnfinishedSummaryEvenWhenTheOriginalResultContainsOne() {
        val stored = pendingImage.copy(imageSummary = ImageAnalysisSummary("COMPLETED", "이전 요약"))
        for (status in listOf(null, ImageSummaryResult.Pending, ImageSummaryResult.LookupError, ImageSummaryResult.Failed)) {
            val export = stored.toImageExport(ImageSummaryUiState(status, checking = false))
            assertEquals(ImageSummaryResult.Unavailable, export.initialImageSummary())
            assertNull(export.imageSummary?.summary)
            assertTrue(export.imageSummary?.actionGuides.orEmpty().isEmpty())
        }
        assertEquals(stored.imageSummary, stored.toImageExport(null).imageSummary)
    }

    @Test fun exportKeepsFindingsAndGuidanceWhileRemovingRawInputsAndIdentifiers() {
        val raw = "원본 메시지 그대로"
        val number = ExtractedPhoneNumber("01012345678", "010-1234-5678", "OCR")
        val result = AnalysisResult(
            "result-id", RiskLevel.CAUTION, "주의가 필요한 결과",
            sourceText = raw, sourceUrl = "https://example.com/private", targetLabel = number.displayNumber,
            keyFinding = "신고 이력이 있어요", historyType = HistoryType.PHONE,
            analyzedAt = "2026-11-25T06:25:00Z",
            textSummary = "분석에서 ${number.phoneNumber} 확인",
            evidence = listOf(RiskEvidence("근거", "연락처 ${number.displayNumber}")),
            screenTextEvidence = listOf(RiskEvidence("OCR 근거", "$raw / **02-1234-5678** / 15881234")),
            guidance = listOf("공식 기관에 확인하세요"),
            extractedPhoneNumbers = listOf(number),
        )
        val export = result.toImageExport(null)
        assertNull(export.sourceText)
        assertNull(export.sourceUrl)
        assertTrue(export.targetLabel.isEmpty())
        assertTrue(export.extractedPhoneNumbers.isEmpty())
        assertEquals("분석에서 [가림] 확인", export.textSummary)
        assertEquals("연락처 [가림]", export.evidence.single().description)
        assertEquals("[가림] / **[가림]** / [가림]", export.screenTextEvidence.single().description)
        assertEquals(result.summary, export.summary)
        assertEquals(result.keyFinding, export.keyFinding)
        assertEquals(result.guidance, export.guidance)
        assertEquals(result.analyzedAt, export.analyzedAt)
        assertEquals(raw, result.sourceText)
    }

    @Test fun exportFormatsOnlyAProvidedValidInspectionTime() {
        val zone = ZoneId.of("Asia/Seoul")
        assertEquals("2026.11.25 15:25", formatImageInspectionTime("2026-11-25T06:25:00Z", zone))
        assertNull(formatImageInspectionTime(null, zone))
        assertNull(formatImageInspectionTime("invalid", zone))
    }
}
