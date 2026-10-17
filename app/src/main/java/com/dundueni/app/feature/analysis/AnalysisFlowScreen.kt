package com.dundueni.app.feature.analysis

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dundueni.app.BuildConfig
import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.feature.analysisresult.AnalysisResultRoute
import com.dundueni.app.ui.component.InspectionErrorDialog
import com.dundueni.app.ui.component.InspectionLoadingDialog

/**
 * 전달받은 검사 상태에 맞춰 준비·로딩·결과·실패 안내를 보여 주는 화면이다.
 * 직접 이미지를 읽거나 API를 호출하지 않고 결과와 동작을 외부에서 받는다.
 * 재시도 버튼은 Activity의 입력 준비 흐름을 다시 시작하도록 요청한다.
 * 닫기 버튼도 전달받은 동작을 실행하며 화면 종료와 자원 정리는 Activity가 맡는다.
 * Fake 결과는 샘플 판정을 포함해 PB-05에 전달하고, 실제 결과는 판정 미확정 상태로 같은 화면에 전달한다.
 */
@Composable
fun AnalysisFlowScreen(
    state: AnalysisUiState,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    onHome: () -> Unit,
    isDemo: Boolean = BuildConfig.ANALYSIS_API_MODE.trim().equals("FAKE", ignoreCase = true),
) {
    // 팝업 표시 여부는 분석 상태와 별개로 저장해 회전 후에도 닫힌 상태를 유지한다.
    var loadingPopupDismissed by rememberSaveable { mutableStateOf(false) }
    val presented = (state as? AnalysisUiState.Success)?.result?.toPb05Result(isDemo)
    if (presented != null) {
        AnalysisResultRoute(presented, onReturn = onClose, onHome = onHome, isDemo = isDemo)
        return
    }
    Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
        if (isDemo) Text("Mock 검사 — 실제 이미지 분석 결과가 아닙니다.")
        when (state) {
            AnalysisUiState.Idle -> Text("검사 준비 중")
            AnalysisUiState.Loading -> Text("검사가 진행 중이에요. 완료되면 결과가 표시됩니다.")
            is AnalysisUiState.Success -> AnalysisResultContent(state.result)
            is AnalysisUiState.Error -> Unit
        }
        if (state !is AnalysisUiState.Error) {
            Button(onClick = onClose) {
                Text(if (state == AnalysisUiState.Loading) "검사 취소" else "닫기")
            }
        }
    }
    when (state) {
        AnalysisUiState.Loading -> if (!loadingPopupDismissed) {
            InspectionLoadingDialog(onDismissPopup = { loadingPopupDismissed = true })
        }
        is AnalysisUiState.Error -> {
            val canRetry = state.kind != AnalysisErrorKind.CONFIGURATION
            InspectionErrorDialog(
                message = buildString {
                    append(state.kind.message)
                    state.httpStatus?.let { append("\nHTTP 상태: $it") }
                    if (canRetry) append("\n다시 시도하면 같은 이미지로 분석을 새로 요청합니다.")
                },
                onRetry = {
                    loadingPopupDismissed = false
                    onRetry()
                },
                onCancelInspection = onClose,
                showRetry = canRetry,
            )
        }
        else -> Unit
    }
}

@Composable
fun AnalysisResultContent(result: AnalysisResult) {
    Text("최종 위험 판정 미확정")
    Text("AI 생성·변조 정보만 제공되어 사기 위험의 안전·주의·위험 판정은 표시하지 않습니다.")
    val score = result.aiGenerationScore?.takeIf { it.isFinite() && it in 0.0..100.0 }
    Text("AI 생성·변조 점수: ${score?.let { "$it%" } ?: "정보 없음"}")
    Text("서버 AI 위험 수준: ${result.aiRiskLevel?.takeIf { it.isNotBlank() } ?: "정보 없음"}")
    Text("판단 근거")
    result.reasons.forEach { Text(it) }
    if (result.reasons.isEmpty()) Text("근거 정보 없음")
    if (result.recommendedActions.isNotEmpty()) {
        Text("대응 행동")
        result.recommendedActions.forEach { Text(it) }
    }
}
