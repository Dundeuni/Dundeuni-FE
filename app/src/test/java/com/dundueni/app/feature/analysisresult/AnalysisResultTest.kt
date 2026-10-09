package com.dundueni.app.feature.analysisresult

import org.junit.Assert.*
import org.junit.Test

class AnalysisResultTest {
    @Test fun backendVerdictIsPreservedEvenWhenScoresDiffer() {
        val result = AnalysisResult("test", RiskLevel.DANGER, "서버에서 위험 판정", fraudLevel = RiskLevel.SAFE, fraudScore = 4.0, aiLikelihood = AiLikelihood.LOW)
        assertEquals(RiskLevel.DANGER, result.riskLevel)
        assertEquals(RiskLevel.SAFE, result.fraudLevel)
        assertEquals(AiLikelihood.LOW, result.aiLikelihood)
    }

    @Test fun supportedVerdictsAreCaseInsensitive() {
        assertEquals(RiskLevel.CAUTION, RiskLevel.fromWireValue(" caution "))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownVerdictCannotBecomeSafe() { RiskLevel.fromWireValue("unknown") }

    @Test(expected = IllegalArgumentException::class)
    fun invalidScoreIsRejected() { AnalysisResult("id", RiskLevel.SAFE, "요약", fraudScore = 101.0) }

    @Test(expected = IllegalArgumentException::class)
    fun nanScoreIsRejected() { AnalysisResult("id", RiskLevel.SAFE, "요약", aiScore = Double.NaN) }

    @Test fun missingScoresAndEvidenceAreAllowed() {
        val result = AnalysisResult("id", RiskLevel.CAUTION, "추가 확인 필요")
        assertNull(result.fraudScore)
        assertEquals(AiLikelihood.UNKNOWN, result.aiLikelihood)
        assertTrue(result.evidence.isEmpty())
    }

    @Test fun domainPreviewNeverUsesUserInfoAsHost() {
        assertEquals("evil.example", domainForPreview("https://bank.example@evil.example/path"))
        assertNull(domainForPreview("javascript:alert(1)"))
        assertNull(domainForPreview("not a url"))
        assertNull(domainForPreview(null))
    }
}
