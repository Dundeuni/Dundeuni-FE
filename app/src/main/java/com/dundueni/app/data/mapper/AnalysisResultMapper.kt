package com.dundueni.app.data.mapper

import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.data.model.RiskLevel
import com.dundueni.app.data.remote.dto.AnalysisResponseDto

/**
 * 임시 계약의 위험도는 LOW/MEDIUM/HIGH입니다. 다른 값은 UNKNOWN으로 유지합니다.
 * 누락/null 목록은 빈 목록, null 항목은 제외하며 문자열 내용과 순서는 보존합니다.
 * 빈 목록은 정보 없음이며 임의의 판단 근거나 행동 안내를 생성하지 않습니다.
 */
/**
 * 서버 응답 DTO와 앱 내부 결과 모델 사이의 변환을 담당한다.
 * 위험도 문자열을 앱이 다룰 수 있는 RiskLevel 값으로 바꾼다.
 * 화면이 서버 필드 형식이나 누락 처리 규칙을 직접 알지 않아도 되게 한다.
 * UNKNOWN을 정상 결과로 표시할지 여부는 이후 ViewModel이 결정한다.
 */
fun AnalysisResponseDto.toDomain(): AnalysisResult = AnalysisResult(
    riskLevel = when (riskLevel) {
        "LOW" -> RiskLevel.LOW
        "MEDIUM" -> RiskLevel.MEDIUM
        "HIGH" -> RiskLevel.HIGH
        else -> RiskLevel.UNKNOWN
    },
    reasons = reasons.orEmpty().filterNotNull(),
    recommendedActions = recommendedActions.orEmpty().filterNotNull()
)
