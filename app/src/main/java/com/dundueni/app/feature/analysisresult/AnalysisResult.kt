package com.dundueni.app.feature.analysisresult

import java.util.Locale

/** The overall verdict comes from the analysis service; the UI never derives it from scores. */
enum class RiskLevel(val label: String) {
    SAFE("안전"), CAUTION("주의"), DANGER("위험");

    companion object {
        fun fromWireValue(value: String): RiskLevel =
            entries.firstOrNull { it.name == value.trim().uppercase(Locale.ROOT) }
                ?: throw IllegalArgumentException("지원하지 않는 위험 단계입니다: $value")
    }
}

enum class AiLikelihood(val label: String) {
    LOW("낮음"), MEDIUM("보통"), HIGH("높음"), UNKNOWN("정보 없음")
}

data class RiskEvidence(val title: String, val description: String) {
    init {
        require(title.isNotBlank()) { "근거 제목이 필요합니다." }
        require(description.isNotBlank()) { "근거 설명이 필요합니다." }
    }
}

data class AnalysisResult(
    val id: String,
    val riskLevel: RiskLevel,
    val summary: String,
    val keyFinding: String? = null,
    val fraudLevel: RiskLevel? = null,
    val fraudScore: Double? = null,
    val aiLikelihood: AiLikelihood = AiLikelihood.UNKNOWN,
    val aiScore: Double? = null,
    val evidence: List<RiskEvidence> = emptyList(),
    val sourceText: String? = null,
    val sourceUrl: String? = null,
    val targetLabel: String = "수신된 문자 메시지 및 첨부 링크",
) {
    init {
        require(id.isNotBlank()) { "결과 ID가 필요합니다." }
        require(summary.isNotBlank()) { "분석 요약이 필요합니다." }
        require(fraudScore == null || fraudScore.isFinite() && fraudScore in 0.0..100.0)
        require(aiScore == null || aiScore.isFinite() && aiScore in 0.0..100.0)
    }
}

enum class ResultAction {
    RETURN, PROFILE, CALL_FAMILY, SHARE, REPORT, PREVIEW_DOMAIN, ASK_FAMILY, SAVE_HISTORY
}
