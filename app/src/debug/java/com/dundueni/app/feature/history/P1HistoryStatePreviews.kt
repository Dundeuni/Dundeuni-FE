package com.dundueni.app.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
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

@Preview(name = "P1 기록 · 상세 조회 실패", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun HistoryDetailErrorPreview() = DundueniFETheme {
    HistoryScreen(
        records = emptyList(), onHome = {}, onDelete = {}, onClear = {},
        initialRecordId = "preview-text", onCloseDetail = {}, allowDelete = false,
        detailContent = { HistoryResultDetail(detail = null, error = "기록 상세를 불러오지 못했어요.", onRetry = {}) },
    )
}

@Preview(name = "P1 기록 · 상세 근거 없음", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun HistoryDetailWithoutEvidencePreview() = TextHistoryDetailPreview(emptyList())

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
