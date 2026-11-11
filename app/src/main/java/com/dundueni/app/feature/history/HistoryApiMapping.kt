package com.dundueni.app.feature.history

import com.dundueni.app.data.remote.api.HistoryEntryDto
import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.feature.analysisresult.RiskLevel
import java.time.Instant

/** List has no final image/text verdict; only the phone risk level is a BE verdict. */
internal fun HistoryEntryDto.toHistoryRecord(): HistoryRecord {
    val kind = when (type) {
        "IMAGE" -> HistoryType.IMAGE
        "TEXT" -> HistoryType.TEXT
        "PHONE" -> HistoryType.PHONE
        else -> HistoryType.UNKNOWN
    }
    val risk = if (status == "COMPLETED" && kind == HistoryType.PHONE) when (phoneResult?.riskLevel) {
        "SAFE" -> RiskLevel.SAFE
        "CAUTION" -> RiskLevel.CAUTION
        "DANGER" -> RiskLevel.DANGER
        else -> RiskLevel.UNKNOWN
    } else RiskLevel.UNKNOWN
    val preview = when (kind) {
        HistoryType.TEXT -> textPreview?.takeIf { it.isNotBlank() }
            ?: textResult?.decision?.takeIf { it.isNotBlank() }?.let { "텍스트 판정: $it" }
        HistoryType.PHONE -> phoneResult?.reasons?.firstOrNull { it.isNotBlank() }
            ?: phoneResult?.phoneNumber
        else -> null
    }
    return HistoryRecord(
        id = requireNotNull(id),
        type = kind,
        riskLevel = risk,
        savedAt = requireNotNull(createdAt).let { Instant.parse(it).toEpochMilli() },
        preview = preview,
        status = status,
        evidence = phoneResult?.reasons.orEmpty().filter { it.isNotBlank() },
    )
}

internal fun HistoryRecord.withImageDetail(result: AnalysisResult): HistoryRecord {
    require(type == HistoryType.IMAGE && result.type == "IMAGE" && result.analysisId == id)
    return copy(
        aiScore = result.aiGenerationScore?.takeIf { it.isFinite() && it in 0.0..100.0 },
        preview = result.summary?.takeIf { it.isNotBlank() }
            ?: result.imageOverallDecisionLabel?.takeIf { it.isNotBlank() }
            ?: preview,
        evidence = result.reasons.filter { it.isNotBlank() },
    )
}
