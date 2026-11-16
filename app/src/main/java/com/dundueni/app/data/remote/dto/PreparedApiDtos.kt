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
    val size: Int? = null,
    val totalElements: Int? = null,
    val totalPages: Int? = null,
)

data class HistoryPageItemDto(
    val historyId: String? = null,
    val analysisId: String? = null,
    val type: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val summaryStatus: String? = null,
    val createdAt: String? = null,
)

data class RecentHistoryDto(val items: List<RecentHistoryItemDto>? = null)

/** The ZIP has two different item shapes for /recent. Preserve both without choosing a UI mapping. */
data class RecentHistoryItemDto(
    val historyId: String? = null,
    val analysisId: String? = null,
    val type: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val summaryStatus: String? = null,
    val maskedSummary: String? = null,
    val riskLevel: String? = null,
    val createdAt: String? = null,
)

data class DeleteHistoryDto(val deleted: Boolean? = null)
data class DeleteAllHistoryDto(val deletedCount: Int? = null)

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
