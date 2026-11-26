package com.dundueni.app.feature.history

import com.dundueni.app.data.mapper.toDetails
import com.dundueni.app.data.mapper.toDomain
import com.dundueni.app.data.model.ImageAnalysisSummary
import com.dundueni.app.data.remote.dto.AnalysisDetailDto
import com.dundueni.app.data.remote.dto.AnalysisModelResultDto
import com.dundueni.app.data.remote.dto.HistoryDetailDto
import com.dundueni.app.data.remote.dto.HistoryPageItemDto
import com.dundueni.app.data.remote.dto.TextAnalysisQualityDto
import com.dundueni.app.data.remote.dto.TextAnalysisTextResultDto
import com.dundueni.app.data.repository.ImageSummaryResult
import com.dundueni.app.data.repository.toImageSummaryResult
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
    val completedAt: Instant? = null,
    val expiresAt: Instant? = null,
    val imageResultStatus: String? = null,
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
    if (kind == HistoryType.IMAGE || kind == HistoryType.SCREEN) return toImageRecordDetail(record)
    if (status != "COMPLETED") return HistoryRecordDetail(record, null)
    return when (kind) {
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

private fun HistoryDetailDto.toImageRecordDetail(record: HistoryRecord): HistoryRecordDetail {
    val stored = detail
    val metadata = HistoryRecordDetail(
        record = record,
        result = null,
        completedAt = completedAt.toStoredInstant(),
        expiresAt = expiresAt.toStoredInstant(),
        imageResultStatus = stored?.imageResultStatus,
    )
    if (status != "COMPLETED" || metadata.imageResultStatus == "FAILED") return metadata

    val models = stored?.modelResults.orEmpty()
    val generation = models.firstOrNull { it?.kind == "GENERATION" }
    val manipulation = models.firstOrNull { it?.kind == "MANIPULATION" }
    val decision = storedImageDecision(generation, manipulation)
    val imageSummary = stored?.let {
        ImageAnalysisSummary(
            summaryStatus = it.summaryStatus,
            summary = it.summary,
            summaryModel = it.summaryModel,
            summarizedAt = it.summarizedAt,
        )
    }
    val summaryStatus = imageSummary.toImageSummaryResult()
    val result = AnalysisResult(
        id = record.id,
        historyType = record.type,
        riskLevel = RiskLevel.UNKNOWN,
        aiRiskLabel = aiRiskLevel?.takeIf(String::isNotBlank),
        aiScore = aiGenerationScore.validDisplayScore().takeIf { generation == null && manipulation == null },
        summary = (summaryStatus as? ImageSummaryResult.Completed)?.text.orEmpty(),
        evidence = record.evidence.map { RiskEvidence("이미지 분석 근거", it) },
        imageModelResult = true,
        imageOverallDecision = decision,
        imageOverallDecisionLabel = "판단 불가".takeIf { decision == null },
        imageGenerationDecision = generation?.decision,
        imageGenerationDisplayScore = generation?.displayScore.validDisplayScore(),
        imageGenerationDetails = generation?.toDetails(),
        imageManipulationDecision = manipulation?.decision,
        imageManipulationDisplayScore = manipulation?.displayScore.validDisplayScore(),
        imageManipulationDetails = manipulation?.toDetails(),
        imageSummary = imageSummary,
    )
    return metadata.copy(result = result, imageSummaryStatus = summaryStatus)
}

/** The BE image verdict uses model outcomes. Old records can have no usable model outcome. */
private fun storedImageDecision(
    generation: AnalysisModelResultDto?,
    manipulation: AnalysisModelResultDto?,
): String? {
    val generationDecision = generation?.decision.takeIf { generation?.status == "SUCCEEDED" }
    val manipulationDecision = manipulation?.decision.takeIf { manipulation?.status == "SUCCEEDED" }
    return when {
        generationDecision == "AI_GENERATED_SUSPECTED" || manipulationDecision == "MANIPULATION_SUSPECTED" ->
            "GENERATION_OR_MANIPULATION_SUSPECTED"
        generationDecision == "NO_CLEAR_GENERATION_SIGNAL" && manipulationDecision == "NO_CLEAR_MANIPULATION_SIGNAL" ->
            "NO_CLEAR_SIGNAL"
        generationDecision in setOf("NO_CLEAR_GENERATION_SIGNAL", "INCONCLUSIVE") ||
            manipulationDecision in setOf("NO_CLEAR_MANIPULATION_SIGNAL", "INCONCLUSIVE") ||
            generation?.status == "FAILED" || manipulation?.status == "FAILED" -> "INCONCLUSIVE"
        else -> null
    }
}

private fun Double?.validDisplayScore(): Double? = this?.takeIf { it.isFinite() && it in 0.0..100.0 }

private fun String?.toStoredInstant(): Instant? = this?.let { runCatching { Instant.parse(it) }.getOrNull() }

private fun historyType(type: String?): HistoryType = when (type) {
    "IMAGE" -> HistoryType.IMAGE
    "SCREEN" -> HistoryType.SCREEN
    "TEXT" -> HistoryType.TEXT
    "PHONE" -> HistoryType.PHONE
    else -> HistoryType.UNKNOWN
}

private fun phoneRiskLevel(value: String?): RiskLevel =
    RiskLevel.entries.firstOrNull { it.name == value } ?: RiskLevel.UNKNOWN
