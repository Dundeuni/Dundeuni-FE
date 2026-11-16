package com.dundueni.app.data.remote.api

import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import com.dundueni.app.data.remote.network.BearerAuthInterceptor
import retrofit2.Response
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.DELETE
import com.dundueni.app.data.remote.dto.AnalysisDetailResponseDto
import com.dundueni.app.data.remote.dto.AnalysisSummaryResponseDto
import com.dundueni.app.data.remote.dto.TextAnalysisTextResultDto

/**
 * 분석 서버에 어떤 방식으로 이미지를 보낼지 정의하는 API 계약이다.
 * Retrofit은 아래 요청 방식과 경로를 읽어 실제 HTTP 호출 구현을 만든다.
 * Multipart 요청에 type=IMAGE 일반 필드와 file 이미지 Part를 넣는다.
 * JSON 해석과 앱 결과 모델 변환은 Repository에 맡긴다.
 * Fake도 같은 인터페이스를 구현하므로 호출 측은 같은 방식으로 사용할 수 있다.
 */
interface AnalysisApiService {
    @Multipart
    @retrofit2.http.Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @HTTP(method = AnalysisApiContract.METHOD, path = AnalysisApiContract.PATH, hasBody = true)
    suspend fun analyzeImage(
        @Part image: MultipartBody.Part,
        // RequestBody를 사용해 Gson의 JSON 문자열 인코딩 없이 IMAGE를 전송한다.
        @Part(AnalysisApiContract.TYPE_FIELD) type: RequestBody =
            AnalysisApiContract.IMAGE_TYPE.toRequestBody("text/plain".toMediaType())
    ): Response<ResponseBody>
}

interface AnalysisQueryApiService {
    @retrofit2.http.Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @GET("/api/analysis/{analysisId}")
    suspend fun getAnalysis(@Path("analysisId") analysisId: String): Response<AnalysisDetailResponseDto>

    @retrofit2.http.Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @GET("/api/analysis/{analysisId}/summary")
    suspend fun getSummary(@Path("analysisId") analysisId: String): Response<AnalysisSummaryResponseDto>
}

interface PhotoSelectionApiService {
    @Multipart
    @retrofit2.http.Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @POST("/api/analysis/image-uploads")
    suspend fun upload(@Part file: MultipartBody.Part): Response<PhotoUploadResponse>

    @retrofit2.http.Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @POST("/api/analysis/image-uploads/{uploadId}/analyses")
    suspend fun select(
        @Path("uploadId") uploadId: String,
        @Body request: PhotoSelectionRequest,
    ): Response<ResponseBody>

    @retrofit2.http.Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @DELETE("/api/analysis/image-uploads/{uploadId}")
    suspend fun cancel(@Path("uploadId") uploadId: String): Response<ResponseBody>
}

data class PhotoSelectionRequest(val elementId: String)
data class PhotoUploadResponse(
    val isSuccess: Boolean? = null,
    val code: String? = null,
    val result: PhotoUpload? = null,
)
data class PhotoUpload(
    val uploadId: String? = null,
    val status: String? = null,
    val image: PhotoImage? = null,
    val expiresAt: String? = null,
    val original: PhotoCandidate? = null,
    val elements: List<PhotoCandidate>? = null,
)
data class PhotoImage(val width: Int = 0, val height: Int = 0, val coordinateSpace: String? = null)
data class PhotoCandidate(
    val elementId: String? = null,
    val type: String? = null,
    val bbox: PhotoBox? = null,
)
data class PhotoBox(val left: Int = 0, val top: Int = 0, val width: Int = 0, val height: Int = 0)

interface TextAnalysisApiService {
    @retrofit2.http.Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @POST(AnalysisApiContract.PATH)
    suspend fun analyzeText(@Body request: TextAnalysisRequest): Response<TextAnalysisResponse>

    @Multipart
    @retrofit2.http.Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @HTTP(method = AnalysisApiContract.METHOD, path = AnalysisApiContract.PATH, hasBody = true)
    suspend fun analyzeScreenCrop(
        @Part file: MultipartBody.Part,
        @Part("type") type: RequestBody = "TEXT".toRequestBody("text/plain".toMediaType()),
        @Part("sourceType") sourceType: RequestBody =
            "SCREEN_CROP".toRequestBody("text/plain".toMediaType()),
    ): Response<TextAnalysisResponse>
}

data class TextAnalysisRequest(val type: String = "TEXT", val text: String)

data class TextAnalysisResponse(
    val isSuccess: Boolean? = null,
    val code: String? = null,
    val result: TextAnalysisResponseResult? = null,
)

data class TextAnalysisResponseResult(
    val analysisId: String? = null,
    val type: String? = null,
    val status: String? = null,
    val textResult: TextAnalysisTextResultDto? = null,
)
