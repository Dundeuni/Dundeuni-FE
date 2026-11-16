package com.dundueni.app.feature.analysis

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dundueni.app.data.model.AnalysisResult as BackendResult
import com.dundueni.app.data.repository.AnalysisExecutionStatus
import com.dundueni.app.feature.history.HistoryType
import com.dundueni.app.ui.theme.DundueniFETheme

// These are completed detail responses supplied directly to the production presentation path.
// No ViewModel, repository, or network client is created by these previews.
internal fun previewImageDetail() = BackendResult(
    analysisId = "preview-image", type = "IMAGE", status = "COMPLETED",
    aiGenerationScore = null, aiRiskLevel = null,
    reasons = listOf("이미지의 일부 영역에서 합성 흔적을 확인했어요."),
    modelVersion = null, errorCode = null, createdAt = null,
    completedAt = null, expiresAt = null,
    summary = "이미지 분석 결과를 확인해 주세요.",
    imageOverallDecisionLabel = "이미지 변조 의심",
    imageGenerationDecision = "생성 징후 낮음",
    imageGenerationDisplayScore = 12.0,
    imageManipulationDecision = "변조 의심",
    imageManipulationDisplayScore = 82.0,
)

internal fun previewTextDetail(sourceType: String = "SCREEN_CROP") = BackendResult(
    analysisId = "preview-text", type = "TEXT", status = "COMPLETED",
    aiGenerationScore = null, aiRiskLevel = null, reasons = emptyList(),
    modelVersion = null, errorCode = null, createdAt = null,
    completedAt = null, expiresAt = null,
    summary = "송금을 재촉하는 문구가 포함되어 있어요.",
    normalizedRiskLevel = "DANGER", textDecision = "phishing", riskScore = 0.87,
    sourceType = sourceType, textNeedsReview = true,
    textEvidence = listOf("급한 송금을 요구하는 문구가 있어요."),
    textLimitations = listOf("화면에 보이지 않는 문맥은 확인할 수 없어요."),
)

private fun screenState(
    image: BackendResult? = previewImageDetail(),
    text: ScreenCropAnalysis? = previewScreenText(),
    imageError: AnalysisUiState.Error? = null,
    textError: AnalysisUiState.Error? = null,
) = AnalysisUiState.ScreenSuccess(image, text, imageError, textError)

private fun previewScreenText() = ScreenCropAnalysis(
    analysisId = "preview-text", decision = "phishing", detail = previewTextDetail(),
)

@Composable
private fun FlowPreview(state: AnalysisUiState, historyType: HistoryType = HistoryType.IMAGE) {
    DundueniFETheme {
        AnalysisFlowScreen(
            state = state, onRetry = {}, onClose = {}, onHome = {},
            onRefresh = {}, isDemo = false, historyType = historyType,
        )
    }
}

@Preview(name = "P1 검사 · 로딩", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun InspectionLoadingPreview() = FlowPreview(AnalysisUiState.Loading)

@Preview(name = "P1 검사 · 서버 분석 중", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun InspectionPendingPreview() = FlowPreview(
    AnalysisUiState.Pending("preview-analysis", AnalysisExecutionStatus.ANALYZING),
)

@Preview(name = "P1 검사 · 일반 실패", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun InspectionErrorPreview() = FlowPreview(AnalysisUiState.Error())

@Preview(name = "P1 검사 · 연결 오류", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun InspectionConnectionErrorPreview() = FlowPreview(
    AnalysisUiState.Error(AnalysisErrorKind.CONNECTION),
)

@Preview(name = "P1 검사 · HTTP 오류", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun InspectionHttpErrorPreview() = FlowPreview(
    AnalysisUiState.Error(AnalysisErrorKind.HTTP, httpStatus = 503),
)

@Preview(name = "P1 검사 · 시간 초과", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun InspectionTimeoutPreview() = FlowPreview(
    AnalysisUiState.Error(AnalysisErrorKind.TIMEOUT),
)

@Preview(name = "P1 검사 · 결과 재조회", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun InspectionRefreshErrorPreview() = FlowPreview(
    AnalysisUiState.Error(AnalysisErrorKind.CONNECTION, pendingAnalysisId = "preview-analysis"),
)

@Preview(name = "P1 화면 검사 · 이미지와 문자 완료", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ScreenBothSucceededPreview() = FlowPreview(screenState(), HistoryType.SCREEN)

@Preview(name = "P1 화면 검사 · 문자 분석 실패", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ScreenTextFailedPreview() = FlowPreview(
    screenState(text = null, textError = AnalysisUiState.Error(AnalysisErrorKind.SERVER)),
    HistoryType.SCREEN,
)

@Preview(name = "P1 화면 검사 · 이미지 분석 실패", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ScreenImageFailedPreview() = FlowPreview(
    screenState(image = null, imageError = AnalysisUiState.Error(AnalysisErrorKind.CONNECTION)),
    HistoryType.SCREEN,
)

@Preview(name = "P1 화면 검사 · 부분 결과 재조회", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ScreenPartialRefreshPreview() = FlowPreview(
    screenState(text = null, textError = AnalysisUiState.Error(
        AnalysisErrorKind.IN_PROGRESS, pendingAnalysisId = "preview-text",
    )),
    HistoryType.SCREEN,
)
