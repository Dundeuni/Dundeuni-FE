package com.dundueni.app.data.remote.dto

/** Draft API responses are nullable so absent fields cannot masquerade as valid values. */
data class PreparedApiEnvelope<T>(
    val isSuccess: Boolean? = null,
    val code: String? = null,
    val message: String? = null,
    val result: T? = null,
)

data class HistoryPageDto(
    val items: List<HistoryPageItemDto>? = null,
    val page: Int? = null,
    val limit: Int? = null,
    val total: Int? = null,
    val hasNext: Boolean? = null,
)

data class HistoryPageItemDto(
    val analysisId: String? = null,
    val type: String? = null,
    val targetSummary: String? = null,
    val status: String? = null,
    val aiRiskLevel: String? = null,
    val createdAt: String? = null,
)

data class HistoryDetailDto(
    val analysisId: String? = null,
    val type: String? = null,
    val status: String? = null,
    val aiGenerationScore: Double? = null,
    val aiRiskLevel: String? = null,
    val reasons: List<HistoryReasonDto>? = null,
    val createdAt: String? = null,
    val completedAt: String? = null,
    val detail: HistoryTypeDetailDto? = null,
)

data class HistoryReasonDto(val description: String? = null)

/** The record type selects the fields supplied by each history feature branch. */
data class HistoryTypeDetailDto(
    // IMAGE
    val summary: String? = null,
    val summaryStatus: String? = null,
    val summaryModel: String? = null,
    val summarizedAt: String? = null,
    // TEXT
    val textPreview: String? = null,
    val decision: String? = null,
    val riskScore: Double? = null,
    val scoreMeaning: String? = null,
    val modelVersion: String? = null,
    val policyVersion: String? = null,
    val needsReview: Boolean? = null,
    // PHONE
    val maskedPhoneNumber: String? = null,
    val riskLevel: String? = null,
    val reportCount: Int? = null,
    val reportCountIsRange: Boolean? = null,
    val reasons: List<String>? = null,
    val analyzedAt: String? = null,
)

data class DeleteHistoryDto(val deletedCount: Int? = null)

data class OnboardingRequestDto(
    val easyMode: Boolean,
    val ageRange: String? = null,
    val completed: Boolean = true,
)

/** Null means omitted on PATCH. Gson's default converter omits null fields. */
data class SettingsPatchRequestDto(
    val easyMode: Boolean? = null,
    val saveHistory: Boolean? = null,
    val retentionDays: Int? = null,
)

data class UserSettingsDto(
    val easyMode: Boolean? = null,
    val saveHistory: Boolean? = null,
    val retentionDays: Int? = null,
    val onboardingCompleted: Boolean? = null,
    val ageRange: String? = null,
)

data class UsageDto(
    val plan: String? = null,
    val limit: Int? = null,
    val used: Int? = null,
    val reserved: Int? = null,
    val remaining: Int? = null,
    val resetsAt: String? = null,
)

data class HomeDashboardResponseDto(
    val success: Boolean? = null,
    val data: HomeDashboardDto? = null,
)

data class HomeDashboardDto(
    val floating: HomeFloatingDto? = null,
    val recentAnalyses: List<HomeRecentAnalysisDto>? = null,
)

data class HomeFloatingDto(val enabled: Boolean? = null)

data class HomeRecentAnalysisDto(
    val analysisId: Long? = null,
    val analysisType: String? = null,
    val analyzedAt: String? = null,
    val riskLevel: String? = null,
)

data class SafetyTipsResponseDto(
    val success: Boolean? = null,
    val data: SafetyTipsDto? = null,
)

data class SafetyTipsDto(val tips: List<SafetyTipDto>? = null)

data class SafetyTipDto(
    val tipId: Long? = null,
    val category: String? = null,
    val content: String? = null,
)

data class RetryAnalysisRequestDto(val failedElementIds: List<String>? = null)

data class RetryAnalysisDto(
    val analysisId: String? = null,
    val status: String? = null,
    val expiresAt: String? = null,
)

data class ImageUploadLookupDto(
    val uploadId: String? = null,
    val status: String? = null,
    val image: ImageUploadLookupImageDto? = null,
    val detectorVersion: String? = null,
    val expiresAt: String? = null,
    val original: ImageUploadLookupElementDto? = null,
    val elements: List<ImageUploadLookupElementDto>? = null,
)

data class ImageUploadLookupImageDto(
    val width: Int? = null,
    val height: Int? = null,
    val coordinateSpace: String? = null,
)

data class ImageUploadLookupElementDto(
    val elementId: String? = null,
    val type: String? = null,
    val bbox: ImageUploadLookupBoxDto? = null,
)

data class ImageUploadLookupBoxDto(
    val left: Int? = null,
    val top: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
)
