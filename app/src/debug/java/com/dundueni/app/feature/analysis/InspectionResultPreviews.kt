package com.dundueni.app.feature.analysis

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.dundueni.app.feature.analysisresult.AnalysisResult as PresentedResult
import com.dundueni.app.feature.analysisresult.AnalysisResultScreen
import com.dundueni.app.feature.analysisresult.ImageSummaryUiState
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.data.model.ExtractedPhoneNumber
import com.dundueni.app.data.repository.ImageSummaryResult
import com.dundueni.app.data.remote.api.HistoryEntryDto
import com.dundueni.app.data.remote.api.HistoryPhoneResultDto
import com.dundueni.app.feature.history.HistoryType
import com.dundueni.app.feature.phone.toPhoneResult
import com.dundueni.app.ui.component.InspectionLoadingDialog
import com.dundueni.app.ui.theme.DundueniFETheme
import com.dundueni.app.ui.theme.LocalEasyMode

@Composable
internal fun ResultPreview(
    result: PresentedResult,
    easyMode: Boolean = false,
    imageSummary: ImageSummaryUiState? = null,
    screenResultType: ScreenResultType? = null,
    phoneInspectionInProgress: Boolean = false,
) {
    DundueniFETheme {
        CompositionLocalProvider(LocalEasyMode provides easyMode) {
            AnalysisResultScreen(
                result = result, onAction = {}, isDemo = false,
                imageSummary = imageSummary, screenResultType = screenResultType,
                onInspectPhone = {}, phoneInspectionInProgress = phoneInspectionInProgress,
            )
            if (phoneInspectionInProgress) {
                InspectionLoadingDialog(onDismissPopup = {}, message = "선택한 전화번호를 검사하고 있어요.")
            }
        }
    }
}

@Preview(name = "P1 결과 · 이미지 위험 · 통합 요약 · PNG 저장", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ImageResultPreview() = ImageSummaryCompletedPreview(easyMode = false)

@Preview(name = "P1 결과 · 이미지 위험 · 쉬운 모드", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun EasyImageResultPreview() = ImageSummaryCompletedPreview(easyMode = true)

@Composable
private fun ImageSummaryCompletedPreview(easyMode: Boolean) = ResultPreview(
    checkNotNull(previewImageDetail().toPb05Result(isDemo = false)),
    easyMode = easyMode,
)

@Preview(name = "P1 결과 · 이미지 안전", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ImageNoClearSignalPreview() = ResultPreview(
    checkNotNull(previewImageDetail(noClearSignal = true).toPb05Result(isDemo = false)),
)

@Preview(name = "P1 결과 · 이미지 · 화면 검사 선택 · AI 요약 완료", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ScreenImageSummaryCompletedPreview() = ScreenImageResultPreview(easyMode = false)

@Preview(name = "P1 결과 · 이미지 · 화면 검사 선택 · 쉬운 모드", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun EasyScreenImageResultPreview() = ScreenImageResultPreview(easyMode = true)

@Composable
private fun ScreenImageResultPreview(easyMode: Boolean) {
    val image = previewImageDetail(noClearSignal = true)
    val state = screenState(
        image = previewImageDetail(summaryStatus = "PENDING", noClearSignal = true),
        text = null,
        resultType = ScreenResultType.IMAGE,
    )
    ResultPreview(
        checkNotNull(state.toPresentedResult(false, HistoryType.SCREEN)),
        easyMode = easyMode,
        imageSummary = ImageSummaryUiState(
            ImageSummaryResult.Completed(checkNotNull(image.imageSummary)), checking = false,
        ),
        screenResultType = state.resultType,
    )
}

@Preview(name = "P1 결과 · 텍스트 피싱 의심 · 전화번호 1개 · PNG 저장", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun TextResultPreview() = TextPhishingResultPreview(easyMode = false)

@Preview(name = "P1 결과 · 텍스트 피싱 의심 · 쉬운 모드", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun EasyTextResultPreview() = TextPhishingResultPreview(easyMode = true)

@Composable
private fun TextPhishingResultPreview(easyMode: Boolean) = ResultPreview(
    previewTextDetail(sourceType = "TEXT", decision = "phishing")
        .toTextResult("급하게 송금해 주세요. 문의는 010-1234-5678로 주세요.")
        .copy(extractedPhoneNumbers = listOf(ExtractedPhoneNumber("01012345678", "010-1234-5678", "TEXT"))),
    easyMode = easyMode,
)

@Preview(name = "P1 결과 · 일반 텍스트 · 전화번호 미검출", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun TextNormalResultPreview() = ResultPreview(
    previewTextDetail(sourceType = "TEXT", decision = "normal").toTextResult("토요일에 도서관 앞에서 봐요."),
)

@Preview(name = "P1 결과 · 전화번호 기본 모드 · 안전", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun PhoneSafeResultPreview() =
    ResultPreview(previewPhoneResult(RiskLevel.SAFE))

@Preview(name = "P1 결과 · 전화번호 기본 모드 · 주의 · PNG 저장", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun PhoneResultPreview() =
    ResultPreview(previewPhoneResult(RiskLevel.CAUTION))

@Preview(name = "P1 결과 · 전화번호 기본 모드 · 위험", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun PhoneDangerResultPreview() =
    ResultPreview(previewPhoneResult(RiskLevel.DANGER))

@Preview(name = "P1 결과 · 전화번호 기본 모드 · 판정 미확정", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun PhoneUnknownResultPreview() =
    ResultPreview(previewPhoneResult(RiskLevel.UNKNOWN))

private fun previewPhoneResult(risk: RiskLevel): PresentedResult {
    val details = when (risk) {
        RiskLevel.SAFE -> HistoryPhoneResultDto(reportCount = 0, reasons = listOf("조회된 신고 기록이 없어요."))
        RiskLevel.CAUTION -> HistoryPhoneResultDto(spamType = "광고", reportCount = 3,
            reasons = listOf("광고 전화로 신고된 이력이 있어요."))
        RiskLevel.DANGER -> HistoryPhoneResultDto(spamType = "보이스피싱", reportCount = 8,
            reasons = listOf("보이스피싱 의심 전화로 접수된 신고가 있어요."))
        RiskLevel.UNKNOWN -> HistoryPhoneResultDto()
    }
    return HistoryEntryDto(
        id = "preview-phone-${risk.name}", type = "PHONE", status = "COMPLETED",
        phoneResult = details.copy(phoneNumber = "010********", riskLevel = risk.name),
    ).toPhoneResult()
}

@Preview(name = "P1 결과 · 텍스트 판단 보류", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun TextDeferredResultPreview() = ResultPreview(
    previewTextDetail(sourceType = "TEXT", decision = "defer").toTextResult("확인 부탁드립니다."),
)

@Preview(name = "P1 결과 · OCR · 전화번호 여러 개", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ScreenTextResultPreview() = ScreenOcrResultPreview("phishing")

@Preview(name = "P1 결과 · OCR · 일반 텍스트", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ScreenTextNormalResultPreview() = ScreenOcrResultPreview("normal")

@Preview(name = "P1 결과 · OCR · 판단 보류", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ScreenTextDeferredResultPreview() = ScreenOcrResultPreview("defer")

@Preview(name = "P1 결과 · 이미지와 OCR · 전화번호 여러 개 · 쉬운 모드 · PNG 저장", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun EasyScreenTextResultPreview() = ScreenOcrResultPreview("phishing", easyMode = true, includeImage = true)

@Preview(name = "P1 결과 · OCR · 전화번호 검사 요청 중", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ScreenPhoneInspectionLoadingPreview() = ScreenOcrResultPreview("phishing", phoneInspectionInProgress = true)

@Composable
private fun ScreenOcrResultPreview(
    decision: String,
    easyMode: Boolean = false,
    includeImage: Boolean = false,
    phoneInspectionInProgress: Boolean = false,
) {
    val state = screenState(
        image = if (includeImage) previewImageDetail(noClearSignal = true) else null,
        text = previewTextDetail(decision = decision).toScreenCropAnalysis(),
        resultType = if (includeImage) null else ScreenResultType.TEXT,
    )
    val result = checkNotNull(state.toPresentedResult(false, HistoryType.SCREEN))
    val phones = if (decision == "phishing") listOf(
        ExtractedPhoneNumber("021234567", "02-123-4567", "OCR"),
        ExtractedPhoneNumber("01012345678", "010-1234-5678", "OCR"),
    ) else emptyList()
    ResultPreview(
        result.copy(extractedPhoneNumbers = phones),
        easyMode = easyMode,
        screenResultType = state.resultType,
        phoneInspectionInProgress = phoneInspectionInProgress,
    )
}

@Preview(name = "P1 결과 · 기존 이미지 기록 · 요약만 제공", widthDp = 390, heightDp = 1200, showBackground = true)
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

@Preview(name = "P1 결과 · 전화번호 쉬운 모드 · 안전", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun EasyPhoneSafeResultPreview() =
    ResultPreview(previewPhoneResult(RiskLevel.SAFE), easyMode = true)

@Preview(name = "P1 결과 · 전화번호 쉬운 모드 · 주의", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun EasyPhoneResultPreview() =
    ResultPreview(previewPhoneResult(RiskLevel.CAUTION), easyMode = true)

@Preview(name = "P1 결과 · 전화번호 쉬운 모드 · 위험", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun EasyPhoneDangerResultPreview() =
    ResultPreview(previewPhoneResult(RiskLevel.DANGER), easyMode = true)

@Preview(name = "P1 결과 · 전화번호 쉬운 모드 · 판정 미확정", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun EasyPhoneUnknownResultPreview() =
    ResultPreview(previewPhoneResult(RiskLevel.UNKNOWN), easyMode = true)
