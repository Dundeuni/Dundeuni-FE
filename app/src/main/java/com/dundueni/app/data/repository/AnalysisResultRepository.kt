package com.dundueni.app.data.repository

import com.dundueni.app.data.model.AnalysisResult
import okhttp3.MultipartBody

/** ViewModel에 실제 Repository 또는 Fake를 주입하기 위한 최소 계약입니다. */
/**
 * 검사 상태 계층이 분석 처리에 요구하는 최소 동작을 정의한다.
 * 입력은 준비된 이미지 Part이고, 출력은 앱 내부의 AnalysisResult다.
 * ViewModel은 HTTP 응답이나 JSON 처리 방법을 알 필요가 없다.
 * 구현체를 전달받는 구조라 테스트에서는 별도의 결과 제공자로 바꿀 수 있다.
 */
fun interface AnalysisResultRepository {
    suspend fun analyzeResult(image: MultipartBody.Part): AnalysisResult
}
