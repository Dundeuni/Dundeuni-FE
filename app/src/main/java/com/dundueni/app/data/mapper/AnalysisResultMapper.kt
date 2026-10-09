package com.dundueni.app.data.mapper

import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.data.remote.dto.AnalysisResponseDto
import com.dundueni.app.data.remote.dto.AnalysisResponseResultDto

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
