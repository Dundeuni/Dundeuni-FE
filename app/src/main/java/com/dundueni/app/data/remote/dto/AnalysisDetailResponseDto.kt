package com.dundueni.app.data.remote.dto

/** GET /api/analysis/{analysisId}. */
data class AnalysisDetailResponseDto(
    val isSuccess: Boolean? = null,
    val code: String? = null,
    val message: String? = null,
    val result: AnalysisDetailDto? = null,
)

data class AnalysisDetailDto(
    val analysisId: String? = null,
    val type: String? = null,
    val sourceType: String? = null,
    val textResult: TextAnalysisTextResultDto? = null,
    val status: String? = null,
    val aiGenerationScore: Double? = null,
    val aiRiskLevel: String? = null,
    val reasons: List<AnalysisReasonDto?>? = null,
    val modelVersion: String? = null,
    val errorCode: String? = null,
    val models: AnalysisImageModelsDto? = null,
    val overallResult: AnalysisOverallResultDto? = null,
    val guides: List<String?>? = null,
    val imageResultStatus: String? = null,
    val modelResults: List<AnalysisModelResultDto?>? = null,
    val result: NormalizedAnalysisResultDto? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val completedAt: String? = null,
    val expiresAt: String? = null,
)

data class AnalysisImageModelsDto(
    val status: String? = null,
    val generation: AnalysisModelResultDto? = null,
    val manipulation: AnalysisModelResultDto? = null,
)

data class AnalysisOverallResultDto(
    val decision: String? = null,
    val decisionLabel: String? = null,
    val policyVersion: String? = null,
    val suspectedKinds: List<String?>? = null,
    val uncertainKinds: List<String?>? = null,
)

data class AnalysisModelResultDto(
    val kind: String? = null,
    val status: String? = null,
    val displayScore: Double? = null,
    val decision: String? = null,
    val reasons: List<AnalysisReasonDto?>? = null,
    val limitations: List<String?>? = null,
    val modelName: String? = null,
    val modelVersion: String? = null,
    val policyVersion: String? = null,
    val errorCode: String? = null,
    val retryable: Boolean? = null,
)

data class NormalizedAnalysisResultDto(
    val riskLevel: String? = null,
    val textDecision: String? = null,
    val riskScore: Double? = null,
    val scoreScale: String? = null,
    val scoreMeaning: String? = null,
    val reasons: List<String?>? = null,
    val guidance: List<String?>? = null,
    val summary: String? = null,
)
