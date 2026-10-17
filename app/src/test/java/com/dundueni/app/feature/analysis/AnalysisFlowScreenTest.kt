package com.dundueni.app.feature.analysis

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.StateRestorationTester
import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.ui.theme.DundueniFETheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AnalysisFlowScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun successReplacesLoadingWithPassedDomainResult() {
        val state = mutableStateOf<AnalysisUiState>(AnalysisUiState.Loading)
        compose.setContent { DundueniFETheme { AnalysisFlowScreen(state.value, {}, {}, {}, isDemo = true) } }
        compose.onNodeWithText("잠시만 기다려 주세요").assertIsDisplayed()
        compose.runOnIdle {
            state.value = AnalysisUiState.Success(result("MOCK_CAUTION"))
        }
        compose.onNodeWithText("잠시만 기다려 주세요").assertDoesNotExist()
        compose.onNodeWithText("조금 더 확인해보세요").assertIsDisplayed()
        compose.onNodeWithText("분석 점수 41.2%").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("분석 근거 1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("링크 열지 않고 도메인만 미리보기").performScrollTo().assertIsNotEnabled()
    }

    @Test fun dismissingLoadingPopupKeepsRequestVisibleAndResultArrives() {
        val state = mutableStateOf<AnalysisUiState>(AnalysisUiState.Loading)
        val restoration = StateRestorationTester(compose)
        restoration.setContent { DundueniFETheme { AnalysisFlowScreen(state.value, {}, {}, {}, isDemo = true) } }

        compose.onNodeWithText("닫고 검사 계속하기").performClick()
        compose.onNodeWithText("잠시만 기다려 주세요").assertDoesNotExist()
        compose.onNodeWithText("검사가 진행 중이에요. 완료되면 결과가 표시됩니다.").assertIsDisplayed()
        compose.runOnIdle { assertEquals(AnalysisUiState.Loading, state.value) }

        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("잠시만 기다려 주세요").assertDoesNotExist()
        compose.runOnIdle { state.value = AnalysisUiState.Success(result("MOCK_CAUTION")) }
        compose.onNodeWithText("조금 더 확인해보세요").assertIsDisplayed()
    }

    @Test fun safeUsesPb05AndReturnsThroughExistingCloseCallback() {
        var closes = 0
        compose.setContent { DundueniFETheme {
            AnalysisFlowScreen(AnalysisUiState.Success(result("MOCK_SAFE")), {}, { closes++ }, {}, isDemo = true)
        } }
        compose.onNodeWithText("현재는 안전해 보여요").assertIsDisplayed()
        compose.onNodeWithText("원래 화면으로 돌아가기").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, closes) }
    }

    @Test fun dangerUsesPb05UrgentWarning() {
        compose.setContent { DundueniFETheme {
            AnalysisFlowScreen(AnalysisUiState.Success(result("MOCK_DANGER")), {}, {}, {}, isDemo = true)
        } }
        compose.onNodeWithText("위험할 가능성이 높아요").assertIsDisplayed()
        compose.onNodeWithText("지금은 절대 돈을 보내지 마세요!").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("신고 (112/118)").performScrollTo().assertIsDisplayed()
    }

    @Test fun realResultUsesSharedPb05LayoutWithoutInventingAnOverallVerdict() {
        compose.setContent { DundueniFETheme {
        AnalysisFlowScreen(AnalysisUiState.Success(result("SAFE")), {}, {}, {}, isDemo = false)
        } }
        compose.onNodeWithText("최종 위험 판정 미확정").assertIsDisplayed()
        compose.onNodeWithText("결과 정보를 확인해주세요").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("SAFE").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("분석 점수 41.2%").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("test-reason").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("현재는 안전해 보여요").assertDoesNotExist()
        compose.onNodeWithText("원래 화면으로 돌아가기").performScrollTo().assertIsDisplayed()
    }

    @Test fun unknownDemoRiskDoesNotBecomeSafe() {
        compose.setContent { DundueniFETheme {
        AnalysisFlowScreen(AnalysisUiState.Success(result("FUTURE_RISK")), {}, {}, {}, isDemo = true)
        } }
        compose.onNodeWithText("최종 위험 판정 미확정").assertIsDisplayed()
        compose.onNodeWithText("현재는 안전해 보여요").assertDoesNotExist()
    }

    @Test fun errorRetriesThroughLoadingAndCanCancel() {
        var retries = 0
        var closes = 0
        val state = mutableStateOf<AnalysisUiState>(AnalysisUiState.Loading)
        compose.setContent { DundueniFETheme { AnalysisFlowScreen(state.value,
            { retries++; state.value = AnalysisUiState.Loading }, { closes++ }, {}) } }
        compose.onNodeWithText("잠시만 기다려 주세요").assertIsDisplayed()
        compose.runOnIdle { state.value = AnalysisUiState.Error() }
        compose.onNodeWithText("잠시만 기다려 주세요").assertDoesNotExist()
        compose.onNodeWithText("검사를 완료하지 못했어요").assertIsDisplayed()
        compose.onNodeWithText("다시 시도").performClick()
        compose.onNodeWithText("잠시만 기다려 주세요").assertIsDisplayed()
        compose.onNodeWithText("검사를 완료하지 못했어요").assertDoesNotExist()
        compose.runOnIdle { state.value = AnalysisUiState.Error() }
        compose.onNodeWithText("검사 취소").performClick()
        compose.runOnIdle {
            assertEquals(1, retries)
            assertEquals(1, closes)
        }
    }

    @Test fun configurationErrorOnlyOffersCancel() {
        compose.setContent { DundueniFETheme {
            AnalysisFlowScreen(AnalysisUiState.Error(AnalysisErrorKind.CONFIGURATION), {}, {}, {}, isDemo = false)
        } }
        compose.onNodeWithText("다시 시도").assertDoesNotExist()
        compose.onNodeWithText("검사 취소").assertIsDisplayed()
    }

    @Test fun realResultWithNullFieldsUsesSharedUnknownLayoutAndCanClose() {
        var closes = 0
        val missing = result("unused").copy(analysisId = null, type = null, aiGenerationScore = null,
            aiRiskLevel = null, reasons = emptyList())
        compose.setContent { DundueniFETheme {
            AnalysisFlowScreen(AnalysisUiState.Success(missing), {}, { closes++ }, {}, isDemo = false)
        } }
        compose.onNodeWithText("최종 위험 판정 미확정").assertIsDisplayed()
        compose.onAllNodesWithText("정보 없음").assertCountEquals(2)
        compose.onNodeWithText("세부 근거가 제공되지 않았어요. 요약과 대응 행동을 확인해주세요.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("현재는 안전해 보여요").assertDoesNotExist()
        compose.onNodeWithText("원래 화면으로 돌아가기").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, closes) }
    }

    @Test fun errorsShowDistinctMessagesAndSafeHttpStatus() {
        val state = mutableStateOf<AnalysisUiState>(AnalysisUiState.Error(AnalysisErrorKind.CONNECTION))
        compose.setContent { DundueniFETheme { AnalysisFlowScreen(state.value, {}, {}, {}, isDemo = false) } }
        for (kind in listOf(AnalysisErrorKind.CONNECTION, AnalysisErrorKind.TIMEOUT, AnalysisErrorKind.HTTP, AnalysisErrorKind.RESPONSE)) {
            compose.runOnIdle { state.value = AnalysisUiState.Error(kind, if (kind == AnalysisErrorKind.HTTP) 503 else null) }
            compose.onNodeWithText(kind.message, substring = true).assertIsDisplayed()
            compose.onNodeWithText("다시 시도").assertIsDisplayed()
            if (kind == AnalysisErrorKind.HTTP) compose.onNodeWithText("HTTP 상태: 503", substring = true).assertIsDisplayed()
        }
    }

    private fun result(risk: String) = AnalysisResult(
        analysisId = "test-id", type = "IMAGE", status = "COMPLETED",
        aiGenerationScore = 41.2, aiRiskLevel = risk, reasons = listOf("test-reason"),
        modelVersion = null, errorCode = null, createdAt = null, completedAt = null, expiresAt = null,
    )
}
