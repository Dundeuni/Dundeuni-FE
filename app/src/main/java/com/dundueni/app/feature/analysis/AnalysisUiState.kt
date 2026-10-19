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
    // HTTP/파싱 예외와 서버 원문을 UI 상태에 노출하지 않습니다.
    data object Error : AnalysisUiState
}
