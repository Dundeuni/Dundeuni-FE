package com.dundueni.app.feature.analysis

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
            state.value = AnalysisUiState.Success(AnalysisResult(analysisId = "test-id", type = "IMAGE", status = "COMPLETED", aiGenerationScore = 41.2, aiRiskLevel = "MOCK_MEDIUM", reasons = listOf("test-reason"), modelVersion = null, errorCode = null, createdAt = null, completedAt = null, expiresAt = null, recommendedActions = listOf("test-action")))
        }
        compose.onNodeWithText("위험도: MOCK_MEDIUM").assertIsDisplayed()
        compose.onNodeWithText("test-reason").assertIsDisplayed()
        compose.onNodeWithText("test-action").assertIsDisplayed()
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
}
