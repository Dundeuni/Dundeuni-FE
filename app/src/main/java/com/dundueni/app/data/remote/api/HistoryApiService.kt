package com.dundueni.app.data.remote.api

import com.dundueni.app.data.remote.dto.DeleteAllHistoryDto
import com.dundueni.app.data.remote.dto.DeleteHistoryDto
import com.dundueni.app.data.remote.dto.HistoryPageDto
import com.dundueni.app.data.remote.dto.PreparedApiEnvelope
import com.dundueni.app.data.remote.dto.RecentHistoryDto
import com.dundueni.app.data.remote.network.BearerAuthInterceptor
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

/** Prepared only: history management is not wired to the local HistoryStore or UI. */
interface HistoryApiService {
    @Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @GET("/api/analysis-history")
    suspend fun list(
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
    ): Response<PreparedApiEnvelope<HistoryPageDto>>

    @Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @GET("/api/analysis-history/recent")
    suspend fun recent(): Response<PreparedApiEnvelope<RecentHistoryDto>>

    // GET /analysis-history/{analysisId} is withheld: its ZIP page describes /recent instead.

    @Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @DELETE("/api/analysis-history/{analysisId}")
    suspend fun deleteOne(@Path("analysisId") analysisId: String): Response<PreparedApiEnvelope<DeleteHistoryDto>>

    @Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @DELETE("/api/analysis-history")
    suspend fun deleteAll(): Response<PreparedApiEnvelope<DeleteAllHistoryDto>>
}
