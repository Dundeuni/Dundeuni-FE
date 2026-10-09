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
    /** The current BE response does not contain actions; kept empty until the contract adds them. */
    val recommendedActions: List<String> = emptyList()
) {
    /** Compatibility accessor for the existing result screen; value remains the raw BE string. */
    val riskLevel: String? get() = aiRiskLevel
}
