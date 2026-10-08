package com.dundueni.app.feature.analysis

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dundueni.app.data.model.AnalysisResult

/**
 * 전달받은 검사 상태에 맞춰 준비·로딩·결과·실패 안내를 보여 주는 화면이다.
 * 직접 이미지를 읽거나 API를 호출하지 않고 결과와 동작을 외부에서 받는다.
 * 재시도 버튼은 Activity의 입력 준비 흐름을 다시 시작하도록 요청한다.
 * 닫기 버튼도 전달받은 동작을 실행하며 화면 종료와 자원 정리는 Activity가 맡는다.
 * 현재는 Mock 검사임을 알리고 결과를 간단한 텍스트로 표시한다.
 */
@Composable
fun AnalysisFlowScreen(state: AnalysisUiState, onRetry: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Mock 검사 — 실제 이미지 분석 결과가 아닙니다.")
        when (state) {
            AnalysisUiState.Idle -> Text("검사 준비 중")
            AnalysisUiState.Loading -> {
                CircularProgressIndicator()
                Text("분석 중")
            }
            is AnalysisUiState.Success -> AnalysisResultContent(state.result)
            AnalysisUiState.Error -> {
                Text("검사에 실패했습니다. 다시 시도하거나 이미지를 다시 선택해 주세요.")
                Button(onClick = onRetry) { Text("다시 시도") }
            }
        }
        Button(onClick = onClose) { Text("닫기") }
    }
}

/** PB-05 연결 지점. 현재는 승인된 임시 텍스트 표시만 하며 별도 디자인을 정의하지 않습니다. */
@Composable
fun AnalysisResultContent(result: AnalysisResult) {
    Text("위험도: ${result.riskLevel}")
    Text("판단 근거")
    result.reasons.forEach { Text(it) }
    Text("대응 행동")
    result.recommendedActions.forEach { Text(it) }
}
