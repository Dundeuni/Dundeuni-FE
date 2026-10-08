package com.dundueni.app.data.model

/** JSON/Retrofit에 의존하지 않는 앱 내부 분석 결과입니다. */
/**
 * 서버 응답을 정리한 뒤 검사 상태와 결과 화면에 전달하는 앱 모델이다.
 * 위험도와 판단 근거, 대응 행동을 하나의 결과로 묶는다.
 * DTO의 JSON 필드명 대신 앱에서 사용할 이름과 타입을 가진다.
 * Repository가 Mapper로 만들고 ViewModel과 화면이 이 결과를 사용한다.
 */
data class AnalysisResult(
    val riskLevel: RiskLevel,
    val reasons: List<String>,
    val recommendedActions: List<String>
)

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    // 누락되거나 해석할 수 없는 값이며, 안전하다는 의미가 아닙니다.
    UNKNOWN
}
