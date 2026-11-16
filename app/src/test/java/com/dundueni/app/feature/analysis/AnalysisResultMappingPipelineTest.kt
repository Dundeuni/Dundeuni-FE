package com.dundueni.app.feature.analysis

import com.dundueni.app.data.mapper.toDomain
import com.dundueni.app.data.remote.dto.AnalysisDetailResponseDto
import com.dundueni.app.data.remote.dto.AnalysisResponseDto
import com.dundueni.app.data.remote.api.TextAnalysisResponse
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.feature.history.HistoryType
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class AnalysisResultMappingPipelineTest {
    private val gson = Gson()
    private fun get(payload: String, type: String = "IMAGE") = gson.fromJson(
        """{"result":{"analysisId":"id","type":"$type","status":"COMPLETED",
        "aiGenerationScore":99,"aiRiskLevel":"HIGH","result":$payload}}""",
        AnalysisDetailResponseDto::class.java,
    ).result!!.toDomain()

    @Test fun normalizedVerdictAndAllContentReachPb05ForImageAndText() {
        for (type in listOf("IMAGE", "TEXT")) for (level in listOf("SAFE", "CAUTION", "DANGER", "UNKNOWN")) {
            val domain = get("""{"riskLevel":"$level","reasons":["서버 근거",null,""],
                "guidance":["서버 대응 안내",null," "],"summary":"서버 요약",
                "textDecision":"phishing","riskScore":0.99}""", type)
            val result = checkNotNull(domain.toPb05Result(false))
            assertEquals(if (type == "TEXT") RiskLevel.valueOf(level) else RiskLevel.UNKNOWN, result.riskLevel)
            assertEquals(level, domain.riskLevel)
            assertEquals("서버 요약", result.summary)
            assertEquals(if (type == "TEXT") emptyList<String>() else listOf("서버 근거"),
                result.evidence.map { it.description })
            assertEquals(listOf("서버 대응 안내"), result.guidance)
            assertEquals(if (type == "TEXT") HistoryType.TEXT else HistoryType.IMAGE, result.historyType)
            assertNull(result.fraudScore)
            if (type == "TEXT") assertNull(result.aiScore)
        }
    }

    @Test fun missingNullAndEmptyContentStayNeutralWithoutSampleContent() {
        for (payload in listOf("{}", """{"riskLevel":null,"reasons":null,"guidance":null,"summary":null}""",
            """{"riskLevel":"UNSUPPORTED","reasons":[],"guidance":[],"summary":" "}""")) {
            val result = checkNotNull(get(payload).toPb05Result(false))
            assertEquals(RiskLevel.UNKNOWN, result.riskLevel)
            assertTrue(result.evidence.isEmpty())
            assertTrue(result.guidance.isEmpty())
            assertEquals("", result.summary)
            assertNull(result.fraudScore)
        }
    }

    @Test fun aiScoresAndTextDecisionsNeverBecomeFinalRisk() {
        for (risk in listOf("UNKNOWN", "LOW", "MOCK_DANGER", "safe", " SAFE ")) {
            val result = checkNotNull(get("""{"riskLevel":"$risk","textDecision":"phishing","riskScore":100}""", "TEXT").toPb05Result(false))
            assertEquals(RiskLevel.UNKNOWN, result.riskLevel)
            assertNull(result.fraudLevel)
            assertNull(result.fraudScore)
            assertEquals("피싱 의심", result.keyFinding)
        }
    }

    @Test fun imagePostObjectReasonsAndMockVerdictsRemainSeparateFromRealVerdicts() {
        val domain = gson.fromJson("""{"result":{"analysisId":"id","type":"IMAGE","status":"COMPLETED",
            "aiRiskLevel":"MOCK_DANGER","aiGenerationScore":95,"reasons":[{"description":"제공된 이미지 근거"}]}}""",
            AnalysisResponseDto::class.java).toDomain()
        assertEquals(RiskLevel.DANGER, domain.toPb05Result(true)!!.riskLevel)
        assertEquals(RiskLevel.UNKNOWN, domain.toPb05Result(false)!!.riskLevel)
        assertEquals("제공된 이미지 근거", domain.toPb05Result(false)!!.evidence.single().description)
        assertTrue(domain.toPb05Result(false)!!.guidance.isEmpty())
    }

    @Test fun textPostUsesDecisionWithoutAssumingGetFields() {
        for (decision in listOf("normal", "phishing", "defer", "future")) {
            val domain = gson.fromJson("""{"result":{"analysisId":"text-id","type":"TEXT","status":"COMPLETED",
                "textResult":{"sourceType":"TEXT","decision":"$decision"}}}""", TextAnalysisResponse::class.java).result!!.toDomain()
            val result = domain.toPb05Result(false)!!
            assertEquals(HistoryType.TEXT, result.historyType)
            assertEquals(RiskLevel.UNKNOWN, result.riskLevel)
            assertTrue(result.evidence.isEmpty())
            assertTrue(result.guidance.isEmpty())
            assertNull(result.aiScore)
        }
    }

    @Test fun textPostMapsScoreAndQualityWithoutDerivingFraudRisk() {
        val domain = gson.fromJson("""{"result":{"analysisId":"text-id","type":"TEXT","status":"COMPLETED",
            "textResult":{"decision":"phishing","riskScore":0.87,"sourceType":"TEXT",
                "quality":{"needsReview":true,"reasonCodes":["LOW_CONFIDENCE"],"ocrUsed":false},
                "evidence":["링크 주소가 요청 기관과 다릅니다."],"limitations":["발신자 신원은 확인하지 못했습니다."]}}}""",
            TextAnalysisResponse::class.java).result!!.toDomain()
        val presented = domain.toTextResult()
        assertEquals("phishing", domain.textDecision)
        assertEquals(0.87, domain.riskScore!!, 0.0)
        assertEquals("TEXT", domain.sourceType)
        assertEquals(true, presented.textNeedsReview)
        assertEquals(listOf("LOW_CONFIDENCE"), presented.textReasonCodes)
        assertEquals(false, presented.textOcrUsed)
        assertEquals(0.87, presented.textRiskScore!!, 0.0)
        assertEquals(listOf("링크 주소가 요청 기관과 다릅니다."), presented.evidence.map { it.description })
        assertEquals(listOf("발신자 신원은 확인하지 못했습니다."), presented.textLimitations)
        assertEquals("", presented.summary)
        assertEquals(RiskLevel.UNKNOWN, presented.riskLevel)
        assertNull(presented.fraudScore)
    }

    @Test fun textDetailKeepsEvidenceSeparateFromQualityCodes() {
        val domain = gson.fromJson("""{"result":{"analysisId":"text-id","type":"TEXT","status":"COMPLETED",
            "textResult":{"decision":"defer","riskScore":null,"evidence":["맥락이 부족합니다."],
            "limitations":["OCR 일부가 흐립니다."],"quality":{"reasonCodes":["POOR_OCR"]}}}}""",
            AnalysisDetailResponseDto::class.java).result!!.toDomain()
        val presented = domain.toTextResult()
        assertEquals("판단 보류", presented.keyFinding)
        assertEquals(listOf("맥락이 부족합니다."), presented.evidence.map { it.description })
        assertEquals(listOf("OCR 일부가 흐립니다."), presented.textLimitations)
        assertEquals(listOf("POOR_OCR"), presented.textReasonCodes)
        assertNull(presented.textRiskScore)
    }
}
