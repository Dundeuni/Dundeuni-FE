package com.dundueni.app.feature.analysis

import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.feature.history.HistoryType
import com.dundueni.app.feature.analysisresult.AnalysisResult as PresentedResult
import com.dundueni.app.feature.analysisresult.RiskEvidence
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.feature.analysisresult.AiLikelihood

/** Image model decisions stay separate from the final fraud risk and text classification. */
internal fun AnalysisResult.toPb05Result(isDemo: Boolean): PresentedResult? {
    if (status != "COMPLETED") return null
    if (isDemo && type != "IMAGE") return null
    if (!isDemo && type !in setOf(null, "IMAGE", "TEXT")) return null
    if (!isDemo) return toRealPb05Result()
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

/** Scores and text classification never supply the final fraud verdict. */
private fun AnalysisResult.toRealPb05Result(): PresentedResult {
    val descriptions = (if (type == "TEXT") textEvidence else reasons).filter { it.isNotBlank() }
    val isText = type == "TEXT"
    val decisionLabel = if (isText) when (textDecision) {
        "normal" -> "일반 텍스트로 판단"
        "phishing" -> "피싱 의심"
        "defer" -> "판단 보류"
        else -> null
    } else null
    return PresentedResult(
        historyType = if (isText) HistoryType.TEXT else HistoryType.IMAGE,
        id = analysisId?.takeIf { it.isNotBlank() } ?: "image-result-${hashCode()}",
        riskLevel = if (isText) textDisplayRiskLevel(textDecision) else RiskLevel.UNKNOWN,
        keyFinding = decisionLabel,
        // The current TEXT detail attaches guides computed from image model results.
        guidance = if (isText) emptyList() else recommendedActions.filter { it.isNotBlank() },
        summary = if (isText) "" else summary?.takeIf { it.isNotBlank() }.orEmpty(),
        aiLikelihood = AiLikelihood.UNKNOWN,
        aiRiskLabel = null,
        aiScore = aiGenerationScore?.takeIf {
            type == "IMAGE" && !hasImageModels &&
                imageGenerationDecision == null && imageManipulationDecision == null &&
                imageGenerationDisplayScore == null && imageManipulationDisplayScore == null &&
                imageGenerationDetails == null && imageManipulationDetails == null &&
                it.isFinite() && it in 0.0..100.0
        },
        imageModelResult = !isText,
        imageOverallDecisionLabel = if (isText) null else imageOverallDecisionLabel?.takeIf { it.isNotBlank() },
        imageGenerationDecision = if (isText) null else imageGenerationDecision,
        imageGenerationDisplayScore = if (isText) null else imageGenerationDisplayScore?.takeIf { it.isFinite() && it in 0.0..100.0 },
        imageManipulationDecision = if (isText) null else imageManipulationDecision,
        imageManipulationDisplayScore = if (isText) null else imageManipulationDisplayScore?.takeIf { it.isFinite() && it in 0.0..100.0 },
        imageGenerationDetails = if (isText) null else imageGenerationDetails,
        imageManipulationDetails = if (isText) null else imageManipulationDetails,
        textDecision = if (isText) textDecision else null,
        textRiskScore = if (isText) riskScore?.takeIf { it.isFinite() && it in 0.0..1.0 } else null,
        textSourceType = if (isText) sourceType else null,
        textNeedsReview = if (isText) textNeedsReview else null,
        textReasonCodes = if (isText) textReasonCodes else null,
        textOcrUsed = if (isText) textOcrUsed else null,
        textLimitations = if (isText) textLimitations else emptyList(),
        evidence = descriptions.mapIndexed { index, description ->
            RiskEvidence("분석 근거 ${index + 1}", description)
        },
        targetLabel = if (isText) "입력한 텍스트 내용" else "선택한 이미지 또는 캡처 화면",
    )
}

internal fun textDisplayRiskLevel(decision: String?): RiskLevel = when (decision) {
    "normal" -> RiskLevel.SAFE
    "phishing" -> RiskLevel.CAUTION
    else -> RiskLevel.UNKNOWN
}

/** Preserve the submitted text alongside the server's text classifier result. */
internal fun AnalysisResult.toTextResult(text: String? = null): PresentedResult {
    check(status == "COMPLETED" && type == "TEXT")
    require(!analysisId.isNullOrBlank())
    return checkNotNull(toPb05Result(isDemo = false)).copy(sourceText = text)
}
