package com.dundueni.app.feature.history

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.ui.theme.DundueniFETheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HistoryScreenTest {
    @get:Rule val compose = createComposeRule()
    private val record = HistoryRecord("test", RiskLevel.DANGER, 100, HistoryType.TEXT, 82.0)

    @Test fun emptyStateReturnsHome() {
        var homes = 0
        compose.setContent { DundueniFETheme { HistoryScreen(emptyList(), { homes++ }, {}, {}) } }
        compose.onNodeWithText("아직 검사 기록이 없어요").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("홈으로 돌아가기").performScrollTo().performClick()
        assertEquals(1, homes)
    }

    @Test fun openingLegacyRecordShowsScoresAndHonestMissingEvidence() {
        var deleted: String? = null
        compose.setContent { DundueniFETheme { HistoryScreen(listOf(record), {}, { deleted = it }, {}) } }
        compose.onNodeWithText("텍스트 검사 ·", substring = true).performScrollTo().performClick()
        compose.onNodeWithText("검사 기록 상세").assertIsDisplayed()
        compose.onNodeWithText("82%").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("history-detail").performScrollToNode(hasText("이 기록에는 세부 근거가 저장되어 있지 않아요.", substring = true))
        compose.onNodeWithText("이 기록에는 세부 근거가 저장되어 있지 않아요.", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("history-detail").performScrollToNode(hasText("이 기록 삭제하기"))
        compose.onNodeWithText("이 기록 삭제하기").performClick()
        assertEquals(record.id, deleted)
    }

    @Test fun noFilterMatchesCanResetToAllRecords() {
        compose.setContent { DundueniFETheme { HistoryScreen(listOf(record), {}, {}, {}) } }
        compose.onNodeWithText("화면 0").performClick()
        compose.onNodeWithText("조건에 맞는 기록이 없어요").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("전체 기록 보기").performScrollTo().performClick()
        compose.onNodeWithText("텍스트 검사 ·", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test fun deletionWhileViewingDetailReturnsToEmptyList() {
        val records = mutableStateOf(listOf(record))
        compose.setContent { DundueniFETheme { HistoryScreen(records.value, {}, { records.value = emptyList() }, {}) } }
        compose.onNodeWithText("텍스트 검사 ·", substring = true).performScrollTo().performClick()
        compose.onNodeWithTag("history-detail").performScrollToNode(hasText("이 기록 삭제하기"))
        compose.onNodeWithText("이 기록 삭제하기").performClick()
        compose.onNodeWithText("아직 검사 기록이 없어요").performScrollTo().assertIsDisplayed()
    }

    @Test fun koreanSearchFiltersRecordsAndClearingRestoresList() {
        compose.setContent { DundueniFETheme { HistoryScreen(listOf(record), {}, {}, {}) } }
        compose.onNodeWithContentDescription("검사 기록 검색").performClick()
        compose.onNodeWithTag("history-search").performTextInput("위험")
        compose.onNodeWithTag("history-search").assertTextContains("위험")
        compose.onNodeWithTag("history-search").performImeAction()
        compose.onNodeWithTag("history-list").performScrollToNode(hasText("텍스트 검사 ·", substring = true))
        compose.onNodeWithText("텍스트 검사 ·", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("history-list").performScrollToNode(hasTestTag("history-search"))
        compose.onNodeWithTag("history-search").performTextReplacement("안전")
        compose.onNodeWithTag("history-search").performImeAction()
        compose.onNodeWithTag("history-list").performScrollToNode(hasText("조건에 맞는 기록이 없어요"))
        compose.onNodeWithText("조건에 맞는 기록이 없어요").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("history-list").performScrollToNode(hasTestTag("history-search"))
        compose.onNodeWithTag("history-search").performTextClearance()
        compose.onNodeWithTag("history-search").performImeAction()
        compose.onNodeWithTag("history-list").performScrollToNode(hasText("텍스트 검사 ·", substring = true))
        compose.onNodeWithText("텍스트 검사 ·", substring = true).assertIsDisplayed()
    }

    @Test fun easyDangerCardShowsWarningAndOpensDetail() {
        compose.setContent { DundueniFETheme { HistoryScreen(listOf(record), {}, {}, {}) } }
        compose.onNodeWithText("쉬운 모드").performClick()
        compose.onNodeWithText("위험 발견").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("위험 판정 기록이에요. 송금 전 다시 확인하세요.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("easy-record-test").performClick()
        compose.onNodeWithText("검사 기록 상세").assertIsDisplayed()
    }

    @Test fun homeEntryWaitsForRecordsThenOpensSelectedDetailAndReturnsHome() {
        val records = mutableStateOf(emptyList<HistoryRecord>())
        val loading = mutableStateOf(true)
        var homes = 0
        compose.setContent { DundueniFETheme {
            HistoryScreen(records.value, {}, {}, {}, loading = loading.value,
                initialRecordId = record.id, onCloseDetail = { homes++ })
        } }
        compose.runOnIdle { records.value = listOf(record); loading.value = false }
        compose.onNodeWithText("검사 기록 상세").assertIsDisplayed()
        compose.onNodeWithContentDescription("홈으로 돌아가기").performClick()
        assertEquals(1, homes)
    }
}
