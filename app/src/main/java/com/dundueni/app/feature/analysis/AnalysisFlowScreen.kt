package com.dundueni.app.feature.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dundueni.app.BuildConfig
import com.dundueni.app.feature.history.HistoryType
import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.feature.analysisresult.AnalysisResultRoute
import com.dundueni.app.feature.analysisresult.AnalysisResult as PresentedResult
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.ui.component.InspectionErrorDialog
import com.dundueni.app.ui.component.InspectionLoadingDialog
import com.dundueni.app.ui.component.DundueniButton
import com.dundueni.app.ui.component.DundueniButtonType
import com.dundueni.app.ui.theme.PageBackground

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
    historyType: HistoryType = HistoryType.IMAGE,
    onRefresh: () -> Unit = {},
    onChoosePhoto: (String) -> Unit = {},
) {
    // 팝업 표시 여부는 분석 상태와 별개로 저장해 회전 후에도 닫힌 상태를 유지한다.
    var loadingPopupDismissed by rememberSaveable { mutableStateOf(false) }
    // The host window is translucent: render only the dialog over the screen that launched inspection.
    when (state) {
        AnalysisUiState.Idle -> return
        AnalysisUiState.Loading -> {
            if (!loadingPopupDismissed) InspectionLoadingDialog(onDismissPopup = { loadingPopupDismissed = true })
            return
        }
        is AnalysisUiState.Error -> {
            val canRetry = state.kind != AnalysisErrorKind.CONFIGURATION
            InspectionErrorDialog(
                message = buildString {
                    append(state.message)
                    state.httpStatus?.let { append("\nHTTP 상태: $it") }
                    if (canRetry) append(if (state.pendingAnalysisId != null)
                        "\n다시 시도하면 같은 분석 ID로 결과를 조회합니다."
                    else if (isDemo) "\n다시 시도하면 같은 이미지로 분석을 새로 요청합니다."
                    else "\n다시 시도하면 이미지를 다시 업로드합니다.")
                },
                onRetry = {
                    loadingPopupDismissed = false
                    if (state.pendingAnalysisId != null) onRefresh() else onRetry()
                },
                onCancelInspection = onClose,
                showRetry = canRetry,
            )
            return
        }
        else -> Unit
    }
    val screen = state as? AnalysisUiState.ScreenSuccess
    val textPresented = screen?.text?.detail?.toTextResult()
    val presented = state.toPresentedResult(isDemo, historyType)
    if (presented != null) {
        Column {
            if (screen?.imageError?.pendingAnalysisId != null || screen?.textError?.pendingAnalysisId != null) {
                DundueniButton("결과 다시 확인", onRefresh, Modifier.fillMaxWidth(), DundueniButtonType.SECONDARY)
            }
            AnalysisResultRoute(
                presented, onReturn = onClose, onHome = onHome, isDemo = isDemo,
                screenCropDecision = screen?.text?.let { "${it.label}\n위험 수준: ${textPresented?.riskLevel?.label}" },
                imageAnalysisError = screen?.imageError?.message,
                screenCropError = screen?.textError?.message,
            )
        }
        return
    }
    Column(Modifier.fillMaxSize().background(PageBackground).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("검사 진행", style = MaterialTheme.typography.titleLarge)
        if (isDemo) Text("Mock 검사 — 실제 이미지 분석 결과가 아닙니다.")
        when (state) {
            AnalysisUiState.Idle -> Text("검사 준비 중")
            AnalysisUiState.Loading -> Text("검사가 진행 중이에요. 완료되면 결과가 표시됩니다.")
            is AnalysisUiState.Selection -> {
                val upload = state.upload
                val image = checkNotNull(upload.image)
                val original = checkNotNull(upload.original)
                var selected by rememberSaveable(upload.uploadId) { mutableStateOf<String?>(null) }
                Text(if (upload.elements.isNullOrEmpty())
                    "사진 영역을 찾지 못했어요. 전체 이미지를 분석할까요?"
                else "전체 이미지 또는 표시된 사진 영역을 선택해 주세요.")
                Text("업로드는 10분 후 만료됩니다. 만료되면 이미지를 다시 업로드해 주세요.")
                Box(Modifier.fillMaxWidth().aspectRatio(state.preview.width.toFloat() / state.preview.height)) {
                    Image(state.preview.asImageBitmap(), contentDescription = "업로드한 이미지",
                        modifier = Modifier.fillMaxSize())
                    Canvas(Modifier.fillMaxSize().pointerInput(upload.uploadId, selected) {
                        detectTapGestures { tap ->
                            val x = tap.x * image.width / size.width
                            val y = tap.y * image.height / size.height
                            selected = upload.elements.orEmpty().lastOrNull { candidate ->
                                val box = candidate.bbox ?: return@lastOrNull false
                                x >= box.left && x <= box.left + box.width &&
                                    y >= box.top && y <= box.top + box.height
                            }?.elementId ?: original.elementId
                        }
                    }) {
                        upload.elements.orEmpty().forEach { candidate ->
                            val box = candidate.bbox ?: return@forEach
                            val color = if (selected == candidate.elementId) Color(0xFF1463D6) else Color(0xFFFFC107)
                            drawRect(color, Offset(box.left * size.width / image.width,
                                box.top * size.height / image.height),
                                Size(box.width * size.width / image.width,
                                    box.height * size.height / image.height),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))
                        }
                    }
                }
                DundueniButton("전체 이미지 선택", { selected = original.elementId }, Modifier.fillMaxWidth(),
                    DundueniButtonType.SECONDARY)
                upload.elements.orEmpty().forEachIndexed { index, candidate ->
                    DundueniButton("사진 후보 ${index + 1}${if (selected == candidate.elementId) " ✓" else ""}",
                        { selected = candidate.elementId }, Modifier.fillMaxWidth(), DundueniButtonType.SECONDARY)
                }
                if (selected == original.elementId) Text("선택: 전체 이미지")
                if (selected != null) DundueniButton("선택한 대상 분석", { onChoosePhoto(checkNotNull(selected)) },
                    Modifier.fillMaxWidth(), DundueniButtonType.PRIMARY)
            }
            is AnalysisUiState.Success -> AnalysisResultContent(state.result)
            is AnalysisUiState.ScreenSuccess -> Unit
            is AnalysisUiState.Pending -> Text("분석 요청이 접수되었으며 서버에서 처리 중입니다.")
            is AnalysisUiState.Error -> Unit
        }
        if (state is AnalysisUiState.Pending) {
            DundueniButton("결과 다시 확인", onRefresh, Modifier.fillMaxWidth(), DundueniButtonType.SECONDARY)
        }
        if (state !is AnalysisUiState.Error) {
            DundueniButton(if (state == AnalysisUiState.Loading) "검사 취소" else "닫기", onClose,
                Modifier.fillMaxWidth(), DundueniButtonType.PRIMARY)
        }
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

/** Reuses the inline PB-05 mapping for the overlay destination. */
internal fun AnalysisUiState.toPresentedResult(isDemo: Boolean, historyType: HistoryType): PresentedResult? {
    val state = this
    val screen = state as? AnalysisUiState.ScreenSuccess
    val imagePresented = screen?.image?.toPb05Result(isDemo = false)
    val textPresented = screen?.text?.detail?.toTextResult()
    val screenPresented = (imagePresented ?: textPresented)?.let { primary ->
        primary.copy(
            historyType = HistoryType.SCREEN,
            summary = listOfNotNull(imagePresented?.summary, textPresented?.summary).joinToString("\n"),
            evidence = imagePresented?.evidence.orEmpty() + textPresented?.evidence.orEmpty(),
            guidance = imagePresented?.guidance.orEmpty() + textPresented?.guidance.orEmpty(),
            textDecision = textPresented?.textDecision,
            textRiskScore = textPresented?.textRiskScore,
            textSourceType = textPresented?.textSourceType,
            textNeedsReview = textPresented?.textNeedsReview,
            textReasonCodes = textPresented?.textReasonCodes,
            textOcrUsed = textPresented?.textOcrUsed,
            targetLabel = "캡처 화면",
        )
    }
    val presented = (state as? AnalysisUiState.Success)?.result?.toPb05Result(isDemo)?.let {
        if (it.historyType == HistoryType.TEXT) it else it.copy(historyType = historyType)
    }
        ?: screenPresented
    return presented
}
