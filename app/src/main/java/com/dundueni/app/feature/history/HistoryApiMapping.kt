package com.dundueni.app.feature.history

import com.dundueni.app.data.remote.api.HistoryEntryDto
import com.dundueni.app.data.mapper.toDomain
import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.data.remote.dto.AnalysisDetailDto
import com.dundueni.app.feature.analysis.toPb05Result
import com.dundueni.app.feature.analysis.toTextResult
import com.dundueni.app.feature.analysisresult.AnalysisResult as PresentedResult
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.feature.phone.toPhoneResult
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
        textDecision = if (kind == HistoryType.TEXT) textResult?.decision else null,
        textRiskScore = if (kind == HistoryType.TEXT) textResult?.riskScore else null,
        textSourceType = if (kind == HistoryType.TEXT) textResult?.sourceType else null,
        textNeedsReview = if (kind == HistoryType.TEXT) textResult?.quality?.needsReview else null,
        textReasonCodes = if (kind == HistoryType.TEXT) textResult?.quality?.reasonCodes else null,
        textOcrUsed = if (kind == HistoryType.TEXT) textResult?.quality?.ocrUsed else null,
    )
}

internal data class HistoryRecordDetail(val record: HistoryRecord, val result: PresentedResult?)

internal fun HistoryEntryDto.toRecordDetail(imageResult: AnalysisResult? = null): HistoryRecordDetail {
    val record = toHistoryRecord()
    if (status != "COMPLETED") return HistoryRecordDetail(record, null)

    val result = when (record.type) {
        HistoryType.TEXT -> AnalysisDetailDto(
            analysisId = record.id,
            type = type,
            status = status,
            textResult = textResult,
        ).toDomain().toTextResult(textPreview?.takeIf { it.isNotBlank() })
        HistoryType.IMAGE -> {
            val image = checkNotNull(imageResult)
            check(image.type == "IMAGE" && image.analysisId == record.id)
            checkNotNull(image.toPb05Result(isDemo = false))
        }
        HistoryType.PHONE -> toPhoneResult()
        else -> null
    }
    return HistoryRecordDetail(record, result)
}
