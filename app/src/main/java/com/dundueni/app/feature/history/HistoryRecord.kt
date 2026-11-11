package com.dundueni.app.feature.history

import com.dundueni.app.feature.analysisresult.AiLikelihood
import com.dundueni.app.feature.analysisresult.RiskLevel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class HistoryType(val label: String) {
    SCREEN("화면"), LINK("링크"), IMAGE("이미지"), TEXT("텍스트"), PHONE("전화번호"), UNKNOWN("종류 미상")
}

data class HistoryRecord(
    val id: String,
    val riskLevel: RiskLevel,
    val savedAt: Long,
    val type: HistoryType = HistoryType.UNKNOWN,
    val fraudScore: Double? = null,
    val aiScore: Double? = null,
    // Display-only sample information. Original content is never written by the history store.
    val preview: String? = null,
    val status: String? = null,
    val evidence: List<String> = emptyList(),
    val aiLikelihood: AiLikelihood = AiLikelihood.UNKNOWN,
    val textDecision: String? = null,
    val textRiskScore: Double? = null,
    val textSourceType: String? = null,
    val textNeedsReview: Boolean? = null,
    val textReasonCodes: List<String>? = null,
    val textOcrUsed: Boolean? = null,
) {
    val title: String get() = "${type.label} 검사"
    val statusLabel: String get() = when (status) {
        "QUEUED", "ANALYZING" -> "분석 중"
        "FAILED" -> "분석 실패"
        else -> if (type == HistoryType.IMAGE && aiLikelihood == AiLikelihood.HIGH) "AI 생성 의심" else riskLevel.label
    }
    fun date(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(savedAt).atZone(zone).toLocalDate()
}

internal fun filterHistory(records: List<HistoryRecord>, type: HistoryType?, query: String): List<HistoryRecord> {
    val keyword = query.trim()
    return records.filter {
        (type == null || it.type == type) &&
            (keyword.isEmpty() || "${it.title} ${it.riskLevel.label} ${it.preview.orEmpty()} ${it.statusLabel}".contains(keyword, ignoreCase = true))
    }.sortedByDescending { it.savedAt }
}
