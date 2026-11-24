package com.dundueni.app.data.remote.api

import com.dundueni.app.data.remote.dto.DeleteHistoryDto
import com.dundueni.app.data.remote.dto.HistoryDetailDto
import com.dundueni.app.data.remote.dto.HistoryPageDto
import com.dundueni.app.data.remote.dto.PreparedApiEnvelope
import com.dundueni.app.data.remote.network.BearerAuthInterceptor
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface HistoryApiService {
    @Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @GET("/api/analysis/history")
    suspend fun list(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Response<PreparedApiEnvelope<HistoryPageDto>>

    @Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @GET("/api/analysis/history/{analysisId}")
    suspend fun detail(@Path("analysisId") analysisId: String): Response<PreparedApiEnvelope<HistoryDetailDto>>

    @Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @DELETE("/api/analysis/history/{analysisId}")
    suspend fun deleteOne(@Path("analysisId") analysisId: String): Response<PreparedApiEnvelope<DeleteHistoryDto>>

    @Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @DELETE("/api/analysis/history")
    suspend fun deleteAll(): Response<PreparedApiEnvelope<DeleteHistoryDto>>
}
