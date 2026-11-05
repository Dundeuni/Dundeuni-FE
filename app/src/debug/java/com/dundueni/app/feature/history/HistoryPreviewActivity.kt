package com.dundueni.app.feature.history

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.dundueni.app.feature.analysisresult.AiLikelihood
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.ui.theme.DundueniFETheme
import java.time.LocalDate
import java.time.ZoneId

internal fun historySamples(): List<HistoryRecord> {
    val zone = ZoneId.systemDefault()
    fun at(days: Long, hour: Int, minute: Int) = LocalDate.now().minusDays(days).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
    return listOf(
        HistoryRecord("demo-1", RiskLevel.DANGER, at(0, 14, 32), HistoryType.SCREEN, 92.0, 18.0, "“엄마 나 액정 깨져서 수리 맡겼는데…”"),
        HistoryRecord("demo-2", RiskLevel.CAUTION, at(0, 13, 8), HistoryType.IMAGE, null, 87.0, "투자·수익 안내 이미지\nAI 생성·변조 가능성", aiLikelihood = AiLikelihood.HIGH),
        HistoryRecord("demo-3", RiskLevel.SAFE, at(0, 11, 26), HistoryType.TEXT, 8.0, 4.0, "“우체국 택배 배송 안내”"),
        HistoryRecord("demo-4", RiskLevel.DANGER, at(1, 18, 40), HistoryType.LINK, 91.0, null, "“본인 확인이 필요해요 — 첨부 링크 클릭…”"),
        HistoryRecord("demo-5", RiskLevel.DANGER, at(1, 16, 12), HistoryType.SCREEN, 95.0, null, "“안전결제 수수료를 송금해 주세요…”"),
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

/** Fictional examples for the three easy-mode states shown in the design. */
internal fun easyHistorySamples(): List<HistoryRecord> {
    val samples = historySamples()
    return listOf(
        samples.first { it.id == "demo-1" },
        samples.first { it.id == "demo-2" }.copy(id = "easy-link", type = HistoryType.LINK,
            fraudScore = 64.0, aiScore = 28.0, aiLikelihood = AiLikelihood.UNKNOWN,
            preview = "알 수 없는 단축 주소 (me2.kr/safe…)"),
        samples.first { it.id == "demo-3" }.copy(preview = "정상 배송 알림 확인 완료"),
    )
}

@Preview(name = "PB-06 쉬운 모드", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun EasyHistoryPreview() { DundueniFETheme { HistoryScreen(easyHistorySamples(), {}, {}, {}, initialEasyMode = true) } }

@Preview(name = "PB-06 쉬운 모드 큰 글씨", widthDp = 320, heightDp = 720, fontScale = 1.3f, showBackground = true)
@Composable
private fun EasyHistoryLargeTextPreview() { DundueniFETheme { HistoryScreen(easyHistorySamples(), {}, {}, {}, initialEasyMode = true) } }