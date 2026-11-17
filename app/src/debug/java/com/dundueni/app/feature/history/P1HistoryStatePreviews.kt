package com.dundueni.app.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
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
private fun HistoryDetailPreview() = DundueniFETheme {
    val record = historySamples().first().copy(evidence = listOf("송금을 재촉하는 문구가 있어요."))
    HistoryScreen(
        records = listOf(record), onHome = {}, onDelete = {}, onClear = {},
        initialRecordId = record.id, onCloseDetail = {}, allowDelete = false,
    )
}

@Preview(name = "P1 기록 · 상세 조회 실패", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun HistoryDetailErrorPreview() = DundueniFETheme {
    val record = historySamples().first()
    HistoryScreen(
        records = listOf(record), onHome = {}, onDelete = {}, onClear = {},
        initialRecordId = record.id, onCloseDetail = {}, allowDelete = false,
        error = "기록 상세를 불러오지 못했어요. 다시 시도해주세요.",
    )
}

@Preview(name = "P1 기록 · 상세 근거 없음", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun HistoryDetailWithoutEvidencePreview() = DundueniFETheme {
    val record = historySamples().first().copy(evidence = emptyList())
    HistoryScreen(
        records = listOf(record), onHome = {}, onDelete = {}, onClear = {},
        initialRecordId = record.id, onCloseDetail = {}, allowDelete = false,
    )
}
