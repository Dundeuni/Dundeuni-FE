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
    @Test fun easyTextAndLinkFilterCombinesTypesAndBasicModeResetsFilter() {
        val records = listOf(
            record.copy(id = "screen", type = HistoryType.SCREEN),
            record.copy(id = "link", type = HistoryType.LINK, riskLevel = RiskLevel.CAUTION),
            record.copy(id = "text", riskLevel = RiskLevel.SAFE),
        )
        compose.setContent { DundueniFETheme { HistoryScreen(records, {}, {}, {}, initialEasyMode = true) } }
        compose.onNodeWithText("Easy History").assertIsDisplayed()
        compose.onNodeWithText("문자·링크").performClick()
        compose.onNodeWithTag("easy-record-screen").assertDoesNotExist()
        compose.onNodeWithTag("easy-record-link").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("easy-record-text").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("기본 모드로 전환").performClick()
        compose.onNodeWithText("든든이").assertIsDisplayed()
        compose.onNodeWithText("전체 3").assertIsSelected()
        compose.onNodeWithTag("record-screen").performScrollTo().assertIsDisplayed()
    }

    @Test fun easyClearButtonRequestsDeletionWithoutRemovingRecordsItself() {
        var requested = 0
        compose.setContent { DundueniFETheme { HistoryScreen(listOf(record), {}, {}, { requested++ }, initialEasyMode = true) } }
        compose.onNodeWithTag("history-list").performScrollToNode(hasText("전체 기록 모두 지우기"))
        compose.onNodeWithText("전체 기록 모두 지우기").performClick()
        compose.runOnIdle { assertEquals(1, requested) }
        compose.onNodeWithTag("easy-record-test").performScrollTo().assertIsDisplayed()
    }
    @Test fun phoneAndUnclassifiedHistoryKeepTheirTypeAndNavigation() {
        var destination: com.dundueni.app.feature.home.HomeDestination? = null
        val phone = record.copy(id = "phone", type = HistoryType.PHONE, riskLevel = RiskLevel.UNKNOWN)
        compose.setContent { DundueniFETheme {
            HistoryScreen(listOf(phone), {}, {}, {}, initialEasyMode = true, onNavigationSelected = { destination = it })
        } }
        compose.onNodeWithText("전화번호").performScrollTo().performClick()
        compose.onNodeWithTag("easy-record-phone").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("안전 확인").assertDoesNotExist()
        compose.onNodeWithTag("easy-record-phone").performClick()
        compose.onNodeWithText("최종 위험 단계: 판정 미확정").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("검사 기록 목록으로 돌아가기").performClick()
        compose.onNodeWithContentDescription("기록 설정").performClick()
        assertEquals(com.dundueni.app.feature.home.HomeDestination.SETTINGS, destination)
        compose.onNodeWithContentDescription("내 기록 정보").performClick()
        assertEquals(com.dundueni.app.feature.home.HomeDestination.MY_SHELTER, destination)
    }
}
