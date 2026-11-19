package com.dundueni.app.feature.analysisresult

import com.dundueni.app.feature.history.HistoryType
import com.dundueni.app.feature.analysis.AnalysisErrorKind
import com.dundueni.app.feature.analysis.AnalysisUiState
import com.dundueni.app.data.remote.dto.AnalysisDetailDto
import com.dundueni.app.data.mapper.toDomain
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
        val partial = AnalysisUiState.ScreenSuccess(
            image, null, null, AnalysisUiState.Error(AnalysisErrorKind.IN_PROGRESS, pendingAnalysisId = "ocr-id"),
        )
        val intent = AnalysisResultActivity.createScreenIntent(context, partial)
        assertEquals(partial, AnalysisResultActivity.readDelivery(context, intent)?.screen)
    }

    @Test fun textMetadataSurvivesResultActivityJsonTransport() {
        val source = AnalysisResult("text-id", RiskLevel.UNKNOWN, "summary", historyType = HistoryType.TEXT,
            textDecision = "defer", textRiskScore = 0.37, textSourceType = "SCREEN_CROP",
            textNeedsReview = true, textReasonCodes = listOf("LOW_CONFIDENCE"), textOcrUsed = true)
        assertEquals(source, AnalysisResultJsonAdapter.decode(Gson().toJson(source)))
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
