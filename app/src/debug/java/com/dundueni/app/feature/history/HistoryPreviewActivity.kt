package com.dundueni.app.feature.history

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.ui.theme.DundueniFETheme
import java.time.LocalDate
import java.time.ZoneId

internal fun historySamples(): List<HistoryRecord> {
    val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    fun at(days: Int, hour: Int, minute: Int) = today - days * 86_400_000L + (hour * 60L + minute) * 60_000L
    return listOf(
        HistoryRecord("demo-1", RiskLevel.DANGER, at(0,14,32), HistoryType.SCREEN, 92.0, 18.0, "가족을 사칭한 메시지 · 송금 요청"),
        HistoryRecord("demo-2", RiskLevel.CAUTION, at(0,13,8), HistoryType.LINK, 64.0, 28.0, "출처를 확인하기 어려운 링크"),
        HistoryRecord("demo-3", RiskLevel.CAUTION, at(0,11,26), HistoryType.IMAGE, null, 87.0, "첨부 이미지 · AI 생성·변조 가능성"),
        HistoryRecord("demo-4", RiskLevel.SAFE, at(1,18,40), HistoryType.TEXT, 8.0, 4.0, "우체국 택배 배송 안내"),
        HistoryRecord("demo-5", RiskLevel.DANGER, at(1,15,12), HistoryType.SCREEN, 91.0, null, "당근마켓 거래 화면 · 외부 결제 요청"),
    )
}

class HistoryPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DundueniFETheme {
            var samples by remember { mutableStateOf(historySamples()) }
            HistoryScreen(samples, onHome = { finish() }, onDelete = { id -> samples = samples.filterNot { it.id == id } }, onClear = { samples = emptyList() }, isDemo = true)
        } }
    }
}

@Preview(name = "PB-06 검사 기록 목록", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun HistoryListPreview() { DundueniFETheme { HistoryScreen(historySamples(), {}, {}, {}, isDemo = true) } }

@Preview(name = "PB-06 기록 없음", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun HistoryEmptyPreview() { DundueniFETheme { HistoryScreen(emptyList(), {}, {}, {}) } }

@Preview(name = "PB-06 작은 화면 큰 글씨", widthDp = 320, heightDp = 720, fontScale = 1.3f, showBackground = true)
@Composable
private fun HistoryLargeTextPreview() { DundueniFETheme { HistoryScreen(historySamples(), {}, {}, {}, isDemo = true) } }
