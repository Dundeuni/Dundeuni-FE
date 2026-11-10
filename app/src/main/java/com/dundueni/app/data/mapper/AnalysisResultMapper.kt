package com.dundueni.app.data.mapper

import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.data.remote.api.TextAnalysisResponseResult
import com.dundueni.app.data.remote.dto.AnalysisResponseDto
import com.dundueni.app.data.remote.dto.AnalysisResponseResultDto
import com.dundueni.app.data.remote.dto.AnalysisDetailDto

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

/** GET uses result.result for common findings; outer IMAGE fields keep their original meaning. */
fun AnalysisDetailDto.toDomain(): AnalysisResult {
    val normalized = requireNotNull(result) { "Normalized analysis result is missing" }
    return AnalysisResult(
        analysisId = analysisId,
        type = type,
        status = status,
        aiGenerationScore = aiGenerationScore,
        aiRiskLevel = aiRiskLevel,
        reasons = normalized.reasons.orEmpty().filterNotNull().filter { it.isNotBlank() },
        modelVersion = modelVersion,
        errorCode = errorCode,
        createdAt = createdAt,
        completedAt = completedAt,
        expiresAt = expiresAt,
        recommendedActions = normalized.guidance.orEmpty().filterNotNull().filter { it.isNotBlank() },
        summary = normalized.summary,
        normalizedRiskLevel = normalized.riskLevel,
        textDecision = normalized.textDecision,
        riskScore = normalized.riskScore,
        sourceType = sourceType,
    )
}

/** Text POST contains a classifier decision, not a normalized final fraud verdict. */
fun TextAnalysisResponseResult.toDomain(): AnalysisResult = AnalysisResult(
    analysisId = analysisId, type = type ?: "TEXT", status = status,
    aiGenerationScore = null, aiRiskLevel = null, reasons = emptyList(),
    modelVersion = null, errorCode = null, createdAt = null, completedAt = null, expiresAt = null,
    textDecision = textResult?.decision,
    sourceType = textResult?.sourceType,
)
