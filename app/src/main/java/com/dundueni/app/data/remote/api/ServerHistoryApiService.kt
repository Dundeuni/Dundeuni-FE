package com.dundueni.app.data.remote.api

import com.dundueni.app.data.remote.network.BearerAuthInterceptor
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

/** Immediate phone result lookup on demo/all-features. */
internal interface ServerHistoryApiService {
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

data class HistoryEntryDto(
    val id: String? = null,
    val type: String? = null,
    val status: String? = null,
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
