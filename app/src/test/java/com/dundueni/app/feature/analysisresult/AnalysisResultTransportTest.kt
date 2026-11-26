package com.dundueni.app.feature.analysisresult

import com.dundueni.app.feature.history.HistoryType
import com.dundueni.app.feature.analysis.AnalysisErrorKind
import com.dundueni.app.feature.analysis.AnalysisUiState
import com.dundueni.app.data.remote.dto.AnalysisDetailDto
import com.dundueni.app.data.mapper.toDomain
import com.dundueni.app.data.model.ExtractedPhoneNumber
import com.dundueni.app.data.model.ImageActionGuide
import com.dundueni.app.data.model.ImageAnalysisSummary
import com.dundueni.app.data.model.ImageDetailedReason
import com.dundueni.app.data.model.TextAnalysisPhoneNumbers
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AnalysisResultTransportTest {
    private val imageSummary = ImageAnalysisSummary(
        summaryStatus = "COMPLETED",
        summary = "통합 요약",
        detailedReasons = listOf(ImageDetailedReason("GENERATION", "AI 생성 근거", "생성 분석 설명")),
        actionGuides = listOf(ImageActionGuide("확인하기", "공식 연락처로 확인하세요.")),
        reassurance = "안심 조언",
        summaryModel = "gemini",
        summarizedAt = "2026-11-25T06:30:00.000Z",
    )

    @Test fun largeResultReopensWithoutPuttingAnalysisDataInIntent() {
        val context = RuntimeEnvironment.getApplication()
        val source = AnalysisResult(
            "text-id", RiskLevel.UNKNOWN, "", historyType = HistoryType.TEXT,
            textDecision = "phishing", textRiskScore = 0.87,
            evidence = List(20) { RiskEvidence("근거 ${it + 1}", "근거 내용".repeat(680)) },
            textLimitations = listOf("원문의 전체 문맥은 확인하지 못했습니다."),
        )
        val json = Gson().toJson(source)
        assertTrue(json.length > 65_536)
        val intent = AnalysisResultActivity.createIntent(context, json)
        assertFalse(intent.hasExtra(AnalysisResultActivity.EXTRA_RESULT_JSON))
        repeat(2) {
            val restored = requireNotNull(AnalysisResultActivity.readDelivery(context, intent)?.resultJson)
            assertEquals(source, AnalysisResultJsonAdapter.decode(restored))
        }
    }

    @Test fun screenDeliveryKeepsCompletedSideAndPendingId() {
        val context = RuntimeEnvironment.getApplication()
        val image = AnalysisDetailDto("image-id", type = "IMAGE", status = "COMPLETED").toDomain()
            .copy(imageSummary = imageSummary, imageOverallDecision = "NO_CLEAR_SIGNAL")
        val partial = AnalysisUiState.ScreenSuccess(
            image, null, null, AnalysisUiState.Error(AnalysisErrorKind.IN_PROGRESS, pendingAnalysisId = "ocr-id"),
            pendingTextPhoneNumbers = TextAnalysisPhoneNumbers(
                "ocr-id", listOf(ExtractedPhoneNumber("021234567", "02-123-4567", "OCR")),
            ),
        )
        val intent = AnalysisResultActivity.createScreenIntent(context, partial)
        assertEquals(partial, AnalysisResultActivity.readDelivery(context, intent)?.screen)
    }

    @Test fun integratedImageMetadataSurvivesResultActivityJsonTransport() {
        val source = AnalysisResult(
            "image-id", RiskLevel.UNKNOWN, "", historyType = HistoryType.IMAGE,
            imageOverallDecision = "NO_CLEAR_SIGNAL",
            imageOverallDecisionLabel = "안전",
            imageSummary = imageSummary,
            analyzedAt = "2026-11-25T06:25:00Z",
        )
        val context = RuntimeEnvironment.getApplication()
        val intent = AnalysisResultActivity.createIntent(context, Gson().toJson(source))
        val restored = requireNotNull(AnalysisResultActivity.readDelivery(context, intent)?.resultJson)
        assertEquals(source, AnalysisResultJsonAdapter.decode(restored))
    }

    @Test fun missingImageSummaryArraysAreEmptyAfterJsonDelivery() {
        val decoded = AnalysisResultJsonAdapter.decode(
            """{"id":"image-id","riskLevel":"UNKNOWN","imageSummary":{"summaryStatus":"PENDING","summary":null}}""",
        )
        assertEquals(ImageAnalysisSummary(summaryStatus = "PENDING"), decoded.imageSummary)
    }

    @Test fun textMetadataSurvivesResultActivityJsonTransport() {
        val source = AnalysisResult("text-id", RiskLevel.UNKNOWN, "summary", historyType = HistoryType.TEXT,
            textDecision = "defer", textRiskScore = 0.37, textSourceType = "SCREEN_CROP",
            textNeedsReview = true, textReasonCodes = listOf("LOW_CONFIDENCE"), textOcrUsed = true,
            analyzedAt = "2026-11-25T06:25:00Z",
            textSummary = "서버가 제공한 텍스트 분석 요약",
            extractedPhoneNumbers = listOf(
                ExtractedPhoneNumber("021234567", "02-123-4567", "OCR"),
                ExtractedPhoneNumber("01012345678", "010-1234-5678", "OCR"),
            ),
        )
        assertEquals(source, AnalysisResultJsonAdapter.decode(Gson().toJson(source)))
    }

    @Test fun missingOrNullPhoneArraysInResultJsonDecodeAsEmpty() {
        for (fields in listOf("", ""","extractedPhoneNumbers":null""")) {
            val result = AnalysisResultJsonAdapter.decode("""{"id":"text-id","riskLevel":"UNKNOWN"$fields}""")
            assertTrue(result.extractedPhoneNumbers.isEmpty())
        }
    }

    @Test fun pb05ActivityTransportPreservesKnownAndUnknownRiskAndServerContent() {
        for (type in listOf(HistoryType.IMAGE, HistoryType.TEXT, HistoryType.SCREEN, HistoryType.PHONE)) {
            for (risk in RiskLevel.entries) {
                val source = AnalysisResult("id", risk, "서버 요약", historyType = type,
                    guidance = listOf("첫 안내", "둘째 안내"),
                    evidence = listOf(RiskEvidence("분석 근거 1", "서버 근거")),
                    aiRiskLabel = "HIGH", aiScore = 90.0,
                )
                assertEquals(source, AnalysisResultJsonAdapter.decode(Gson().toJson(source)))
            }
        }
    }
}
