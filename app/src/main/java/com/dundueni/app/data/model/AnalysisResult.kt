package com.dundueni.app.data.model

/** Backend analysis data exposed to the result flow without interpreting server values. */
data class AnalysisResult(
    val analysisId: String?,
    val type: String?,
    val status: String?,
    val aiGenerationScore: Double?,
    val aiRiskLevel: String?,
    val reasons: List<String>,
    val modelVersion: String?,
    val errorCode: String?,
    val createdAt: String?,
    val completedAt: String?,
    val expiresAt: String?,
    /** Normalized GET guidance; POST responses without guidance leave this empty. */
    val recommendedActions: List<String> = emptyList(),
    val summary: String? = null,
    val normalizedRiskLevel: String? = null,
    val textDecision: String? = null,
    val riskScore: Double? = null,
    val sourceType: String? = null,
) {
    /** Compatibility accessor for the existing result screen; only the normalized final verdict is exposed. */
    val riskLevel: String? get() = normalizedRiskLevel
}
