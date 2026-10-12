package com.dundueni.app.feature.analysis

import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.feature.analysisresult.AnalysisResult as PresentedResult
import com.dundueni.app.feature.analysisresult.RiskEvidence
import com.dundueni.app.feature.analysisresult.RiskLevel

/**
 * The current BE contract supplies AI likelihood, not an overall fraud verdict.
 * Only explicitly synthetic results select PB-05's SAFE/CAUTION/DANGER layouts.
 * Real results remain unclassified until the overall verdict contract is agreed.
 */
internal fun AnalysisResult.toPb05Result(isDemo: Boolean): PresentedResult? {
    if (!isDemo || status != "COMPLETED" || type != "IMAGE") return null
    val level = when (aiRiskLevel) {
        "MOCK_SAFE" -> RiskLevel.SAFE
        "MOCK_CAUTION" -> RiskLevel.CAUTION
        "MOCK_DANGER" -> RiskLevel.DANGER
        else -> return null
    }
    val id = analysisId?.takeIf { it.isNotBlank() } ?: return null
    val descriptions = reasons.filter { it.isNotBlank() }
    return PresentedResult(
        id = id,
        riskLevel = level,
        summary = descriptions.firstOrNull() ?: "이미지 분석 결과를 확인해주세요.",
        aiScore = aiGenerationScore?.takeIf { it.isFinite() && it in 0.0..100.0 },
        evidence = descriptions.mapIndexed { index, description ->
            RiskEvidence("분석 근거 ${index + 1}", description)
        },
        targetLabel = "선택한 이미지 또는 캡처 화면",
    )
}
