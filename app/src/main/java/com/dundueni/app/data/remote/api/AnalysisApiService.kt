package com.dundueni.app.data.remote.api

import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.Part

/**
 * 분석 서버에 어떤 방식으로 이미지를 보낼지 정의하는 API 계약이다.
 * Retrofit은 아래 요청 방식과 경로를 읽어 실제 HTTP 호출 구현을 만든다.
 * Multipart 요청에 type=IMAGE 일반 필드와 file 이미지 Part를 넣는다.
 * JSON 해석과 앱 결과 모델 변환은 Repository에 맡긴다.
 * Fake도 같은 인터페이스를 구현하므로 호출 측은 같은 방식으로 사용할 수 있다.
 */
interface AnalysisApiService {
    @Multipart
    @HTTP(method = AnalysisApiContract.METHOD, path = AnalysisApiContract.PATH, hasBody = true)
    suspend fun analyzeImage(
        @Part image: MultipartBody.Part,
        // RequestBody를 사용해 Gson의 JSON 문자열 인코딩 없이 IMAGE를 전송한다.
        @Part(AnalysisApiContract.TYPE_FIELD) type: RequestBody =
            AnalysisApiContract.IMAGE_TYPE.toRequestBody("text/plain".toMediaType())
    ): Response<ResponseBody>
}
