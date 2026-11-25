package com.dundueni.app.feature.analysis

import com.dundueni.app.data.mapper.toDomain
import com.dundueni.app.data.model.ExtractedPhoneNumber
import com.dundueni.app.data.model.ImageActionGuide
import com.dundueni.app.data.model.ImageAnalysisSummary
import com.dundueni.app.data.model.ImageDetailedReason
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

    @Test fun normalizedVerdictStaysSeparateFromDisplayRisk() {
        for (type in listOf("IMAGE", "TEXT")) for (level in listOf("SAFE", "CAUTION", "DANGER", "UNKNOWN")) {
            val domain = get("""{"riskLevel":"$level","reasons":["서버 근거",null,""],
                "guidance":["서버 대응 안내",null," "],"summary":"서버 요약",
                "textDecision":"phishing","riskScore":0.99}""", type)
            val result = checkNotNull(domain.toPb05Result(false))
            assertEquals(if (type == "TEXT") RiskLevel.CAUTION else RiskLevel.UNKNOWN, result.riskLevel)
            assertEquals(level, domain.riskLevel)
            assertEquals(if (type == "TEXT") "" else "서버 요약", result.summary)
            assertEquals(if (type == "TEXT") emptyList<String>() else listOf("서버 근거"),
                result.evidence.map { it.description })
            assertEquals(if (type == "TEXT") emptyList<String>() else listOf("서버 대응 안내"), result.guidance)
            assertEquals(if (type == "TEXT") HistoryType.TEXT else HistoryType.IMAGE, result.historyType)
            assertNull(result.fraudScore)
            if (type == "TEXT") assertNull(result.aiScore)
        }
    }

    @Test fun integratedImageDetailPreservesOrderedSummaryAndServerDecision() {
        val domain = detail(
            """{
                "analysisId":"id","type":"IMAGE","status":"COMPLETED",
                "overallResult":{"decision":"GENERATION_OR_MANIPULATION_SUSPECTED","decisionLabel":"위험"},
                "models":{"generation":{"status":"SUCCEEDED","displayScore":82.5},"manipulation":null},
                "summaryStatus":"COMPLETED","summary":"이미지 분석 요약",
                "detailedReasons":[
                    {"type":"MANIPULATION","title":"변조 근거","description":"변조 설명"},
                    {"type":"GENERATION","title":"생성 근거","description":"생성 설명"}],
                "actionGuides":[
                    {"title":"첫 수칙","description":"첫 설명"},
                    {"title":"둘째 수칙","description":"둘째 설명"},
                    {"title":"셋째 수칙","description":"셋째 설명"}],
                "reassurance":"안심 조언","summaryModel":"configured-gemini-model",
                "summarizedAt":"2026-11-25T06:30:00.000Z"
            }""",
        )
        val expected = ImageAnalysisSummary(
            summaryStatus = "COMPLETED",
            summary = "이미지 분석 요약",
            detailedReasons = listOf(
                ImageDetailedReason("MANIPULATION", "변조 근거", "변조 설명"),
                ImageDetailedReason("GENERATION", "생성 근거", "생성 설명"),
            ),
            actionGuides = listOf(
                ImageActionGuide("첫 수칙", "첫 설명"),
                ImageActionGuide("둘째 수칙", "둘째 설명"),
                ImageActionGuide("셋째 수칙", "셋째 설명"),
            ),
            reassurance = "안심 조언",
            summaryModel = "configured-gemini-model",
            summarizedAt = "2026-11-25T06:30:00.000Z",
        )
        val presented = checkNotNull(domain.toPb05Result(false))
        assertEquals(expected, domain.imageSummary)
        assertEquals(expected, presented.imageSummary)
        assertNull(domain.summary)
        assertEquals("GENERATION_OR_MANIPULATION_SUSPECTED", presented.imageOverallDecision)
        assertEquals("위험", presented.imageOverallDecisionLabel)
        assertEquals(RiskLevel.UNKNOWN, presented.riskLevel)
        assertEquals(82.5, presented.imageGenerationDisplayScore!!, 0.0)
        assertNull(presented.imageManipulationDetails)
        assertNull(presented.imageManipulationDisplayScore)
    }

    @Test fun absentUnfinishedAndLegacyImageSummariesKeepNullsAndEmptyLists() {
        val cases = listOf(
            "" to ImageAnalysisSummary(),
            ""","summaryStatus":null,"summary":null,"detailedReasons":[],"actionGuides":[]""" to
                ImageAnalysisSummary(),
            ""","summaryStatus":"PENDING","summary":null,"detailedReasons":[],"actionGuides":[]""" to
                ImageAnalysisSummary(summaryStatus = "PENDING"),
            ""","summaryStatus":"FAILED","detailedReasons":null,"actionGuides":null""" to
                ImageAnalysisSummary(summaryStatus = "FAILED"),
            ""","summaryStatus":"COMPLETED","summary":"기존 요약"""" to
                ImageAnalysisSummary(summaryStatus = "COMPLETED", summary = "기존 요약"),
        )
        for ((fields, expected) in cases) {
            val domain = detail(
                """{"analysisId":"id","type":"IMAGE","status":"COMPLETED",
                    "overallResult":{"decision":"INCONCLUSIVE","decisionLabel":"주의"},
                    "models":{"generation":null,
                        "manipulation":{"status":"FAILED","decision":null,"displayScore":null}}$fields}""",
            )
            val presented = checkNotNull(domain.toPb05Result(false))
            assertEquals(expected, presented.imageSummary)
            assertEquals("INCONCLUSIVE", presented.imageOverallDecision)
            assertEquals("주의", presented.imageOverallDecisionLabel)
            assertNull(presented.imageGenerationDetails)
            assertNull(presented.imageGenerationDisplayScore)
            assertEquals("FAILED", presented.imageManipulationDetails?.status)
            assertNull(presented.imageManipulationDecision)
            assertNull(presented.imageManipulationDisplayScore)
        }
    }

    @Test fun overallFailureRemainsDistinctFromImageSummaryFailure() {
        val domain = detail(
            """{"analysisId":"id","type":"IMAGE","status":"FAILED","errorCode":"ANALYSIS503",
                "overallResult":{"decision":"INCONCLUSIVE","decisionLabel":"판단 불가"},
                "summaryStatus":null,"detailedReasons":[],"actionGuides":[]}""",
        )
        assertEquals("FAILED", domain.status)
        assertEquals("ANALYSIS503", domain.errorCode)
        assertEquals("판단 불가", domain.imageOverallDecisionLabel)
        assertEquals(ImageAnalysisSummary(), domain.imageSummary)
        assertNull(domain.toPb05Result(false))
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

    @Test fun phishingUsesCautionDisplayWithoutSettingFraudVerdict() {
        for (risk in listOf("UNKNOWN", "LOW", "MOCK_DANGER", "safe", " SAFE ")) {
            val result = checkNotNull(get("""{"riskLevel":"$risk","textDecision":"phishing","riskScore":100}""", "TEXT").toPb05Result(false))
            assertEquals(RiskLevel.CAUTION, result.riskLevel)
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

    @Test fun textAndScreenOcrUseDecisionForDisplayRisk() {
        val decisions = mapOf(
            "normal" to RiskLevel.SAFE,
            "phishing" to RiskLevel.CAUTION,
            "defer" to RiskLevel.UNKNOWN,
            "future" to RiskLevel.UNKNOWN,
        )
        for ((decision, expectedRisk) in decisions) {
            val domain = gson.fromJson("""{"result":{"analysisId":"text-id","type":"TEXT","status":"COMPLETED",
                "textResult":{"sourceType":"TEXT","decision":"$decision"}}}""", TextAnalysisResponse::class.java).result!!.toDomain()
            val result = domain.toTextResult()
            assertEquals(HistoryType.TEXT, result.historyType)
            assertEquals(expectedRisk, result.riskLevel)
            assertTrue(result.evidence.isEmpty())
            assertTrue(result.guidance.isEmpty())
            assertNull(result.aiScore)
            assertNull(result.fraudLevel)

            val ocr = domain.copy(sourceType = "SCREEN_CROP").toScreenCropAnalysis()
            val screen = AnalysisUiState.ScreenSuccess(
                image = null, text = ocr, imageError = null, textError = null, resultType = ScreenResultType.TEXT,
            ).toPresentedResult(false, HistoryType.SCREEN)
            assertEquals(HistoryType.SCREEN, screen?.historyType)
            assertEquals(expectedRisk, screen?.riskLevel)
            assertEquals(decision, screen?.textDecision)
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
        assertEquals(RiskLevel.CAUTION, presented.riskLevel)
        assertNull(presented.fraudScore)
    }

    @Test fun textAndOcrPostPhonesKeepServerOrderAndFormatting() {
        for (source in listOf("TEXT", "OCR")) {
            val response = gson.fromJson(
                """{"result":{"analysisId":"text-id","type":"TEXT","status":"COMPLETED",
                    "extractedPhoneNumbers":[
                        {"phoneNumber":"021234567","displayNumber":"02-123-4567","source":"$source"},
                        {"phoneNumber":"01012345678","displayNumber":"010-1234-5678","source":"$source"}
                    ]}}""",
                TextAnalysisResponse::class.java,
            )
            val domain = checkNotNull(response.result).toDomain()
            val expected = listOf(
                ExtractedPhoneNumber("021234567", "02-123-4567", source),
                ExtractedPhoneNumber("01012345678", "010-1234-5678", source),
            )
            assertEquals(expected, domain.extractedPhoneNumbers)
            assertEquals(expected, domain.toTextResult().extractedPhoneNumbers)
        }
    }

    @Test fun missingNullAndEmptyPostPhoneArraysBecomeEmptyLists() {
        for (fields in listOf("", ""","extractedPhoneNumbers":null""", ""","extractedPhoneNumbers":[]""")) {
            val response = gson.fromJson(
                """{"result":{"analysisId":"text-id","type":"TEXT","status":"COMPLETED"$fields}}""",
                TextAnalysisResponse::class.java,
            )
            assertTrue(checkNotNull(response.result).toDomain().toTextResult().extractedPhoneNumbers.isEmpty())
        }
    }

    @Test fun textDetailKeepsEvidenceSeparateFromQualityCodes() {
        val domain = gson.fromJson("""{"result":{"analysisId":"text-id","type":"TEXT","status":"COMPLETED",
            "textResult":{"decision":"defer","riskScore":null,"evidence":["맥락이 부족합니다."],
            "limitations":["OCR 일부가 흐립니다."],"quality":{"reasonCodes":["POOR_OCR"]}}}}""",
            AnalysisDetailResponseDto::class.java).result!!.toDomain()
        val presented = domain.toTextResult()
        assertEquals("판단 보류", presented.keyFinding)
        assertNull(presented.imageSummary)
        assertEquals(listOf("맥락이 부족합니다."), presented.evidence.map { it.description })
        assertEquals(listOf("OCR 일부가 흐립니다."), presented.textLimitations)
        assertEquals(listOf("POOR_OCR"), presented.textReasonCodes)
        assertNull(presented.textRiskScore)
    }

    private fun detail(payload: String) = checkNotNull(
        gson.fromJson("""{"result":$payload}""", AnalysisDetailResponseDto::class.java).result,
    ).toDomain()
}
