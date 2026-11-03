package com.dundueni.app.feature.qa

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.dundueni.app.MainActivity
import com.dundueni.app.feature.analysisresult.*
import com.dundueni.app.feature.history.*
import com.dundueni.app.feature.phone.PhoneInputActivity
import com.dundueni.app.feature.textinput.TextInputActivity
import com.dundueni.app.feature.url.UrlInputActivity
import com.google.gson.Gson
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/** Local fixtures only; restores the previous history after every test. No server requests. */
class Pb18UiQaTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val preferences get() = context.getSharedPreferences("analysis_result_history", Context.MODE_PRIVATE)
    private var previous: String? = null

    @Before fun isolateHistory() {
        previous = preferences.getString("results", null)
        preferences.edit().putString("results", "[]").commit()
    }
    @After fun restoreHistory() {
        preferences.edit().apply {
            if (previous == null) remove("results") else putString("results", previous)
        }.commit()
    }
    private fun current(): Activity {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) return ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).single()
        var result: Activity? = null
        instrumentation.runOnMainSync {
            result = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).singleOrNull()
        }
        return requireNotNull(result)
    }
    private fun waitText(text: String) = compose.waitUntil(10_000) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
    private fun rotate(landscape: Boolean) {
        val activity = current()
        instrumentation.runOnMainSync {
            activity.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        compose.waitUntil(10_000) {
            current().resources.configuration.orientation == if (landscape) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
        }
        compose.waitForIdle()
    }
    private fun capture(name: String) {
        // Shell-owned output survives uninstalling the test APK; no app content is uploaded.
        val descriptor = instrumentation.uiAutomation.executeShellCommand("screencap -p /sdcard/pb18-after-fix-$name.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }
    private fun inputSurvivesRotation(activity: Class<out ComponentActivity>, title: String, value: String) {
        ActivityScenario.launch<ComponentActivity>(Intent(context, activity)).use { scenario ->
            waitText(title)
            rotate(false)
            compose.onNode(hasSetTextAction()).performTextReplacement(value)
            rotate(true)
            compose.onNode(hasSetTextAction()).assertTextContains(value)
            rotate(false)
            compose.onNode(hasSetTextAction()).assertTextContains(value)
            scenario.recreate()
            compose.onNode(hasSetTextAction()).assertTextContains(value)
            compose.onNodeWithText("검사하기").assertIsEnabled()
        }
    }
    @Test fun textInputSurvivesRotationAndRecreation() = inputSurvivesRotation(TextInputActivity::class.java, "텍스트 검사", "PB18 QA sample")
    @Test fun urlInputSurvivesRotationAndRecreation() = inputSurvivesRotation(UrlInputActivity::class.java, "링크(URL) 검사", "https://example.com")
    @Test fun phoneInputSurvivesRotationAndRecreation() = inputSurvivesRotation(PhoneInputActivity::class.java, "전화번호 검사", "01012345678")

    private fun openPhoneKeyboard() {
        waitText("전화번호 검사")
        rotate(true)
        compose.onNode(hasSetTextAction()).performTextReplacement("01012345678")
        compose.onNode(hasSetTextAction()).performClick()
        compose.waitUntil(10_000) {
            var visible = false
            instrumentation.runOnMainSync { visible = current().window.decorView.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == true }
            visible
        }
    }
    private fun assertReachableAboveKeyboard(label: String) {
        val node = compose.onNodeWithText(label).performScrollTo().assertIsDisplayed().assertIsEnabled()
        compose.waitUntil(5_000) {
            var keyboardTop = 0
            instrumentation.runOnMainSync {
                val view = current().window.decorView
                keyboardTop = view.height - view.rootWindowInsets.getInsets(WindowInsets.Type.ime()).bottom
            }
            val bounds = node.fetchSemanticsNode()
            bounds.boundsInWindow.bottom <= keyboardTop + 1 && bounds.boundsInWindow.height >= bounds.size.height - 1
        }
    }
    @Test fun landscapePhoneKeyboardAllowsInspect() {
        ActivityScenario.launch<PhoneInputActivity>(Intent(context, PhoneInputActivity::class.java)).use {
            openPhoneKeyboard()
            assertReachableAboveKeyboard("검사하기")
            capture("phone-landscape-inspect")
            compose.onNodeWithText("검사하기").performClick()
            waitText("Inspection Result")
            compose.onNodeWithText("디자인 미리보기 · 샘플 데이터").assertIsDisplayed()
            val activity = current()
            instrumentation.runOnMainSync { activity.finish() }
        }
    }
    @Test fun landscapePhoneKeyboardAllowsClose() {
        ActivityScenario.launch<PhoneInputActivity>(Intent(context, PhoneInputActivity::class.java)).use {
            openPhoneKeyboard()
            assertReachableAboveKeyboard("닫기")
            capture("phone-landscape-close")
            compose.onNodeWithText("닫기").performClick()
            instrumentation.waitForIdleSync()
        }
    }
    @Test fun savingResultFiltersByTypeAndRecentRecordReturnsHome() {
        HistoryStore(context).save(AnalysisResult("pb18-fixture-image", RiskLevel.SAFE, "QA fixture", historyType = HistoryType.IMAGE), 100)
        val result = AnalysisResult("pb18-fixture-text", RiskLevel.SAFE, "QA fixture", historyType = HistoryType.TEXT)
        ActivityScenario.launch<AnalysisResultActivity>(AnalysisResultActivity.createIntent(context, Gson().toJson(result))).use {
            waitText("Inspection Result")
            compose.onNodeWithText("검사 결과 기록에 보관하기").performScrollTo().performClick()
            waitText("검사 결과가 기록에 보관되었어요")
            assertEquals(HistoryType.TEXT, HistoryStore(context).read().first { it.id == result.id }.type)
        }
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use {
            waitText("최근 검사")
            compose.onAllNodesWithText("저장한 검사 결과")[0].performScrollTo().performClick()
            waitText("검사 기록 상세")
            compose.onNodeWithText("텍스트 검사").assertExists()
            capture("recent-record-detail")
            val detail = current() as ComponentActivity
            instrumentation.runOnMainSync { detail.onBackPressedDispatcher.onBackPressed() }
            waitText("최근 검사")
            assertTrue(current() is MainActivity)
            compose.onNodeWithText("전체 기록 보기 ›").performScrollTo().performClick()
            waitText("전체 2")
            compose.onNodeWithText("텍스트 1").performScrollTo().performClick()
            compose.onNodeWithText("텍스트 검사 ·", substring = true).assertExists()
            compose.onNodeWithText("이미지 검사 ·", substring = true).assertDoesNotExist()
            capture("history-text-filter")
            compose.onNodeWithText("이미지 1").performScrollTo().performClick()
            compose.onNodeWithText("이미지 검사 ·", substring = true).assertExists()
            compose.onNodeWithText("텍스트 검사 ·", substring = true).assertDoesNotExist()
            val history = current()
            instrumentation.runOnMainSync { history.finish() }
        }
    }
}
