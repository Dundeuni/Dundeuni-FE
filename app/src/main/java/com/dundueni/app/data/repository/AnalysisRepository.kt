package com.dundueni.app.data.repository

import com.dundueni.app.data.remote.api.AnalysisApiService
import com.dundueni.app.data.mapper.toDomain
import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.data.remote.dto.AnalysisResponseDto
import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * 실제 Retrofit 서비스 또는 Fake를 주입받는 분석 요청 진입점입니다.
 * analyzeImage는 기존 원문 응답을 반환하며 호출자가 body()/errorBody()를 닫습니다.
 * analyzeResult는 DTO를 Domain으로 변환하고 본문을 직접 닫습니다.
 * 통신 오류와 코루틴 취소는 호출자에게 그대로 전파합니다.
 */
/**
 * 검사 상태를 관리하는 ViewModel과 분석 API 사이에서 응답 처리를 맡는다.
 * 원문이 필요한 호출에는 analyzeImage로 HTTP 응답을 그대로 제공한다.
 * 화면에 쓸 결과가 필요한 호출에는 analyzeResult로 앱 모델을 제공한다.
 * API 응답의 JSON을 DTO로 읽고 Mapper를 거쳐 AnalysisResult로 바꾼다.
 * 실패는 예외로 전달하며, 화면에 어떻게 표시할지는 ViewModel이 결정한다.
 */
class AnalysisRepository(private val apiService: AnalysisApiService) : AnalysisResultRepository {
    suspend fun analyzeImage(image: MultipartBody.Part): Response<ResponseBody> =
        apiService.analyzeImage(image)

    /** 상태 계층용 요청. 본문 읽기/파싱은 IO에서 수행하고 응답 리소스를 닫습니다. */
    override suspend fun analyzeResult(image: MultipartBody.Part): AnalysisResult = withContext(Dispatchers.IO) {
        val response = analyzeImage(image)
        // HTTP 실패를 정상 결과와 구분하고, 오류 본문은 닫은 뒤 예외로 알린다.
        if (!response.isSuccessful) {
            response.errorBody()?.close()
            throw IOException("Analysis request failed: HTTP ${response.code()}")
        }
        val body = response.body() ?: throw IOException("Analysis response body is missing")
        // JSON 형식이 맞지 않으면 파싱 예외가 전달되며, 응답 본문은 성공 여부와 관계없이 닫힌다.
        val dto = body.use {
            Gson().fromJson(it.charStream(), AnalysisResponseDto::class.java)
        } ?: throw IOException("Analysis response is empty or null")
        // 서버 응답 형태를 벗어나 앱에서 사용할 결과 모델로 반환한다.
        dto.toDomain()
    }
}
