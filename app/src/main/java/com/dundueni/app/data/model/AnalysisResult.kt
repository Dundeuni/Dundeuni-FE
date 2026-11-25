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
    val textNeedsReview: Boolean? = null,
    val textReasonCodes: List<String>? = null,
    val textOcrUsed: Boolean? = null,
    val textEvidence: List<String> = emptyList(),
    val textLimitations: List<String> = emptyList(),
    val imageOverallDecisionLabel: String? = null,
    val imageGenerationDecision: String? = null,
    val imageGenerationDisplayScore: Double? = null,
    val imageManipulationDecision: String? = null,
    val imageManipulationDisplayScore: Double? = null,
    val imageGenerationDetails: ImageModelDetails? = null,
    val imageManipulationDetails: ImageModelDetails? = null,
    val hasImageModels: Boolean = false,
    val imageOverallDecision: String? = null,
    /** Integrated IMAGE detail summary. The legacy summary remains available to existing callers. */
    val imageSummary: ImageAnalysisSummary? = null,
    val textAnalysisPhoneNumbers: TextAnalysisPhoneNumbers? = null,
) {
    val extractedPhoneNumbers: List<ExtractedPhoneNumber>
        get() = textAnalysisPhoneNumbers?.takeIf { it.analysisId == analysisId }?.numbers.orEmpty()

    /** Compatibility accessor for the existing result screen; only the normalized final verdict is exposed. */
    val riskLevel: String? get() = normalizedRiskLevel
}

data class ExtractedPhoneNumber(val phoneNumber: String, val displayNumber: String, val source: String)

/** Phone numbers returned only by the TEXT or OCR POST, scoped to its analysis ID. */
data class TextAnalysisPhoneNumbers(
    val analysisId: String,
    val numbers: List<ExtractedPhoneNumber>,
) {
    internal fun retaining(previous: TextAnalysisPhoneNumbers?): TextAnalysisPhoneNumbers =
        previous?.takeIf { it.analysisId == analysisId && numbers.isEmpty() } ?: this
}

internal fun AnalysisResult.withExtractedPhoneNumbers(phones: TextAnalysisPhoneNumbers?): AnalysisResult =
    if (phones != null && phones.analysisId == analysisId && phones.numbers.isNotEmpty()) {
        copy(textAnalysisPhoneNumbers = phones)
    } else this

data class ImageAnalysisSummary(
    val summaryStatus: String? = null,
    val summary: String? = null,
    val detailedReasons: List<ImageDetailedReason> = emptyList(),
    val actionGuides: List<ImageActionGuide> = emptyList(),
    val reassurance: String? = null,
    val summaryModel: String? = null,
    val summarizedAt: String? = null,
)

data class ImageDetailedReason(val type: String, val title: String, val description: String)

data class ImageActionGuide(val title: String, val description: String)

data class ImageModelDetails(
    val status: String? = null,
    val reasons: List<String> = emptyList(),
    val limitations: List<String> = emptyList(),
)
