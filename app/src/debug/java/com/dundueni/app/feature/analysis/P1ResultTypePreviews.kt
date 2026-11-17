package com.dundueni.app.feature.analysis

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dundueni.app.feature.analysisresult.AnalysisResult as PresentedResult
import com.dundueni.app.feature.analysisresult.AnalysisResultScreen
import com.dundueni.app.feature.analysisresult.RiskEvidence
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.feature.history.HistoryType
import com.dundueni.app.ui.theme.DundueniFETheme

@Composable
private fun ResultPreview(result: PresentedResult) {
    DundueniFETheme {
        AnalysisResultScreen(result = result, onAction = {}, isDemo = false)
    }
}

@Preview(name = "P1 결과 · 이미지 검사", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun ImageResultPreview() = ResultPreview(
    checkNotNull(previewImageDetail().toPb05Result(isDemo = false)),
)

@Preview(name = "P1 결과 · 텍스트 검사", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun TextResultPreview() = ResultPreview(
    previewTextDetail(sourceType = "DIRECT").toTextResult("급하게 송금해 주세요."),
)

// Same display contract as HistoryEntryDto.toPhoneResult(): the detail response supplies
// the risk level and evidence; the POST completion metadata alone does not.
@Preview(name = "P1 결과 · 전화번호 검사", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun PhoneResultPreview() = ResultPreview(
    PresentedResult(
        id = "preview-phone", historyType = HistoryType.PHONE,
        riskLevel = RiskLevel.CAUTION,
        summary = "신고된 이력이 있는 전화번호예요.",
        keyFinding = "스팸 유형: 광고 · 신고 건수: 3건",
        evidence = listOf(RiskEvidence("전화번호 분석 근거", "최근 신고 이력이 확인되었어요.")),
        targetLabel = "010-0000-0000",
    ),
)

@Preview(name = "P1 결과 · 판정 및 일부 정보 없음", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun MissingResultFieldsPreview() = ResultPreview(
    previewTextDetail(sourceType = "DIRECT").copy(
        normalizedRiskLevel = null,
        summary = null,
        riskScore = null,
        textEvidence = emptyList(),
        textLimitations = emptyList(),
        textDecision = null,
    ).toTextResult(),
)
