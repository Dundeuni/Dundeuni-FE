package com.dundueni.app.feature.analysisresult

import com.dundueni.app.feature.history.HistoryType
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AnalysisResultTransportTest {
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
