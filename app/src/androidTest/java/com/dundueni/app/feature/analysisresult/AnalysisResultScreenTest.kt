package com.dundueni.app.feature.analysisresult

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.dundueni.app.ui.theme.DundueniFETheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class AnalysisResultScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun safeShowsEvidenceAndDispatchesSave() {
        var dispatched: ResultAction? = null
        compose.setContent { DundueniFETheme { AnalysisResultScreen(AnalysisResultSamples.forLevel(RiskLevel.SAFE), onAction = { dispatched = it }) } }
        compose.onNodeWithText("현재는 안전해 보여요").assertIsDisplayed()
        screenshot("pb05-safe")
        compose.onNodeWithText("비정상 발신자 및 사칭 패턴 없음").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("검사 결과 기록에 보관하기").performScrollTo().performClick()
        assertEquals(ResultAction.SAVE_HISTORY, dispatched)
    }

    @Test fun cautionShowsIndependentScoresAndDomainAction() {
        var dispatched: ResultAction? = null
        compose.setContent { DundueniFETheme { AnalysisResultScreen(AnalysisResultSamples.forLevel(RiskLevel.CAUTION), onAction = { dispatched = it }) } }
        compose.onNodeWithText("조금 더 확인해보세요").assertIsDisplayed()
        screenshot("pb05-caution")
        compose.onNodeWithText("분석 점수 64%").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("분석 점수 28%").assertIsDisplayed()
        compose.onNodeWithText("링크 열지 않고 도메인만 미리보기").performScrollTo().performClick()
        assertEquals(ResultAction.PREVIEW_DOMAIN, dispatched)
    }

    @Test fun dangerShowsUrgentWarningAndCallAction() {
        var dispatched: ResultAction? = null
        compose.setContent { DundueniFETheme { AnalysisResultScreen(AnalysisResultSamples.forLevel(RiskLevel.DANGER), onAction = { dispatched = it }) } }
        compose.onNodeWithText("위험할 가능성이 높아요").assertIsDisplayed()
        compose.onNodeWithText("지금은 절대 돈을 보내지 마세요!").assertIsDisplayed()
        screenshot("pb05-danger")
        compose.onNodeWithText("가족에게 직접 전화해 확인하기").performScrollTo().performClick()
        assertEquals(ResultAction.CALL_FAMILY, dispatched)
        compose.onNodeWithText("신고 (112/118)").assertIsEnabled()
    }

    @Test fun missingUrlDisablesPreviewAndEmptyEvidenceIsHonest() {
        compose.setContent { DundueniFETheme {
            AnalysisResultScreen(AnalysisResult("empty", RiskLevel.CAUTION, "추가 확인 필요"), onAction = {})
        } }
        compose.onNodeWithText("세부 근거가 제공되지 않았어요. 요약과 대응 행동을 확인해주세요.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("링크 열지 않고 도메인만 미리보기").performScrollTo().assertIsNotEnabled()
    }

    @Test fun unknownVerdictInJsonIsRejected() {
        val bad = """{"id":"test","riskLevel":"UNKNOWN","summary":"요약"}"""
        assertTrue(runCatching { AnalysisResultJsonAdapter.decode(bad) }.isFailure)
        val valid = """{"id":"test","riskLevel":"DANGER","summary":"요약","fraudScore":4}"""
        assertEquals(RiskLevel.DANGER, AnalysisResultJsonAdapter.decode(valid).riskLevel)
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val target = File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png")
        FileOutputStream(target).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
