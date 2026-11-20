package com.dundueni.app.feature.analysis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dundueni.app.feature.history.HistoryType
import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.feature.analysisresult.AnalysisResultRoute
import com.dundueni.app.feature.analysisresult.AnalysisResult as PresentedResult
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.ui.component.InspectionErrorDialog
import com.dundueni.app.ui.component.InspectionLoadingDialog
import com.dundueni.app.ui.component.DundueniButton
import com.dundueni.app.ui.component.DundueniButtonType
import com.dundueni.app.ui.theme.BrandInk
import com.dundueni.app.ui.theme.CanvasSubLayer
import com.dundueni.app.ui.theme.Caution
import com.dundueni.app.ui.theme.DundueniDimens
import com.dundueni.app.ui.theme.DundueniPillShape
import com.dundueni.app.ui.theme.PageBackground
import com.dundueni.app.ui.theme.ProtectiveBlue
import com.dundueni.app.ui.theme.SecondaryInk
import com.dundueni.app.ui.theme.SoftBlue
import com.dundueni.app.ui.theme.SurfaceLight

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
    isDemo: Boolean = false,
    historyType: HistoryType = HistoryType.IMAGE,
    onRefresh: () -> Unit = {},
    initialSelectionElementId: String? = null,
    onChoosePhoto: (String, ScreenResultType?) -> Unit = { _, _ -> },
) {
    // The host window is translucent: render only the dialog over the screen that launched inspection.
    when (state) {
        AnalysisUiState.Idle -> return
        AnalysisUiState.Loading -> {
            var dismissed by rememberSaveable { mutableStateOf(false) }
            if (!dismissed) InspectionLoadingDialog(onDismissPopup = { dismissed = true })
            return
        }
        is AnalysisUiState.Pending -> {
            InspectionLoadingDialog(
                onDismissPopup = onClose,
                message = AnalysisErrorKind.IN_PROGRESS.message,
                onCheckResult = onRefresh,
            )
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
    val presented = state.toPresentedResult(isDemo, historyType)
    if (presented != null) {
        if (screen != null) {
            ScreenInspectionResult(screen, presented, onClose, onHome, onRefresh)
        } else {
            AnalysisResultRoute(presented, onReturn = onClose, onHome = onHome, isDemo = isDemo)
        }
        return
    }
    if (state is AnalysisUiState.Selection) {
        AnalysisSelectionScreen(
            state, onClose, onChoosePhoto, isDemo, historyType == HistoryType.SCREEN, initialSelectionElementId,
        )
        return
    }
    Column(Modifier.fillMaxSize().background(PageBackground).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("검사 진행", style = MaterialTheme.typography.titleLarge)
        if (isDemo) Text("Mock 검사 — 실제 이미지 분석 결과가 아닙니다.")
        when (state) {
            AnalysisUiState.Idle -> Text("검사 준비 중")
            AnalysisUiState.Loading -> Text("검사가 진행 중이에요. 완료되면 결과가 표시됩니다.")
            is AnalysisUiState.Selection -> Unit
            is AnalysisUiState.Success -> AnalysisResultContent(state.result)
            is AnalysisUiState.ScreenSuccess -> Unit
            is AnalysisUiState.Pending -> Unit
            is AnalysisUiState.Error -> Unit
        }
        if (state !is AnalysisUiState.Error) {
            DundueniButton(if (state == AnalysisUiState.Loading) "검사 취소" else "닫기", onClose,
                Modifier.fillMaxWidth(), DundueniButtonType.PRIMARY)
        }
    }
}

@Composable
internal fun ScreenInspectionResult(
    state: AnalysisUiState.ScreenSuccess,
    result: PresentedResult,
    onReturn: () -> Unit,
    onHome: () -> Unit,
    onRefresh: () -> Unit,
    refreshing: Boolean = false,
) {
    val resultType = state.resultType ?: when {
        state.image == null -> ScreenResultType.TEXT
        state.text == null -> ScreenResultType.IMAGE
        else -> null
    }
    val selectedError = when (resultType) {
        ScreenResultType.IMAGE -> state.imageError
        ScreenResultType.TEXT -> state.textError
        null -> null
    }
    if (selectedError == null) {
        Column {
            if (state.imageError?.pendingAnalysisId != null || state.textError?.pendingAnalysisId != null) {
                DundueniButton(
                    "결과 다시 확인", onRefresh, Modifier.fillMaxWidth(), DundueniButtonType.SECONDARY,
                    enabled = !refreshing,
                )
            }
            AnalysisResultRoute(result, onReturn = onReturn, onHome = onHome, screenResultType = resultType)
        }
    }

    if (refreshing) {
        var dismissed by rememberSaveable { mutableStateOf(false) }
        if (!dismissed) InspectionLoadingDialog(onDismissPopup = { dismissed = true })
        return
    }
    if (selectedError == null) return

    val canRecheck = selectedError.pendingAnalysisId != null
    // A failed detail lookup can keep the analysis ID even when the server verdict is still unknown.
    val awaitingResult = selectedError.kind == AnalysisErrorKind.IN_PROGRESS ||
        (canRecheck && selectedError.kind != AnalysisErrorKind.SERVER)
    if (awaitingResult) {
        InspectionLoadingDialog(
            onDismissPopup = onReturn,
            message = selectedError.message,
            onCheckResult = onRefresh,
        )
    } else {
        val analysis = if (resultType == ScreenResultType.IMAGE) "이미지 분석" else "문자 분석"
        InspectionErrorDialog(
            message = if (selectedError.kind == AnalysisErrorKind.SERVER) "서버에서 ${analysis}에 실패했어요."
                else "$analysis · ${selectedError.message}",
            onRetry = onRefresh,
            onCancelInspection = onReturn,
            showRetry = canRecheck,
        )
    }
}

@Composable
private fun AnalysisSelectionScreen(
    state: AnalysisUiState.Selection,
    onClose: () -> Unit,
    onChoosePhoto: (String, ScreenResultType?) -> Unit,
    isDemo: Boolean,
    fromScreen: Boolean,
    initialSelectionElementId: String?,
) {
    val upload = state.upload
    val image = checkNotNull(upload.image)
    val originalElementId = checkNotNull(upload.original?.elementId)
    val candidates = upload.elements.orEmpty()
    var selectingPart by rememberSaveable(upload.uploadId, initialSelectionElementId) {
        mutableStateOf(candidates.isNotEmpty() && initialSelectionElementId != originalElementId)
    }
    var selectedRegionId by rememberSaveable(upload.uploadId, initialSelectionElementId) {
        mutableStateOf(candidates.firstOrNull { it.elementId == initialSelectionElementId }?.elementId)
    }
    val selectedElementId = if (selectingPart) selectedRegionId else originalElementId
    val selectRegion: (String) -> Unit = { elementId ->
        selectedRegionId = if (selectingPart && selectedRegionId == elementId) null else elementId
        selectingPart = true
    }

    Scaffold(
        containerColor = PageBackground,
        topBar = {
            Surface(color = SurfaceLight) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기", tint = BrandInk)
                    }
                    Text(
                        if (fromScreen) "AI 화면 검사" else "이미지 검사",
                        style = MaterialTheme.typography.titleLarge,
                        color = BrandInk,
                    )
                }
            }
        },
        bottomBar = {
            Surface(color = SurfaceLight, border = BorderStroke(1.dp, CanvasSubLayer)) {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DundueniButton(
                        text = if (selectingPart) "선택한 부분 검사하기" else "전체 이미지 검사하기",
                        onClick = {
                            selectedElementId?.let { onChoosePhoto(it, ScreenResultType.IMAGE.takeIf { fromScreen }) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedElementId != null,
                    )
                    if (fromScreen && !selectingPart) {
                        TextButton(
                            onClick = { onChoosePhoto(originalElementId, ScreenResultType.TEXT) },
                            modifier = Modifier.heightIn(min = DundueniDimens.MinTouchSize),
                        ) {
                            Text("화면 내 텍스트 검사하기", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
    ) { contentPadding ->
        Column(
            Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(color = SoftBlue, shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("검사할 대상을 골라 주세요", style = MaterialTheme.typography.titleMedium, color = BrandInk)
                    Text(
                        "이미지 전체를 검사하거나, 표시된 사진 영역을 하나 선택할 수 있어요.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SecondaryInk,
                    )
                    if (isDemo) Text("Mock 검사 — 실제 이미지 분석 결과가 아닙니다.")
                }
            }

            Row(
                Modifier.fillMaxWidth().selectableGroup().background(CanvasSubLayer, DundueniPillShape).padding(4.dp),
            ) {
                SelectionMode(
                    title = "이미지 부분 검사",
                    selected = selectingPart,
                    enabled = candidates.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    onClick = { selectingPart = true },
                )
                SelectionMode(
                    title = "전체 이미지 검사",
                    selected = !selectingPart,
                    enabled = true,
                    modifier = Modifier.weight(1f),
                    onClick = { selectingPart = false },
                )
            }

            Surface(color = SurfaceLight, shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("이미지 미리보기", style = MaterialTheme.typography.titleMedium, color = BrandInk)
                    val previewShape = MaterialTheme.shapes.medium
                    Box(
                        Modifier.fillMaxWidth()
                            .aspectRatio(state.preview.width.toFloat() / state.preview.height)
                            .clip(previewShape)
                            .border(1.dp, if (selectingPart) CanvasSubLayer else ProtectiveBlue, previewShape),
                    ) {
                        Image(
                            state.preview.asImageBitmap(),
                            contentDescription = if (fromScreen) "캡처한 화면" else "업로드한 이미지",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds,
                        )
                        if (selectingPart) {
                            Canvas(Modifier.fillMaxSize().pointerInput(upload.uploadId) {
                                detectTapGestures { tap ->
                                    val x = tap.x * image.width / size.width
                                    val y = tap.y * image.height / size.height
                                    val candidate = candidates.lastOrNull { candidate ->
                                        val box = candidate.bbox ?: return@lastOrNull false
                                        x >= box.left && x <= box.left + box.width &&
                                            y >= box.top && y <= box.top + box.height
                                    }
                                    candidate?.elementId?.let(selectRegion)
                                }
                            }) {
                                candidates.forEach { candidate ->
                                    val box = candidate.bbox ?: return@forEach
                                    val selected = candidate.elementId == selectedRegionId
                                    val offset = Offset(box.left * size.width / image.width, box.top * size.height / image.height)
                                    val bounds = Size(box.width * size.width / image.width, box.height * size.height / image.height)
                                    if (selected) drawRect(ProtectiveBlue.copy(alpha = 0.16f), offset, bounds)
                                    drawRect(
                                        color = if (selected) ProtectiveBlue else Caution,
                                        topLeft = offset,
                                        size = bounds,
                                        style = Stroke(width = 3.dp.toPx()),
                                    )
                                }
                            }
                        }
                    }
                    if (selectingPart) {
                        Text(
                            "이미지의 테두리 안이나 아래 목록에서 사진 영역을 눌러 주세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryInk,
                        )
                    }
                }
            }

            Surface(color = SurfaceLight, shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("검사 영역", style = MaterialTheme.typography.titleMedium, color = BrandInk)
                    if (candidates.isEmpty()) {
                        Text(
                            "선택할 사진 영역을 찾지 못했어요. 전체 이미지는 검사할 수 있어요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryInk,
                        )
                    } else {
                        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            candidates.forEachIndexed { index, candidate ->
                                val elementId = checkNotNull(candidate.elementId)
                                val selected = selectingPart && selectedRegionId == elementId
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (selected) SoftBlue else PageBackground,
                                    border = BorderStroke(1.dp, if (selected) ProtectiveBlue else CanvasSubLayer),
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .selectable(selected, role = Role.RadioButton, onClick = { selectRegion(elementId) })
                                            .heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RadioButton(selected = selected, onClick = null)
                                        Text("사진 영역 ${index + 1}", style = MaterialTheme.typography.bodyLarge, color = BrandInk)
                                    }
                                }
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth().semantics { disabled() },
                        color = PageBackground,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, CanvasSubLayer),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("텍스트 부분 검사", style = MaterialTheme.typography.bodyLarge, color = SecondaryInk)
                            Text("이 화면에서는 문자 영역을 선택할 수 없어요.", style = MaterialTheme.typography.bodyMedium, color = SecondaryInk)
                        }
                    }
                }
            }

            Text(
                "업로드는 10분 후 만료됩니다. 만료되면 이미지를 다시 업로드해 주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryInk,
            )
        }
    }
}

@Composable
private fun SelectionMode(
    title: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier.clip(DundueniPillShape)
            .background(if (selected) SurfaceLight else Color.Transparent)
            .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = DundueniDimens.MinTouchSize)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = when {
                selected -> ProtectiveBlue
                enabled -> BrandInk
                else -> SecondaryInk
            },
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AnalysisResultContent(result: AnalysisResult) {
    if (result.type == "IMAGE") {
        result.imageOverallDecisionLabel?.takeIf { it.isNotBlank() }?.let { Text(it) }
        result.imageGenerationDecision?.let { Text("AI 생성 판정: $it") }
        result.imageGenerationDisplayScore?.let { Text("AI 생성 표시 점수: $it/100") }
        result.imageManipulationDecision?.let { Text("이미지 변조 판정: $it") }
        result.imageManipulationDisplayScore?.let { Text("이미지 변조 표시 점수: $it/100") }
    } else if (result.type == "TEXT") {
        result.textDecision?.let { Text("텍스트 판정: $it") }
        result.textEvidence.forEach { Text(it) }
        result.textLimitations.forEach { Text("분석 주의사항: $it") }
    }
    result.reasons.forEach { Text(it) }
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
            summary = imagePresented?.summary.orEmpty(),
            evidence = imagePresented?.evidence.orEmpty(),
            screenTextEvidence = textPresented?.evidence.orEmpty(),
            guidance = imagePresented?.guidance.orEmpty(),
            keyFinding = textPresented?.keyFinding,
            textDecision = textPresented?.textDecision,
            textRiskScore = textPresented?.textRiskScore,
            textSourceType = textPresented?.textSourceType,
            textNeedsReview = textPresented?.textNeedsReview,
            textReasonCodes = textPresented?.textReasonCodes,
            textOcrUsed = textPresented?.textOcrUsed,
            textLimitations = textPresented?.textLimitations.orEmpty(),
            targetLabel = "캡처 화면",
        )
    }
    val presented = (state as? AnalysisUiState.Success)?.result?.toPb05Result(isDemo)?.let {
        if (it.historyType == HistoryType.TEXT) it else it.copy(historyType = historyType)
    }
        ?: screenPresented
    return presented
}
