package com.dundueni.app.feature.analysis

import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.feature.history.HistoryType
import com.dundueni.app.feature.analysisresult.AnalysisResult as PresentedResult
import com.dundueni.app.feature.analysisresult.RiskEvidence
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.feature.analysisresult.AiLikelihood

/**
 * The current BE contract supplies AI likelihood, not an overall fraud verdict.
 * Synthetic results select PB-05's sample verdict layouts. Real results use its neutral layout.
 */
internal fun AnalysisResult.toPb05Result(isDemo: Boolean): PresentedResult? {
    if (status != "COMPLETED") return null
    if (type != "IMAGE" && (isDemo || type != null)) return null
    if (!isDemo) return toUnclassifiedPb05Result()
    val level = when (aiRiskLevel) {
        "MOCK_SAFE" -> RiskLevel.SAFE
        "MOCK_CAUTION" -> RiskLevel.CAUTION
        "MOCK_DANGER" -> RiskLevel.DANGER
        else -> return null
    }
    val id = analysisId?.takeIf { it.isNotBlank() } ?: return null
    val descriptions = reasons.filter { it.isNotBlank() }
    return PresentedResult(
        historyType = HistoryType.IMAGE,
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

/** Real AI likelihood is not an overall fraud verdict; keep the shared result layout neutral. */
private fun AnalysisResult.toUnclassifiedPb05Result(): PresentedResult {
    val descriptions = reasons.filter { it.isNotBlank() }
    val likelihood = when (aiRiskLevel?.trim()?.uppercase()) {
        "LOW" -> AiLikelihood.LOW
        "MEDIUM" -> AiLikelihood.MEDIUM
        "HIGH" -> AiLikelihood.HIGH
        else -> AiLikelihood.UNKNOWN
    }
    return PresentedResult(
        historyType = HistoryType.IMAGE,
        id = analysisId?.takeIf { it.isNotBlank() } ?: "image-result-${hashCode()}",
        riskLevel = RiskLevel.UNKNOWN,
        summary = "분석이 완료되었어요. 아래 AI 정보와 분석 근거를 확인해주세요.",
        aiLikelihood = likelihood,
        aiRiskLabel = aiRiskLevel?.trim()?.takeIf { it.isNotEmpty() }?.take(60),
        aiScore = aiGenerationScore?.takeIf { it.isFinite() && it in 0.0..100.0 },
        evidence = descriptions.mapIndexed { index, description ->
            RiskEvidence("분석 근거 ${index + 1}", description)
        },
        targetLabel = "선택한 이미지 또는 캡처 화면",
    )
}
