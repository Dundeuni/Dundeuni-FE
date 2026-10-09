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
        for (level in RiskLevel.entries) {
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

    @Test fun realProviderValuesNeverBecomeAnOverallVerdictEvenWhenTheyResembleDemoValues() {
        for (risk in listOf("SAFE", "CAUTION", "DANGER", "LOW", "MOCK_SAFE", "MOCK_DANGER", null)) {
            assertNull(result.copy(aiRiskLevel = risk).toPb05Result(false))
        }
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
}
