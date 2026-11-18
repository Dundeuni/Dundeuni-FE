package com.dundueni.app.feature.analysis

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.dundueni.app.feature.analysisresult.AnalysisResult as PresentedResult
import com.dundueni.app.feature.analysisresult.AnalysisResultScreen
import com.dundueni.app.feature.analysisresult.ImageSummaryUiState
import com.dundueni.app.data.repository.ImageSummaryResult
import com.dundueni.app.data.remote.api.HistoryEntryDto
import com.dundueni.app.data.remote.api.HistoryPhoneResultDto
import com.dundueni.app.feature.history.HistoryType
import com.dundueni.app.feature.phone.toPhoneResult
import com.dundueni.app.ui.theme.DundueniFETheme
import com.dundueni.app.ui.theme.LocalEasyMode

@Composable
internal fun ResultPreview(result: PresentedResult, easyMode: Boolean = false, imageSummary: ImageSummaryUiState? = null) {
    DundueniFETheme {
        CompositionLocalProvider(LocalEasyMode provides easyMode) {
            AnalysisResultScreen(result = result, onAction = {}, isDemo = false, imageSummary = imageSummary)
        }
    }
}

@Preview(name = "P1 결과 · 이미지 검사", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ImageResultPreview() {
    val detail = previewImageDetail()
    ResultPreview(
        checkNotNull(detail.copy(summary = null).toPb05Result(isDemo = false)),
        imageSummary = ImageSummaryUiState(ImageSummaryResult.Completed(checkNotNull(detail.summary)), checking = false),
    )
}

@Preview(name = "P1 화면 검사 · 이미지 요약 완료", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ScreenImageSummaryCompletedPreview() {
    val state = screenState(image = previewImageDetail(summaryReady = false))
    ResultPreview(
        checkNotNull(state.toPresentedResult(false, HistoryType.SCREEN)),
        imageSummary = ImageSummaryUiState(
            ImageSummaryResult.Completed(checkNotNull(previewImageDetail().summary)), checking = false,
        ),
    )
}

@Preview(name = "P1 결과 · 텍스트 검사", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun TextResultPreview() = ResultPreview(
    previewTextDetail(sourceType = "TEXT").toTextResult("급하게 송금해 주세요."),
)

@Preview(name = "P1 결과 · 전화번호 검사", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun PhoneResultPreview() = ResultPreview(previewPhoneResult())

private fun previewPhoneResult() =
    HistoryEntryDto(
        id = "preview-phone", type = "PHONE", status = "COMPLETED",
        phoneResult = HistoryPhoneResultDto(
            phoneNumber = "010********", riskLevel = "CAUTION", spamType = "광고", reportCount = 3,
            reasons = listOf("스팸 유형 또는 신고 이력이 있어 주의가 필요합니다."),
        ),
    ).toPhoneResult()

@Preview(name = "P1 결과 · 텍스트 판단 보류", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun TextDeferredResultPreview() = ResultPreview(
    previewTextDetail(sourceType = "TEXT", deferred = true).toTextResult(),
)

@Preview(name = "P1 화면 검사 · 이미지와 문자 완료", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ScreenBothSucceededPreview() = FlowPreview(screenState(), HistoryType.SCREEN)

@Preview(name = "P1 결과 · 구형 이미지 응답", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun LegacyImageScoreResultPreview() = ResultPreview(
    checkNotNull(previewLegacyImageDetail().toPb05Result(isDemo = false)),
)

@Preview(name = "P1 결과 · 큰 텍스트 결과", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun LargeTextResultPreview() {
    val evidence = List(20) { index ->
        "근거 ${index + 1}: " +
            "요청의 출처와 송금 문구를 함께 확인했습니다. 발신자를 확인해 주세요. ".repeat(90).take(3500)
    }
    ResultPreview(previewTextDetail(sourceType = "TEXT", evidence = evidence).toTextResult("급하게 송금해 주세요."))
}

@Preview(name = "P1 결과 · 전화번호 쉬운 모드", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun EasyPhoneResultPreview() = ResultPreview(previewPhoneResult(), easyMode = true)
