package com.dundueni.app.feature.history

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.dundueni.app.ui.theme.DundueniFETheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class HistoryFlowTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferences get() = context.getSharedPreferences("analysis_result_history", Context.MODE_PRIVATE)
    private var original: String? = null

    @Before fun rememberStore() { original = preferences.getString("results", null) }
    @After fun restoreStore() {
        preferences.edit().apply {
            if (original == null) remove("results") else putString("results", original)
        }.commit()
    }

    @Test fun groupedListOpensDetailAndFiltersByType() {
        compose.setContent { DundueniFETheme { HistoryScreen(historySamples(), {}, {}, {}) } }
        compose.onNodeWithText("오늘").assertIsDisplayed()
        compose.onNodeWithText("AI 생성 의심").assertIsDisplayed()
        screenshot("pb06-list")
        compose.onNodeWithTag("record-demo-1").performScrollTo().performClick()
        compose.onNodeWithText("검사 기록 상세").assertIsDisplayed()
        compose.onNodeWithText("92%").assertIsDisplayed()
        compose.onNodeWithText("18%").assertIsDisplayed()
        screenshot("pb06-detail")
        compose.onNodeWithContentDescription("검사 기록 목록으로 돌아가기").performClick()
        compose.onNodeWithText("이미지 1").performClick()
        compose.onNodeWithTag("record-demo-2").assertIsDisplayed()
        compose.onNodeWithTag("record-demo-1").assertDoesNotExist()
        compose.onNodeWithText("전체 5").performClick()
        compose.onNodeWithTag("record-demo-1").assertIsDisplayed()
    }

    @Test fun clearRequiresConfirmationAndDeletesAllRecordsEvenWithFilter() {
        preferences.edit().putString("results", """[{"id":"one","riskLevel":"SAFE","savedAt":100,"type":"TEXT"},{"id":"two","riskLevel":"DANGER","savedAt":200,"type":"SCREEN"}]""").commit()
        compose.setContent { DundueniFETheme { HistoryRoute(onHome = {}) } }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("전체 2").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("화면 1").performClick()
        compose.onNodeWithContentDescription("전체 검사 기록 삭제").performClick()
        compose.onNodeWithText("전체 기록을 지울까요?").assertIsDisplayed()
        compose.onNodeWithText("취소").performClick()
        assertEquals(2, HistoryStore(context).read().size)
        compose.onNodeWithContentDescription("전체 검사 기록 삭제").performClick()
        compose.onNodeWithText("삭제", substring = false).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("아직 검사 기록이 없어요").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, HistoryStore(context).read().size)
        screenshot("pb06-empty")
    }

    @Test fun narrowLargeTextListStillOpensAndClosesDetail() {
        compose.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, 1.3f)) {
                DundueniFETheme {
                    Box(Modifier.width(320.dp).fillMaxHeight()) { HistoryScreen(easyHistorySamples(), {}, {}, {}, initialEasyMode = true) }
                }
            }
        }
        compose.onNodeWithTag("easy-record-demo-1").performScrollTo().performClick()
        compose.onNodeWithText("검사 기록 상세").assertIsDisplayed()
        compose.onNodeWithTag("history-detail").performScrollToNode(hasText("이 기록 삭제하기"))
        compose.onNodeWithText("이 기록 삭제하기").assertIsDisplayed()
        screenshot("pb06-easy-large-text")
        compose.onNodeWithContentDescription("검사 기록 목록으로 돌아가기").performClick()
        compose.onNodeWithTag("easy-record-demo-1").performScrollTo().assertIsDisplayed()
    }

    @Test fun easyModeMatchesReferenceAndKeepsCardActions() {
        var clears = 0
        compose.setContent { DundueniFETheme { HistoryScreen(easyHistorySamples(), {}, {}, { clears++ }, initialEasyMode = true) } }
        compose.onNodeWithText("Easy History").assertIsDisplayed()
        compose.onNodeWithText("안심 개인정보 보호").assertIsDisplayed()
        compose.onNodeWithText("오늘 검사한 내용").assertIsDisplayed()
        screenshot("pb06-easy-list")
        compose.onNodeWithTag("easy-record-demo-1").performScrollTo().performClick()
        compose.onNodeWithText("검사 기록 상세").assertIsDisplayed()
        compose.onNodeWithContentDescription("검사 기록 목록으로 돌아가기").performClick()
        compose.onNodeWithTag("history-list").performScrollToNode(hasText("문자·링크"))
        compose.onNodeWithText("문자·링크").performClick()
        compose.onNodeWithTag("easy-record-demo-1").assertDoesNotExist()
        compose.onNodeWithTag("easy-record-easy-link").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("easy-record-demo-3").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("history-list").performScrollToNode(hasText("전체 기록 모두 지우기"))
        compose.onNodeWithText("전체 기록 모두 지우기").performClick()
        assertEquals(1, clears)
        compose.onNodeWithContentDescription("기본 모드로 전환").performClick()
        compose.onNodeWithText("든든이").assertIsDisplayed()
    }
    @Test fun homeRecentEntryOpensRequestedRecordAndReturnsToHomeCallback() {
        var homes = 0
        compose.setContent { DundueniFETheme {
            HistoryScreen(historySamples(), {}, {}, {}, initialRecordId = "demo-3", onCloseDetail = { homes++ })
        } }
        compose.onNodeWithText("검사 기록 상세").assertIsDisplayed()
        compose.onNodeWithText("텍스트 검사").assertIsDisplayed()
        compose.onNodeWithContentDescription("홈으로 돌아가기").performClick()
        assertEquals(1, homes)
    }

    @Test fun phoneFilterAndGlobalSettingsNavigationRemainAvailableInEasyMode() {
        var destination: com.dundueni.app.feature.home.HomeDestination? = null
        val record = historySamples().first().copy(id = "phone", type = HistoryType.PHONE, riskLevel = com.dundueni.app.feature.analysisresult.RiskLevel.UNKNOWN)
        compose.setContent { DundueniFETheme {
            HistoryScreen(listOf(record), {}, {}, {}, initialEasyMode = true, onNavigationSelected = { destination = it })
        } }
        compose.onNodeWithText("전화번호").performScrollTo().performClick()
        compose.onNodeWithTag("easy-record-phone").performScrollTo().performClick()
        compose.onNodeWithText("최종 위험 단계: 판정 미확정").assertIsDisplayed()
        compose.onNodeWithContentDescription("검사 기록 목록으로 돌아가기").performClick()
        compose.onNodeWithContentDescription("기록 설정").performClick()
        assertEquals(com.dundueni.app.feature.home.HomeDestination.SETTINGS, destination)
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        File(context.getExternalFilesDir(null), "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}