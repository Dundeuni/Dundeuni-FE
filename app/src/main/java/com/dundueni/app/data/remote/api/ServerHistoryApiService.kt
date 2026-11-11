package com.dundueni.app.data.remote.api

import com.dundueni.app.data.remote.network.BearerAuthInterceptor
import com.dundueni.app.data.remote.dto.TextAnalysisTextResultDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface ServerHistoryApiService {
    @Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @GET("/api/history")
    suspend fun list(@Query("limit") limit: Int = 20): Response<HistoryEnvelope<HistoryItemsDto>>

    @Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @GET("/api/history/{analysisId}")
    suspend fun detail(@Path("analysisId") analysisId: String): Response<HistoryEnvelope<HistoryEntryDto>>
}

data class HistoryEnvelope<T>(
    val isSuccess: Boolean? = null,
    val code: String? = null,
    val message: String? = null,
    val result: T? = null,
)

data class HistoryItemsDto(val items: List<HistoryEntryDto>? = null)

data class HistoryEntryDto(
    val id: String? = null,
    val type: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val completedAt: String? = null,
    val expiresAt: String? = null,
    val errorCode: String? = null,
    val textPreview: String? = null,
    val textResult: TextAnalysisTextResultDto? = null,
    val phoneResult: HistoryPhoneResultDto? = null,
)

data class HistoryPhoneResultDto(
    val phoneNumber: String? = null,
    val provider: String? = null,
    val riskLevel: String? = null,
    val spamType: String? = null,
    val reportCount: Int? = null,
    val reasons: List<String>? = null,
    val policyVersion: String? = null,
    val analyzedAt: String? = null,
)
