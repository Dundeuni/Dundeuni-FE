package com.dundueni.app.feature.history

import com.dundueni.app.data.mapper.toDomain
import com.dundueni.app.data.remote.dto.AnalysisDetailDto
import com.dundueni.app.data.remote.dto.HistoryDetailDto
import com.dundueni.app.data.remote.dto.HistoryPageItemDto
import com.dundueni.app.data.remote.dto.TextAnalysisQualityDto
import com.dundueni.app.data.remote.dto.TextAnalysisTextResultDto
import com.dundueni.app.data.repository.ImageSummaryResult
import com.dundueni.app.feature.analysis.textDisplayRiskLevel
import com.dundueni.app.feature.analysis.toTextResult
import com.dundueni.app.feature.analysisresult.AnalysisResult
import com.dundueni.app.feature.analysisresult.RiskEvidence
import com.dundueni.app.feature.analysisresult.RiskLevel
import java.time.Instant

internal fun HistoryPageItemDto.toHistoryRecord(): HistoryRecord {
    val kind = historyType(type)
    return HistoryRecord(
        id = requireNotNull(analysisId),
        type = kind,
        riskLevel = if (kind == HistoryType.PHONE && status == "COMPLETED") {
            phoneRiskLevel(aiRiskLevel)
        } else RiskLevel.UNKNOWN,
        savedAt = Instant.parse(requireNotNull(createdAt)).toEpochMilli(),
        preview = targetSummary?.takeIf { it.isNotBlank() },
        status = status,
    )
}

internal data class HistoryRecordDetail(
    val record: HistoryRecord,
    val result: AnalysisResult?,
    val imageSummaryStatus: ImageSummaryResult? = null,
)

internal fun HistoryDetailDto.toRecordDetail(): HistoryRecordDetail {
    val kind = historyType(type)
    val stored = detail
    val descriptions = reasons.orEmpty().mapNotNull { it.description?.takeIf(String::isNotBlank) }
    val record = HistoryRecord(
        id = requireNotNull(analysisId),
        type = kind,
        riskLevel = when {
            status != "COMPLETED" -> RiskLevel.UNKNOWN
            kind == HistoryType.PHONE -> phoneRiskLevel(stored?.riskLevel ?: aiRiskLevel)
            kind == HistoryType.TEXT -> textDisplayRiskLevel(stored?.decision)
            else -> RiskLevel.UNKNOWN
        },
        savedAt = Instant.parse(requireNotNull(createdAt)).toEpochMilli(),
        status = status,
        evidence = descriptions,
    )
    if (status != "COMPLETED") return HistoryRecordDetail(record, null)
    return when (kind) {
        HistoryType.IMAGE, HistoryType.SCREEN -> HistoryRecordDetail(
            record,
            AnalysisResult(
                id = record.id,
                historyType = kind,
                riskLevel = RiskLevel.UNKNOWN,
                aiRiskLabel = aiRiskLevel?.takeIf(String::isNotBlank),
                aiScore = aiGenerationScore?.takeIf { it.isFinite() && it in 0.0..100.0 },
                summary = stored?.summary?.takeIf { stored.summaryStatus == "COMPLETED" }.orEmpty(),
                evidence = descriptions.map { RiskEvidence("이미지 분석 근거", it) },
            ),
            imageSummaryStatus = when (stored?.summaryStatus) {
                "PENDING" -> ImageSummaryResult.Pending
                "FAILED" -> ImageSummaryResult.Failed
                else -> null
            },
        )
        HistoryType.TEXT -> {
            val result = stored?.let {
                AnalysisDetailDto(
                    analysisId = record.id,
                    type = type,
                    status = status,
                    textResult = TextAnalysisTextResultDto(
                        decision = it.decision,
                        riskScore = it.riskScore,
                        quality = it.needsReview?.let { needsReview -> TextAnalysisQualityDto(needsReview = needsReview) },
                        evidence = descriptions,
                    ),
                ).toDomain().toTextResult(it.textPreview?.takeIf(String::isNotBlank))
            }
            HistoryRecordDetail(record, result)
        }
        HistoryType.PHONE -> {
            val result = stored?.let {
                val count = it.reportCount?.takeIf { count -> count >= 0 }
                val phoneReasons = it.reasons?.filter(String::isNotBlank) ?: descriptions
                AnalysisResult(
                    id = record.id,
                    historyType = kind,
                    riskLevel = record.riskLevel,
                    summary = "",
                    keyFinding = count?.let { value ->
                        val suffix = if (it.reportCountIsRange == true) "+" else ""
                        "신고 건수: $value${suffix}건"
                    },
                    evidence = phoneReasons.map { reason -> RiskEvidence("전화번호 분석 근거", reason) },
                    targetLabel = it.maskedPhoneNumber.orEmpty(),
                )
            }
            HistoryRecordDetail(record, result)
        }
        else -> HistoryRecordDetail(record, null)
    }
}

private fun historyType(type: String?): HistoryType = when (type) {
    "IMAGE" -> HistoryType.IMAGE
    "SCREEN" -> HistoryType.SCREEN
    "TEXT" -> HistoryType.TEXT
    "PHONE" -> HistoryType.PHONE
    else -> HistoryType.UNKNOWN
}

private fun phoneRiskLevel(value: String?): RiskLevel =
    RiskLevel.entries.firstOrNull { it.name == value } ?: RiskLevel.UNKNOWN
