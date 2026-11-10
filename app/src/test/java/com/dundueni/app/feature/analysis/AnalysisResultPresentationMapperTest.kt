package com.dundueni.app.feature.analysis

import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.feature.analysisresult.AiLikelihood
import com.dundueni.app.feature.analysisresult.RiskLevel
import org.junit.Assert.*
import org.junit.Test

class AnalysisResultPresentationMapperTest {
    private val result = AnalysisResult(
        analysisId = "id", type = "IMAGE", status = "COMPLETED", aiGenerationScore = 82.4,
        aiRiskLevel = "MOCK_CAUTION", reasons = listOf("provided reason"),
        modelVersion = null, errorCode = null, createdAt = null, completedAt = null, expiresAt = null,
    )

    @Test fun demoStatesPreserveDataWithoutDerivingFraudOrAiLevelsFromScores() {
        for (level in listOf(RiskLevel.SAFE, RiskLevel.CAUTION, RiskLevel.DANGER)) {
            val mapped = checkNotNull(result.copy(aiRiskLevel = "MOCK_${level.name}").toPb05Result(true))
            assertEquals(level, mapped.riskLevel)
            assertEquals(result.analysisId, mapped.id)
            assertEquals(result.aiGenerationScore, mapped.aiScore)
            assertEquals(result.reasons.single(), mapped.evidence.single().description)
            assertNull(mapped.fraudLevel)
            assertNull(mapped.fraudScore)
            assertEquals(AiLikelihood.UNKNOWN, mapped.aiLikelihood)
            assertNull(mapped.sourceUrl)
        }
    }

    @Test fun realProviderValuesUseSharedLayoutWithoutBecomingAnOverallVerdict() {
        for (risk in listOf("SAFE", "CAUTION", "DANGER", "LOW", "MOCK_SAFE", "MOCK_DANGER", null)) {
            val mapped = checkNotNull(result.copy(aiRiskLevel = risk).toPb05Result(false))
            assertEquals(RiskLevel.UNKNOWN, mapped.riskLevel)
            assertEquals(risk?.trim()?.take(60), mapped.aiRiskLabel)
            assertNull(mapped.fraudLevel)
            assertNull(mapped.fraudScore)
        }
    }

    @Test fun realAiLikelihoodOnlyMapsKnownLikelihoodValues() {
        assertEquals(AiLikelihood.HIGH, result.copy(aiRiskLevel = "HIGH").toPb05Result(false)?.aiLikelihood)
        assertEquals(AiLikelihood.UNKNOWN, result.copy(aiRiskLevel = "SERVER_RISK").toPb05Result(false)?.aiLikelihood)
    }

    @Test fun getSummaryAndNormalizedReasonsReachRealResultWithoutRiskInference() {
        val mapped = checkNotNull(result.copy(
            aiRiskLevel = null,
            aiGenerationScore = null,
            normalizedRiskLevel = "UNKNOWN",
            summary = "BE에 저장된 요약",
            reasons = listOf("BE에 저장된 근거"),
        ).toPb05Result(false))
        assertEquals(RiskLevel.UNKNOWN, mapped.riskLevel)
        assertEquals("BE에 저장된 요약", mapped.summary)
        assertEquals("BE에 저장된 근거", mapped.evidence.single().description)
        assertNull(mapped.aiScore)
        assertNull(mapped.fraudScore)
    }

    @Test fun unknownOrIncompleteDemoDataIsNotMapped() {
        assertNull(result.copy(aiRiskLevel = "UNKNOWN").toPb05Result(true))
        assertNull(result.copy(analysisId = null).toPb05Result(true))
        assertNull(result.copy(analysisId = " ").toPb05Result(true))
        assertNull(result.copy(status = "ANALYZING").toPb05Result(true))
        assertNull(result.copy(type = "TEXT").toPb05Result(true))
    }

    @Test fun missingEvidenceAndInvalidScoresDoNotCrashOrInventData() {
        for (score in listOf(null, -1.0, 101.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            val mapped = checkNotNull(result.copy(aiGenerationScore = score, reasons = listOf(" ", "")).toPb05Result(true))
            assertNull(mapped.aiScore)
            assertTrue(mapped.evidence.isEmpty())
        }
    }
    @Test fun textDetailPreservesServerFindingsAndUnknownWithoutInventingScores() {
        val detail = result.copy(type = "TEXT", normalizedRiskLevel = "CAUTION", textDecision = "phishing",
            summary = "GET summary", reasons = listOf("GET reason"), recommendedActions = listOf("GET guidance"), riskScore = 0.9)
        val mapped = detail.toTextResult("original https://example.com")
        assertEquals(RiskLevel.CAUTION, mapped.riskLevel)
        assertEquals("피싱 의심", mapped.keyFinding)
        assertEquals("GET summary", mapped.summary)
        assertEquals("original https://example.com", mapped.sourceText)
        assertEquals("GET reason", mapped.evidence.single().description)
        assertEquals(listOf("GET guidance"), mapped.guidance)
        assertNull(mapped.fraudScore)
        for (risk in listOf(null, "UNKNOWN", "FUTURE")) {
            val absent = detail.copy(normalizedRiskLevel = risk, textDecision = null, summary = null,
                reasons = emptyList(), recommendedActions = emptyList(), riskScore = null).toTextResult()
            assertEquals(RiskLevel.UNKNOWN, absent.riskLevel)
            assertNull(absent.keyFinding)
            assertTrue(absent.evidence.isEmpty())
            assertTrue(absent.guidance.isEmpty())
            assertNull(absent.fraudScore)
        }
    }

}
