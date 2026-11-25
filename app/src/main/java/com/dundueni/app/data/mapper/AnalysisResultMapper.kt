package com.dundueni.app.data.mapper

import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.data.model.ImageActionGuide
import com.dundueni.app.data.model.ImageAnalysisSummary
import com.dundueni.app.data.model.ImageDetailedReason
import com.dundueni.app.data.model.ImageModelDetails
import com.dundueni.app.data.remote.api.TextAnalysisResponseResult
import com.dundueni.app.data.remote.dto.AnalysisResponseDto
import com.dundueni.app.data.remote.dto.AnalysisResponseResultDto
import com.dundueni.app.data.remote.dto.AnalysisDetailDto
import com.dundueni.app.data.remote.dto.AnalysisModelResultDto

/** Maps the BE result payload while preserving its values and treating absent collections as empty. */
fun AnalysisResponseDto.toDomain(): AnalysisResult =
    requireNotNull(result) { "Analysis response result is missing" }.toDomain()

fun AnalysisResponseResultDto.toDomain(): AnalysisResult = AnalysisResult(
    analysisId = analysisId,
    type = type,
    status = status,
    aiGenerationScore = aiGenerationScore,
    aiRiskLevel = aiRiskLevel,
    reasons = reasons.orEmpty().mapNotNull { it?.description },
    modelVersion = modelVersion,
    errorCode = errorCode,
    createdAt = createdAt,
    completedAt = completedAt,
    expiresAt = expiresAt
)

/** GET uses the documented outer fields; older normalized results remain readable. */
fun AnalysisDetailDto.toDomain(): AnalysisResult {
    val normalized = result
    val text = textResult.takeIf { type == "TEXT" }
    return AnalysisResult(
        analysisId = analysisId,
        type = type,
        status = status,
        aiGenerationScore = aiGenerationScore,
        aiRiskLevel = aiRiskLevel,
        reasons = (normalized?.reasons ?: reasons?.map { it?.description }).orEmpty()
            .filterNotNull().filter { it.isNotBlank() },
        modelVersion = modelVersion,
        errorCode = errorCode,
        createdAt = createdAt,
        completedAt = completedAt,
        expiresAt = expiresAt,
        recommendedActions = (guides ?: normalized?.guidance).orEmpty()
            .filterNotNull().filter { it.isNotBlank() },
        summary = text?.summary ?: normalized?.summary,
        normalizedRiskLevel = normalized?.riskLevel,
        textDecision = text?.decision ?: normalized?.textDecision,
        riskScore = if (text != null) text.riskScore else normalized?.riskScore,
        sourceType = text?.sourceType ?: sourceType,
        textNeedsReview = text?.quality?.needsReview,
        textReasonCodes = text?.quality?.reasonCodes,
        textOcrUsed = text?.quality?.ocrUsed,
        textEvidence = text?.evidence.orEmpty().filter { it.isNotBlank() },
        textLimitations = text?.limitations.orEmpty().filter { it.isNotBlank() },
        imageOverallDecisionLabel = if (type == "IMAGE") overallResult?.decisionLabel else null,
        imageGenerationDecision = if (type == "IMAGE") models?.generation?.decision else null,
        imageGenerationDisplayScore = if (type == "IMAGE") models?.generation?.displayScore else null,
        imageManipulationDecision = if (type == "IMAGE") models?.manipulation?.decision else null,
        imageManipulationDisplayScore = if (type == "IMAGE") models?.manipulation?.displayScore else null,
        imageGenerationDetails = if (type == "IMAGE") models?.generation?.toDetails() else null,
        imageManipulationDetails = if (type == "IMAGE") models?.manipulation?.toDetails() else null,
        hasImageModels = type == "IMAGE" && models != null,
        imageOverallDecision = if (type == "IMAGE") overallResult?.decision else null,
        imageSummary = if (type == "IMAGE") toImageSummary() else null,
    )
}

private fun AnalysisDetailDto.toImageSummary() = ImageAnalysisSummary(
    summaryStatus = summaryStatus,
    summary = summary,
    detailedReasons = detailedReasons.orEmpty().mapNotNull { reason ->
        val type = reason?.type ?: return@mapNotNull null
        val title = reason.title ?: return@mapNotNull null
        val description = reason.description ?: return@mapNotNull null
        ImageDetailedReason(type, title, description)
    },
    actionGuides = actionGuides.orEmpty().mapNotNull { guide ->
        val title = guide?.title ?: return@mapNotNull null
        val description = guide.description ?: return@mapNotNull null
        ImageActionGuide(title, description)
    },
    reassurance = reassurance,
    summaryModel = summaryModel,
    summarizedAt = summarizedAt,
)

private fun AnalysisModelResultDto.toDetails() = ImageModelDetails(
    status = status,
    reasons = reasons.orEmpty().mapNotNull { it?.description?.takeIf(String::isNotBlank) },
    limitations = limitations.orEmpty().filterNotNull().filter(String::isNotBlank),
)

/** Text POST contains a classifier decision, not a normalized final fraud verdict. */
fun TextAnalysisResponseResult.toDomain(): AnalysisResult = AnalysisResult(
    analysisId = analysisId, type = type ?: "TEXT", status = status,
    aiGenerationScore = null, aiRiskLevel = null, reasons = emptyList(),
    modelVersion = null, errorCode = null, createdAt = null, completedAt = null, expiresAt = null,
    textDecision = textResult?.decision,
    riskScore = textResult?.riskScore,
    sourceType = textResult?.sourceType,
    textNeedsReview = textResult?.quality?.needsReview,
    textReasonCodes = textResult?.quality?.reasonCodes,
    textOcrUsed = textResult?.quality?.ocrUsed,
    summary = textResult?.summary,
    textEvidence = textResult?.evidence.orEmpty().filter { it.isNotBlank() },
    textLimitations = textResult?.limitations.orEmpty().filter { it.isNotBlank() },
)
