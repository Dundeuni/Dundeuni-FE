package com.dundueni.app.feature.auth

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Uses only Debug reset fixtures; never sends email, changes credentials or logs the user in. */
class PasswordResetFlowQaTest {
    @get:Rule val compose = createAndroidComposeRule<LoginActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun click(text: String) {
        val node = compose.onNodeWithText(text)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
    }
    private fun codeEnabled() = runCatching {
        !compose.onNodeWithContentDescription("인증번호 6자리").fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)
    }.getOrDefault(false)
    private fun input(label: String, value: String) {
        compose.onNodeWithContentDescription(label).performScrollTo().performTextReplacement(value)
    }
    private fun waitFor(text: String) = compose.waitUntil(10_000) {
        runCatching { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
    }
    private fun current(): Activity {
        var activity: Activity? = null
        compose.waitUntil(10_000) {
            instrumentation.runOnMainSync {
                activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).singleOrNull()
            }
            activity != null
        }
        return requireNotNull(activity)
    }
    private fun rotate(landscape: Boolean) {
        val activity = current()
        instrumentation.runOnMainSync { activity.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        val expected = if (landscape) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
        compose.waitUntil(10_000) {
            runCatching { current().resources.configuration.orientation == expected }.getOrDefault(false)
        }
        compose.waitForIdle()
    }
    private fun back() {
        val activity = current() as ComponentActivity
        instrumentation.runOnMainSync { activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }
    private fun capture(name: String) {
        if (!name.contains("keyboard")) {
            val activity = current()
            instrumentation.runOnMainSync { activity.window.insetsController?.hide(WindowInsets.Type.ime()) }
            compose.waitForIdle()
            android.os.SystemClock.sleep(400)
            if (name.startsWith("password")) compose.onNodeWithText("이메일 인증 완료").performScrollTo()
        }
        compose.waitForIdle()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(800)
        val descriptor = instrumentation.uiAutomation.executeShellCommand("screencap -p /sdcard/pb09-sprint3-$name.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }
    private fun enterReset() {
        click("비밀번호 재설정")
        waitFor("가입한 이메일로\n본인 인증을 진행해 주세요")
        assertTrue(current() is PasswordResetActivity)
    }
    private fun verify() {
        input("이메일 주소", "example@kau.kr"); click("인증번호 받기")
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("인증번호 6자리").fetchSemanticsNodes().any { it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.EditableText) && !it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) } }
        input("인증번호 6자리", "123456"); click("다음 단계로")
        waitFor("새로운 비밀번호를\n설정해 주세요")
    }
    @Test fun invalidEmailAndPasswordMismatchThenCompletionReturnToLogin() {
        val previous = com.dundueni.app.data.auth.AuthSessionRuntime.store(compose.activity.applicationContext).read()
        enterReset(); capture("email-initial")
        input("이메일 주소", "example"); click("인증번호 받기")
        compose.onNodeWithText("다음 단계로").assertIsNotEnabled()
        verify()
        input("새 비밀번호", "ValidPass123!"); input("새 비밀번호 확인", "Mismatch123!")
        compose.onNodeWithText("비밀번호가 일치하지 않습니다.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("비밀번호 변경 완료").assertIsNotEnabled()
        capture("password-error")
        input("새 비밀번호 확인", "ValidPass123!")
        capture("password-valid")
        click("비밀번호 변경 완료")
        waitFor("샘플 변경 확인 완료"); capture("completed")
        click("로그인으로 돌아가기")
        waitFor("회원가입")
        compose.onNodeWithText("회원가입").performScrollTo().assertIsDisplayed()
        assertTrue(current() is LoginActivity)
        assertEquals(previous, com.dundueni.app.data.auth.AuthSessionRuntime.store(compose.activity.applicationContext).read())
    }
    @Test fun wrongCodeCanBeCorrectedAndBackRequiresFreshVerification() {
        enterReset()
        input("이메일 주소", "example@kau.kr"); click("인증번호 받기")
        compose.waitUntil(10_000) { codeEnabled() }
        input("인증번호 6자리", "654321"); click("다음 단계로")
        waitFor("인증번호가 일치하지 않습니다."); capture("email-error")
        click("새 번호 발송")
        compose.waitUntil(10_000) { codeEnabled() }
        compose.onNodeWithContentDescription("인증번호 6자리").assertTextEquals("")
        input("인증번호 6자리", "123456"); click("다음 단계로")
        waitFor("새로운 비밀번호를\n설정해 주세요")
        input("새 비밀번호", "ValidPass123!")
        back()
        waitFor("가입한 이메일로\n본인 인증을 진행해 주세요")
        compose.onNodeWithText("다음 단계로").assertIsNotEnabled()
        assertEquals("", compose.onNodeWithContentDescription("이메일 주소").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text)
        back(); waitFor("회원가입"); assertTrue(current() is LoginActivity)
    }
    @Test fun rotationAndKeyboardPreserveResetAndKeepSubmitAboveKeyboard() {
        enterReset(); verify()
        input("새 비밀번호", "ValidPass123!")
        rotate(true)
        compose.onNodeWithContentDescription("새 비밀번호").assertTextContains("ValidPass123!")
        rotate(false)
        input("새 비밀번호 확인", "ValidPass123!")
        compose.onNodeWithContentDescription("새 비밀번호 확인").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            var shown = false
            val activity = current()
            instrumentation.runOnMainSync { shown = activity.window.decorView.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == true }
            shown
        }
        val button = compose.onNodeWithText("비밀번호 변경 완료").assertIsDisplayed().assertIsEnabled()
        var keyboardTop = 0
        val activity = current()
        compose.waitUntil(5_000) {
            instrumentation.runOnMainSync {
                val view = activity.window.decorView
                keyboardTop = view.height - view.rootWindowInsets.getInsets(WindowInsets.Type.ime()).bottom
            }
            button.fetchSemanticsNode().boundsInWindow.bottom <= keyboardTop + 1
        }
        capture("password-keyboard")
        button.performClick(); waitFor("샘플 변경 확인 완료")
        click("로그인으로 돌아가기")
    }
}
