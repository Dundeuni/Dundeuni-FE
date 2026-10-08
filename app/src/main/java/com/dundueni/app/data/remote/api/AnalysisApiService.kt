package com.dundueni.app.data.remote.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.Part

/**
 * 분석 서버에 어떤 방식으로 이미지를 보낼지 정의하는 API 계약이다.
 * Retrofit은 아래 요청 방식과 경로를 읽어 실제 HTTP 호출 구현을 만든다.
 * Multipart 요청에 이미지 Part 한 개를 넣고 HTTP 상태와 원문 본문을 받는다.
 * JSON 해석과 앱 결과 모델 변환은 Repository에 맡긴다.
 * Fake도 같은 인터페이스를 구현하므로 호출 측은 같은 방식으로 사용할 수 있다.
 */
interface AnalysisApiService {
    @Multipart
    @HTTP(method = AnalysisApiContract.METHOD, path = AnalysisApiContract.PATH, hasBody = true)
    suspend fun analyzeImage(@Part image: MultipartBody.Part): Response<ResponseBody>
}
