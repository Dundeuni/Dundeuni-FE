package com.dundueni.app.feature.analysis

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsNotEnabled
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
        compose.setContent { DundueniFETheme { AnalysisFlowScreen(state.value, {}, {}) } }
        compose.onNodeWithText("분석 중").assertIsDisplayed()
        compose.runOnIdle {
            state.value = AnalysisUiState.Success(result("MOCK_CAUTION"))
        }
        compose.onNodeWithText("분석 중").assertDoesNotExist()
        compose.onNodeWithText("조금 더 확인해보세요").assertIsDisplayed()
        compose.onNodeWithText("분석 점수 41.2%").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("분석 근거 1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("링크 열지 않고 도메인만 미리보기").performScrollTo().assertIsNotEnabled()
    }

    @Test fun safeUsesPb05AndReturnsThroughExistingCloseCallback() {
        var closes = 0
        compose.setContent { DundueniFETheme {
            AnalysisFlowScreen(AnalysisUiState.Success(result("MOCK_SAFE")), {}, { closes++ }, isDemo = true)
        } }
        compose.onNodeWithText("현재는 안전해 보여요").assertIsDisplayed()
        compose.onNodeWithText("원래 화면으로 돌아가기").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, closes) }
    }

    @Test fun dangerUsesPb05UrgentWarning() {
        compose.setContent { DundueniFETheme {
            AnalysisFlowScreen(AnalysisUiState.Success(result("MOCK_DANGER")), {}, {}, isDemo = true)
        } }
        compose.onNodeWithText("위험할 가능성이 높아요").assertIsDisplayed()
        compose.onNodeWithText("지금은 절대 돈을 보내지 마세요!").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("신고 (112/118)").performScrollTo().assertIsDisplayed()
    }

    @Test fun realAiRiskIsDisplayedWithoutInventingAnOverallVerdict() {
        compose.setContent { DundueniFETheme {
            AnalysisFlowScreen(AnalysisUiState.Success(result("SAFE")), {}, {}, isDemo = false)
        } }
        compose.onNodeWithText("최종 위험 판정 미확정").assertIsDisplayed()
        compose.onNodeWithText("서버 AI 위험 수준: SAFE").assertIsDisplayed()
        compose.onNodeWithText("AI 생성·변조 점수: 41.2%").assertIsDisplayed()
        compose.onNodeWithText("test-reason").assertIsDisplayed()
        compose.onNodeWithText("현재는 안전해 보여요").assertDoesNotExist()
        compose.onNodeWithText("Mock 검사 — 실제 이미지 분석 결과가 아닙니다.").assertDoesNotExist()
    }

    @Test fun unknownDemoRiskDoesNotBecomeSafe() {
        compose.setContent { DundueniFETheme {
            AnalysisFlowScreen(AnalysisUiState.Success(result("FUTURE_RISK")), {}, {}, isDemo = true)
        } }
        compose.onNodeWithText("최종 위험 판정 미확정").assertIsDisplayed()
        compose.onNodeWithText("현재는 안전해 보여요").assertDoesNotExist()
    }

    @Test fun errorOffersRetryAndClose() {
        var retries = 0
        var closes = 0
        compose.setContent { DundueniFETheme { AnalysisFlowScreen(AnalysisUiState.Error, { retries++ }, { closes++ }) } }
        compose.onNodeWithText("다시 시도").performClick()
        compose.onNodeWithText("닫기").performClick()
        compose.runOnIdle {
            assertEquals(1, retries)
            assertEquals(1, closes)
        }
    }

    private fun result(risk: String) = AnalysisResult(
        analysisId = "test-id", type = "IMAGE", status = "COMPLETED",
        aiGenerationScore = 41.2, aiRiskLevel = risk, reasons = listOf("test-reason"),
        modelVersion = null, errorCode = null, createdAt = null, completedAt = null, expiresAt = null,
    )
}
