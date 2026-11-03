package com.dundueni.app.feature.analysis

import com.dundueni.app.data.model.AnalysisResult

/**
 * 검사 화면이 무엇을 보여 줄지 결정하는 현재 상태를 표현한다.
 * Idle은 대기, Loading은 입력 준비와 분석 진행 중인 상태다.
 * Success에는 표시할 결과를 담고, Error는 실패 안내와 재시도를 위한 상태다.
 * ViewModel이 상태를 바꾸고 AnalysisFlowScreen이 이를 받아 화면을 구성한다.
 */
sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data object Loading : AnalysisUiState
    data class Success(val result: AnalysisResult) : AnalysisUiState
    // Preserve a safe category, never the exception or raw server message.
    data class Error(
        val kind: AnalysisErrorKind = AnalysisErrorKind.UNKNOWN,
        val httpStatus: Int? = null,
    ) : AnalysisUiState
}

enum class AnalysisErrorKind(val message: String) {
    INPUT("이미지를 읽거나 변환하지 못했습니다. 다시 시도하거나 이미지를 다시 선택해 주세요."),
    CONFIGURATION("분석 서버 연결 설정을 확인해 주세요."),
    CONNECTION("분석 서버에 연결하지 못했습니다. 네트워크와 서버 실행 상태를 확인해 주세요."),
    HTTP("분석 서버가 요청을 정상 처리하지 못했습니다."),
    TIMEOUT("분석 서버 응답 시간이 초과되었습니다. 다시 시도해 주세요."),
    RESPONSE("분석 서버 응답 형식을 확인할 수 없습니다."),
    IN_PROGRESS("서버 분석이 아직 완료되지 않았습니다. 다시 시도하면 새 분석 요청을 보냅니다."),
    SERVER("서버에서 이미지 분석에 실패했습니다. 다시 시도해 주세요."),
    UNKNOWN("검사에 실패했습니다. 다시 시도하거나 이미지를 다시 선택해 주세요."),
}
