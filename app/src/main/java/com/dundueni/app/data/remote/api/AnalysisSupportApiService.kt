package com.dundueni.app.data.remote.api

import com.dundueni.app.data.remote.dto.ImageUploadLookupDto
import com.dundueni.app.data.remote.dto.PreparedApiEnvelope
import com.dundueni.app.data.remote.dto.RetryAnalysisDto
import com.dundueni.app.data.remote.dto.RetryAnalysisRequestDto
import com.dundueni.app.data.remote.network.BearerAuthInterceptor
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

/** Auxiliary endpoints do not change the existing image and screen analysis flow. */
interface AnalysisSupportApiService {
    @Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @POST("/api/analysis/{analysisId}/retry")
    suspend fun retry(
        @Path("analysisId") analysisId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: RetryAnalysisRequestDto,
    ): Response<PreparedApiEnvelope<RetryAnalysisDto>>

    @Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @GET("/api/analysis/image-uploads/{uploadId}")
    suspend fun lookupUpload(@Path("uploadId") uploadId: String): Response<PreparedApiEnvelope<ImageUploadLookupDto>>
}
