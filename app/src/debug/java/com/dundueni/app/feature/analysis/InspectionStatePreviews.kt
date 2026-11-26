package com.dundueni.app.feature.analysis

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dundueni.app.data.mapper.toDomain
import com.dundueni.app.data.model.AnalysisResult as BackendResult
import com.dundueni.app.data.remote.dto.AnalysisActionGuideDto
import com.dundueni.app.data.remote.dto.AnalysisDetailDto
import com.dundueni.app.data.remote.dto.AnalysisDetailedReasonDto
import com.dundueni.app.data.remote.dto.AnalysisImageModelsDto
import com.dundueni.app.data.remote.dto.AnalysisModelResultDto
import com.dundueni.app.data.remote.dto.AnalysisOverallResultDto
import com.dundueni.app.data.remote.dto.AnalysisReasonDto
import com.dundueni.app.data.remote.dto.TextAnalysisQualityDto
import com.dundueni.app.data.remote.dto.TextAnalysisTextResultDto
import com.dundueni.app.data.repository.AnalysisExecutionStatus
import com.dundueni.app.data.repository.ImageSummaryResult
import com.dundueni.app.feature.analysisresult.ImageSummaryUiState
import com.dundueni.app.feature.history.HistoryType
import com.dundueni.app.feature.phone.PhoneInspectionPreview
import com.dundueni.app.feature.textinput.TextInputScreen
import com.dundueni.app.ui.component.InspectionErrorDialog
import com.dundueni.app.ui.component.InspectionLoadingDialog
import com.dundueni.app.ui.theme.DundueniFETheme

// These are completed detail responses supplied directly to the production presentation path.
// No ViewModel, repository, or network client is created by these previews.
internal fun previewImageDetail(
    partialFailure: Boolean = false,
    summaryStatus: String? = "COMPLETED",
    noClearSignal: Boolean = false,
    missingModel: Boolean = false,
): BackendResult {
    val inconclusive = partialFailure || missingModel
    val suspicious = !inconclusive && !noClearSignal
    val summaryReady = summaryStatus == "COMPLETED"
    val overallResult = when {
        inconclusive -> AnalysisOverallResultDto(decision = "INCONCLUSIVE", decisionLabel = "주의")
        noClearSignal -> AnalysisOverallResultDto(decision = "NO_CLEAR_SIGNAL", decisionLabel = "안전")
        else -> AnalysisOverallResultDto(
            decision = "GENERATION_OR_MANIPULATION_SUSPECTED", decisionLabel = "위험",
        )
    }
    return AnalysisDetailDto(
        analysisId = "preview-image", type = "IMAGE", status = "COMPLETED",
        overallResult = overallResult,
        models = AnalysisImageModelsDto(
            status = if (partialFailure) "PARTIAL_FAILED" else "COMPLETED",
            generation = AnalysisModelResultDto(
                status = "SUCCEEDED",
                decision = when {
                    missingModel -> "INCONCLUSIVE"
                    suspicious -> "AI_GENERATED_SUSPECTED"
                    else -> "NO_CLEAR_GENERATION_SIGNAL"
                },
                displayScore = if (missingModel) null else if (suspicious) 84.0 else 12.0,
                reasons = listOf(AnalysisReasonDto("모델 내부 분석 근거")),
                limitations = listOf("압축된 이미지에서는 세부 흔적이 흐려질 수 있습니다."),
            ),
            manipulation = when {
                missingModel -> null
                partialFailure -> AnalysisModelResultDto(status = "FAILED", retryable = true)
                else -> AnalysisModelResultDto(
                    status = "SUCCEEDED",
                    decision = if (suspicious) "MANIPULATION_SUSPECTED" else "NO_CLEAR_MANIPULATION_SIGNAL",
                    displayScore = if (suspicious) 82.0 else 9.0,
                )
            },
        ),
        summaryStatus = summaryStatus,
        summary = if (!summaryReady) null else when {
            inconclusive -> "일부 모델의 결과가 충분하지 않아 이미지 변조 여부를 판단하기 어렵습니다."
            noClearSignal -> "이번 검사에서 뚜렷한 생성 또는 변조 신호가 확인되지 않았습니다."
            else -> "이미지에서 AI 생성과 변조 의심 신호가 확인됐습니다."
        },
        detailedReasons = if (summaryReady && suspicious) listOf(
            AnalysisDetailedReasonDto("GENERATION", "AI 생성 의심 신호", "생성 분석에서 의심 신호가 확인되었습니다."),
            AnalysisDetailedReasonDto("MANIPULATION", "이미지 변조 의심 신호", "이미지 일부에서 편집 흔적이 확인되었습니다."),
        ) else emptyList(),
        actionGuides = if (summaryReady) listOf(
            AnalysisActionGuideDto("공식 연락처로 확인하기", "요청을 보낸 기관의 공식 홈페이지에서 연락처를 찾아 확인하세요."),
            AnalysisActionGuideDto("이미지 출처 확인하기", "공식 게시물이나 원본 이미지와 비교해 보세요."),
            AnalysisActionGuideDto("정보 제공 전에 확인하기", "요청의 출처와 용도가 확인될 때까지 개인정보를 보내지 마세요."),
        ) else emptyList(),
        reassurance = "판단이 어렵다면 신뢰하는 사람과 함께 확인해 보세요.".takeIf { summaryReady },
        summaryModel = "preview-model".takeIf { summaryReady },
        summarizedAt = "2026-11-25T06:30:00.000Z".takeIf { summaryReady },
    ).toDomain()
}

internal fun previewLegacyImageDetail() = AnalysisDetailDto(
    analysisId = "preview-legacy-image", type = "IMAGE", status = "COMPLETED",
    aiGenerationScore = 82.4,
    reasons = listOf(AnalysisReasonDto("이미지 일부에서 생성 흔적이 감지됐습니다.")),
    overallResult = AnalysisOverallResultDto(decision = "INCONCLUSIVE", decisionLabel = "주의"),
    summaryStatus = "COMPLETED",
    summary = "현재 분석만으로는 판단하기 어렵습니다. 원본 사진이나 추가 자료를 확인해 주세요.",
).toDomain()

internal fun previewTextDetail(
    sourceType: String = "SCREEN_CROP",
    decision: String = "phishing",
    evidence: List<String> = when (decision) {
        "normal" -> listOf("일상적인 약속을 확인하는 문장입니다.")
        "defer" -> emptyList()
        else -> listOf("급한 송금을 요구하는 문구가 있어요.")
    },
) =
    AnalysisDetailDto(
        analysisId = "preview-text", type = "TEXT", status = "COMPLETED",
        textResult = TextAnalysisTextResultDto(
            decision = decision,
            riskScore = when (decision) {
                "normal" -> 0.08
                "defer" -> null
                else -> 0.87
            },
            sourceType = sourceType,
            quality = TextAnalysisQualityDto(needsReview = decision != "normal", ocrUsed = sourceType == "SCREEN_CROP"),
            evidence = evidence,
            limitations = listOf(if (decision == "defer") "분류에 필요한 문맥이 부족합니다." else "발신자 신원은 확인할 수 없습니다."),
        ),
        guides = listOf("현재 분석만으로는 판단하기 어렵습니다.", "원본 사진이나 추가 자료를 확인해 주세요."),
    ).toDomain()

internal fun screenState(
    image: BackendResult? = previewImageDetail(),
    text: ScreenCropAnalysis? = previewScreenText(),
    imageError: AnalysisUiState.Error? = null,
    textError: AnalysisUiState.Error? = null,
    resultType: ScreenResultType? = null,
) = AnalysisUiState.ScreenSuccess(image, text, imageError, textError, resultType)

private fun previewScreenText() = previewTextDetail().toScreenCropAnalysis()

@Composable
internal fun FlowPreview(state: AnalysisUiState, historyType: HistoryType = HistoryType.IMAGE) {
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

@Preview(name = "P1 검사 · 이미지 전체 분석 실패", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun ImageAnalysisFailedPreview() = FlowPreview(AnalysisUiState.Error(AnalysisErrorKind.SERVER))

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

@Preview(name = "P1 화면 검사 · 문자 분석 실패", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun ScreenTextFailedPreview() = FlowPreview(
    screenState(text = null, textError = AnalysisUiState.Error(AnalysisErrorKind.SERVER), resultType = ScreenResultType.TEXT),
    HistoryType.SCREEN,
)

@Preview(name = "P1 화면 검사 · 이미지 분석 실패", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun ScreenImageFailedPreview() = FlowPreview(
    screenState(image = null, imageError = AnalysisUiState.Error(AnalysisErrorKind.CONNECTION), resultType = ScreenResultType.IMAGE),
    HistoryType.SCREEN,
)

@Preview(name = "P1 화면 검사 · 부분 결과 재조회", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun ScreenPartialRefreshPreview() = FlowPreview(
    screenState(
        text = null, textError = AnalysisUiState.Error(AnalysisErrorKind.IN_PROGRESS, pendingAnalysisId = "preview-text"),
        resultType = ScreenResultType.TEXT,
    ),
    HistoryType.SCREEN,
)

@Preview(name = "P1 결과 · 이미지 주의 · 모델 부분 실패", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ImagePartialFailureResultPreview() = ResultPreview(
    checkNotNull(previewImageDetail(partialFailure = true).toPb05Result(isDemo = false)),
)

@Preview(name = "P1 결과 · 이미지 요약 데이터 누락 및 모델 미분석", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ImageSummaryMissingPreview() = ResultPreview(
    checkNotNull(previewImageDetail(summaryStatus = null, missingModel = true).toPb05Result(isDemo = false)),
)

@Preview(name = "P1 결과 · 이미지 요약 조회 오류 및 다시 확인", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ImageSummaryManualRetryPreview() = ImageSummaryStatusPreview(
    "PENDING", ImageSummaryUiState(ImageSummaryResult.Pending, checking = false, showRetry = true, lookupFailed = true),
)

@Preview(name = "P1 결과 · 이미지 요약 생성 실패", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ImageSummaryFailedPreview() = ImageSummaryStatusPreview("FAILED")

@Preview(name = "P1 결과 · 이미지 요약 생성 중 · PNG 저장 대기", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ImageSummaryPendingResultPreview() = ImageSummaryStatusPreview("PENDING")

@Composable
private fun ImageSummaryStatusPreview(summaryStatus: String, state: ImageSummaryUiState? = null) = ResultPreview(
    checkNotNull(previewImageDetail(summaryStatus = summaryStatus).toPb05Result(isDemo = false)),
    imageSummary = state,
)

@Preview(name = "P1 화면 검사 · 백그라운드 결과 재조회", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun BackgroundScreenPartialRefreshPreview() {
    val state = screenState(
        text = null, textError = AnalysisUiState.Error(AnalysisErrorKind.IN_PROGRESS, pendingAnalysisId = "preview-text"),
        resultType = ScreenResultType.TEXT,
    )
    val result = checkNotNull(state.toPresentedResult(false, HistoryType.SCREEN))
    DundueniFETheme {
        ScreenInspectionResult(
            state, result, onReturn = {}, onHome = {}, onRefresh = {}, refreshing = true,
        )
    }
}

@Preview(name = "P1 화면 검사 · AI 요약 다시 확인", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ScreenSummaryManualRetryPreview() {
    val state = screenState(image = previewImageDetail(summaryStatus = "PENDING"), resultType = ScreenResultType.IMAGE)
    ResultPreview(
        checkNotNull(state.toPresentedResult(false, HistoryType.SCREEN)),
        imageSummary = ImageSummaryUiState(
            ImageSummaryResult.Pending, checking = false, showRetry = true, lookupFailed = true,
        ),
        screenResultType = state.resultType,
    )
}

// POST inspection and the subsequent detail GET both use PhoneInspectionUiState.Loading.
@Preview(name = "P2 전화번호 검사 · 요청 및 상세 조회 중", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun PhoneInspectionLoadingPreview() = PhoneInspectionPreview(loading = true)

@Preview(name = "P2 전화번호 검사 · 일반 실패", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun PhoneInspectionErrorPreview() = PhoneInspectionPreview(
    errorMessage = "검사를 완료하지 못했어요. 다시 시도해 주세요.",
)

@Preview(name = "P2 전화번호 검사 · 네트워크 오류", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun PhoneInspectionNetworkErrorPreview() = PhoneInspectionPreview(
    errorMessage = "네트워크 연결을 확인한 뒤 다시 시도해 주세요.",
)

@Preview(name = "P2 전화번호 검사 · HTTP 오류", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun PhoneInspectionHttpErrorPreview() = PhoneInspectionPreview(
    errorMessage = "검사를 완료하지 못했어요 (HTTP 429).",
)

@Preview(name = "P2 전화번호 검사 · 상세 조회 실패", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun PhoneInspectionDetailErrorPreview() = PhoneInspectionPreview(
    errorMessage = "검사 결과를 조회하지 못했어요. 다시 시도해 주세요.",
)

@Composable
private fun TextInspectionPreview(loading: Boolean = false, errorMessage: String? = null) {
    DundueniFETheme {
        TextInputScreen(
            textInput = "급하게 송금해 주세요.",
            onTextChange = {}, onInspect = {}, onClose = {},
            isInspecting = loading,
        )
        if (loading) InspectionLoadingDialog(onDismissPopup = {})
        errorMessage?.let { message ->
            InspectionErrorDialog(
                message = message, onRetry = {}, onCancelInspection = {},
            )
        }
    }
}

@Preview(name = "P2 텍스트 검사 · 요청 중", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun TextInspectionLoadingPreview() = TextInspectionPreview(loading = true)

@Preview(name = "P2 텍스트 검사 · 실패 후 재시도", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun TextInspectionErrorPreview() = TextInspectionPreview(
    errorMessage = "텍스트 검사를 완료하지 못했어요. 다시 시도하면 같은 텍스트로 검사합니다.",
)

@Preview(name = "P2 텍스트 검사 · 결과 재조회", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun TextInspectionLookupErrorPreview() = TextInspectionPreview(
    errorMessage = "결과를 조회하지 못했어요. 다시 시도하면 같은 분석 ID로 확인합니다.",
)

@Preview(name = "공통 검사 실패 팝업", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun InspectionErrorDialogPreview() {
    DundueniFETheme {
        InspectionErrorDialog(
            message = "네트워크 연결을 확인한 뒤 다시 시도해 주세요.",
            onRetry = {},
            onCancelInspection = {},
        )
    }
}
