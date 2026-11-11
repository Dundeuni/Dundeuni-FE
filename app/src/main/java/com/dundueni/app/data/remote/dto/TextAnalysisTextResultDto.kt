package com.dundueni.app.data.remote.dto

/** TEXT result shared by analysis POST, detail GET, and history detail. */
data class TextAnalysisTextResultDto(
    val decision: String? = null,
    val riskScore: Double? = null,
    val sourceType: String? = null,
    val quality: TextAnalysisQualityDto? = null,
)

data class TextAnalysisQualityDto(
    val needsReview: Boolean? = null,
    val reasonCodes: List<String>? = null,
    val ocrUsed: Boolean? = null,
)
