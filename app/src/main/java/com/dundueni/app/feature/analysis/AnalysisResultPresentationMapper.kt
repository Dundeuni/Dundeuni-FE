package com.dundueni.app.feature.analysis

import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.feature.history.HistoryType
import java.util.Locale
import com.dundueni.app.feature.analysisresult.AnalysisResult as PresentedResult
import com.dundueni.app.feature.analysisresult.RiskEvidence
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.feature.analysisresult.AiLikelihood

/** Only normalized BE risk selects a real verdict; scores and text decisions remain independent. */
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
    val descriptions = reasons.filter { it.isNotBlank() }
    val likelihood = when (aiRiskLevel?.trim()?.uppercase(Locale.ROOT)) {
        "LOW" -> AiLikelihood.LOW
        "MEDIUM" -> AiLikelihood.MEDIUM
        "HIGH" -> AiLikelihood.HIGH
        else -> AiLikelihood.UNKNOWN
    }
    val level = when (normalizedRiskLevel) {
        "SAFE" -> RiskLevel.SAFE
        "CAUTION" -> RiskLevel.CAUTION
        "DANGER" -> RiskLevel.DANGER
        else -> RiskLevel.UNKNOWN
    }
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
        riskLevel = level,
        fraudLevel = level.takeUnless { it == RiskLevel.UNKNOWN },
        keyFinding = decisionLabel,
        guidance = recommendedActions.filter { it.isNotBlank() },
        summary = summary?.takeIf { it.isNotBlank() }
            ?: "서버에서 분석 요약을 제공하지 않았어요.",
        aiLikelihood = likelihood,
        aiRiskLabel = aiRiskLevel?.trim()?.takeIf { it.isNotEmpty() }?.take(60),
        aiScore = aiGenerationScore?.takeIf { it.isFinite() && it in 0.0..100.0 },
        evidence = descriptions.mapIndexed { index, description ->
            RiskEvidence("분석 근거 ${index + 1}", description)
        },
        targetLabel = if (isText) "입력한 텍스트 내용" else "선택한 이미지 또는 캡처 화면",
    )
}

/** Preserve the submitted text while sharing dev's normalized verdict and guidance mapping. */
internal fun AnalysisResult.toTextResult(text: String? = null): PresentedResult {
    check(status == "COMPLETED" && type == "TEXT")
    require(!analysisId.isNullOrBlank())
    return checkNotNull(toPb05Result(isDemo = false)).copy(sourceText = text)
}
