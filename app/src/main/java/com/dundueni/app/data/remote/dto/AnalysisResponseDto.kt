package com.dundueni.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** BE POST /api/analysis envelope. */
data class AnalysisResponseDto(
    @SerializedName("isSuccess") val isSuccess: Boolean? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("result") val result: AnalysisResponseResultDto? = null
)

data class AnalysisResponseResultDto(
    @SerializedName("analysisId") val analysisId: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("aiGenerationScore") val aiGenerationScore: Double? = null,
    @SerializedName("aiRiskLevel") val aiRiskLevel: String? = null,
    @SerializedName("reasons") val reasons: List<AnalysisReasonDto?>? = null,
    @SerializedName("modelVersion") val modelVersion: String? = null,
    @SerializedName("errorCode") val errorCode: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("completedAt") val completedAt: String? = null,
    @SerializedName("expiresAt") val expiresAt: String? = null
)

data class AnalysisReasonDto(
    @SerializedName("description") val description: String? = null
)

/** Error envelope returned by the BE Analysis controller exception filter. */
data class AnalysisErrorResponseDto(
    @SerializedName("isSuccess") val isSuccess: Boolean? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("result") val result: Any? = null
)
