package com.dundueni.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * BE 계약 확정 전 Mock 응답 구조입니다. 필드명과 nullable 규칙은 확정 시 재검토합니다.
 * 위험도 문자열 및 JSON의 누락/null은 Mapper에서 앱 모델로 변환합니다.
 */
/**
 * 서버 응답 JSON의 모양을 그대로 받아 두는 DTO다.
 * SerializedName으로 JSON 필드명과 Kotlin 속성을 연결한다.
 * 서버 표현을 받는 단계이므로 위험도는 아직 문자열로 보관한다.
 * Repository가 JSON을 이 객체로 읽고 Mapper가 앱 모델로 정리한다.
 * 화면은 이 DTO 대신 변환된 AnalysisResult를 사용한다.
 */
data class AnalysisResponseDto(
    @SerializedName("risk_level") val riskLevel: String? = null,
    @SerializedName("reasons") val reasons: List<String?>? = null,
    @SerializedName("recommended_actions") val recommendedActions: List<String?>? = null
)
