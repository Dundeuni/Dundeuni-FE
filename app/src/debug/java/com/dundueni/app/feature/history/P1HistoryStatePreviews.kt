package com.dundueni.app.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dundueni.app.data.remote.dto.AnalysisModelResultDto
import com.dundueni.app.data.remote.dto.AnalysisReasonDto
import com.dundueni.app.data.remote.dto.HistoryDetailDto
import com.dundueni.app.data.remote.dto.HistoryReasonDto
import com.dundueni.app.data.remote.dto.HistoryTypeDetailDto
import com.dundueni.app.ui.theme.DundueniFETheme

@Preview(name = "P1 기록 · 목록 조회 중", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun HistoryLoadingPreview() = DundueniFETheme {
    HistoryScreen(
        records = emptyList(), onHome = {}, onDelete = {}, onClear = {},
        loading = true, allowDelete = false,
    )
}

@Preview(name = "P1 기록 · 서버 조회 실패", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun HistoryLoadErrorPreview() = DundueniFETheme {
    HistoryScreen(
        records = emptyList(), onHome = {}, onDelete = {}, onClear = {},
        error = "기록을 불러오지 못했어요. 다시 시도해주세요.",
        allowDelete = false,
    )
}

@Preview(name = "P1 기록 · 검사 상세", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun HistoryDetailPreview() = TextHistoryDetailPreview(listOf("송금을 재촉하는 문구가 있어요."))

@Preview(name = "P1 기록 · 이미지 상세 조회 실패", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun HistoryDetailErrorPreview() = DundueniFETheme {
    HistoryScreen(
        records = emptyList(), onHome = {}, onDelete = {}, onClear = {},
        initialRecordId = "preview-image", onCloseDetail = {}, allowDelete = false,
        detailContent = {
            HistoryResultDetail(detail = null, error = "검사 기록이 없거나 더 이상 볼 수 없어요.", onRetry = {})
        },
    )
}

@Preview(name = "P1 기록 · 상세 근거 없음", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun HistoryDetailWithoutEvidencePreview() = TextHistoryDetailPreview(emptyList())

@Preview(name = "P1 기록 · 이미지 모델 결과 및 저장 요약", widthDp = 390, heightDp = 1020, showBackground = true)
@Composable
private fun ImageHistoryDetailPreview() = ImageHistoryPreview(previewImageHistory())

@Preview(name = "P1 기록 · 이미지 모델 부분 실패 · 쉬운 모드", widthDp = 390, heightDp = 1020, showBackground = true)
@Composable
private fun ImageHistoryPartialFailurePreview() = ImageHistoryPreview(
    previewImageHistory(partialFailure = true), easyMode = true,
)

@Preview(name = "P1 기록 · 이미지 요약 없음", widthDp = 390, heightDp = 1020, showBackground = true)
@Composable
private fun ImageHistoryWithoutSummaryPreview() = ImageHistoryPreview(previewImageHistory(summaryStatus = null))

@Preview(name = "P1 기록 · 기존 이미지 응답", widthDp = 390, heightDp = 1020, showBackground = true)
@Composable
private fun LegacyImageHistoryPreview() = ImageHistoryPreview(
    HistoryDetailDto(
        analysisId = "preview-legacy-image", type = "IMAGE", status = "COMPLETED",
        createdAt = "2026-11-19T05:32:00Z", aiGenerationScore = 82.0,
        reasons = listOf(HistoryReasonDto("이미지 일부에서 생성 흔적을 확인했습니다.")),
    ),
)

@Composable
private fun ImageHistoryPreview(dto: HistoryDetailDto, easyMode: Boolean = false) = DundueniFETheme {
    val detail = dto.toRecordDetail()
    HistoryScreen(
        records = listOf(detail.record), onHome = {}, onDelete = {}, onClear = {},
        initialRecordId = detail.record.id, onCloseDetail = {}, allowDelete = false,
        initialEasyMode = easyMode,
        detailContent = { HistoryResultDetail(detail, onRetry = {}) },
    )
}

private fun previewImageHistory(
    summaryStatus: String? = "COMPLETED",
    partialFailure: Boolean = false,
) = HistoryDetailDto(
    analysisId = "preview-image", type = "IMAGE", status = "COMPLETED",
    createdAt = "2026-11-25T07:00:00Z", completedAt = "2026-11-25T07:00:30Z",
    expiresAt = "2026-11-29T07:00:00Z",
    detail = HistoryTypeDetailDto(
        summaryStatus = summaryStatus,
        summary = if (summaryStatus != "COMPLETED") null
            else if (partialFailure) "이미지 변조 분석을 완료하지 못해 추가 확인이 필요합니다."
            else "이미지 일부에서 변조 의심 신호가 확인됐습니다.",
        imageResultStatus = if (partialFailure) "PARTIAL_FAILED" else "COMPLETED",
        modelResults = listOf(
            if (partialFailure) AnalysisModelResultDto(kind = "MANIPULATION", status = "FAILED", retryable = true)
            else AnalysisModelResultDto(
                kind = "MANIPULATION", status = "SUCCEEDED", displayScore = 84.0,
                decision = "MANIPULATION_SUSPECTED",
                reasons = listOf(AnalysisReasonDto("이미지 일부에서 편집 흔적이 확인됐습니다.")),
            ),
            AnalysisModelResultDto(
                kind = "GENERATION", status = "SUCCEEDED", displayScore = 12.0,
                decision = "NO_CLEAR_GENERATION_SIGNAL",
                reasons = listOf(AnalysisReasonDto("뚜렷한 생성 신호가 확인되지 않았습니다.")),
            ),
        ),
    ),
)

@Composable
private fun TextHistoryDetailPreview(evidence: List<String>) = DundueniFETheme {
    val detail = HistoryDetailDto(
        analysisId = "preview-text",
        type = "TEXT",
        status = "COMPLETED",
        createdAt = "2026-11-19T05:32:00Z",
        reasons = evidence.map { HistoryReasonDto(it) },
        detail = HistoryTypeDetailDto(
            textPreview = "오늘까지 아래 계좌로 송금해 주세요.",
            decision = "phishing",
            riskScore = 0.89,
        ),
    ).toRecordDetail()
    HistoryScreen(
        records = listOf(detail.record), onHome = {}, onDelete = {}, onClear = {},
        initialRecordId = detail.record.id, onCloseDetail = {}, allowDelete = false,
        detailContent = { HistoryResultDetail(detail, onRetry = {}) },
    )
}
