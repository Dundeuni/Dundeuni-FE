package com.dundueni.app.feature.auth

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowInsets
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.filters.SdkSuppress
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.dundueni.app.MainActivity
import com.dundueni.app.data.auth.AuthSession
import com.dundueni.app.data.auth.AuthSessionRuntime
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

/** 실제 Activity 연결·회전 검증. debug 샘플만 사용하며 서버에 요청하지 않는다. */
@SdkSuppress(minSdkVersion = 30)
class AuthFlowQaTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val authIsolation = object : ExternalResource() {
        private var previous: AuthSession? = null
        private val store get() = AuthSessionRuntime.store(InstrumentationRegistry.getInstrumentation().targetContext)
        override fun before() { previous = store.read(); store.clear() }
        override fun after() { store.clear(); previous?.takeIf { it.isValid() }?.let(store::save) }
    }
    @get:Rule val rules = RuleChain.outerRule(authIsolation).around(compose)

    private fun click(text: String) {
        val node = compose.onNodeWithText(text)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
    }
    private fun input(label: String, value: String) = compose.onNodeWithContentDescription(label)
        .performScrollTo().performTextReplacement(value)

    private fun waitForText(text: String) {
        compose.waitUntil(10_000) { runCatching { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false) }
    }

    private fun currentActivity(): Activity {
        var activity: Activity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).single()
        }
        return requireNotNull(activity)
    }

    private fun back() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { currentResumed().onBackPressedDispatcherOrFinish() }
        compose.waitForIdle()
    }

    private fun currentResumed(): Activity = ActivityLifecycleMonitorRegistry.getInstance()
        .getActivitiesInStage(Stage.RESUMED).single()

    private fun Activity.onBackPressedDispatcherOrFinish() {
        (this as androidx.activity.ComponentActivity).onBackPressedDispatcher.onBackPressed()
    }

    private fun rotate(orientation: Int) {
        val previous = currentActivity()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { previous.requestedOrientation = orientation }
        val expected = if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
        compose.waitUntil(10_000) {
            var ready = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                ready = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .firstOrNull()?.resources?.configuration?.orientation == expected
            }
            ready
        }
        compose.waitForIdle()
    }

    private fun openKeyboard(label: String) {
        val field = compose.onNodeWithContentDescription(label)
        field.performScrollTo().performClick()
        compose.waitUntil(10_000) {
            var visible = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                visible = currentResumed().window.decorView.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == true
            }
            visible
        }
        compose.waitForIdle()
    }

    private fun assertAboveKeyboard(node: SemanticsNodeInteraction) {
        var keyboardTop = 0
        // IME 표시·회전 애니메이션은 Compose의 idle 신호 뒤에도 진행될 수 있다.
        compose.waitUntil(5_000) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val view = currentResumed().window.decorView
                keyboardTop = view.height - view.rootWindowInsets.getInsets(WindowInsets.Type.ime()).bottom
            }
            val semantics = node.fetchSemanticsNode()
            semantics.boundsInWindow.height >= semantics.size.height - 1 &&
                semantics.boundsInWindow.bottom <= keyboardTop + 1
        }
        node.assertIsDisplayed()
        val semantics = node.fetchSemanticsNode()
        assertTrue("Node clipped: ${semantics.boundsInWindow}, size=${semantics.size}",
            semantics.boundsInWindow.height >= semantics.size.height - 1)
        assertTrue("Node behind keyboard: ${semantics.boundsInWindow}, IME top=$keyboardTop",
            semantics.boundsInWindow.bottom <= keyboardTop + 1)
    }

    private fun openEmail() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { compose.activity.startActivity(android.content.Intent().setClassName(compose.activity, "com.dundueni.app.feature.auth.LoginPreviewActivity")) }
        waitForText("입력 전")
        click("입력 전")
        click("회원가입")
        waitForText("약관 동의")
        click("서비스 이용약관 동의")
        click("개인정보 수집 동의")
        click("연령 확인 및 보호자 동의")
        click("다음 단계로")
        waitForText("본인 인증")
    }

    @Test fun homeAccountSignupUsesDebugGatewayAndCompletes() {
        waitForText("빠른 검사")
        click("My 쉼터")
        waitForText("회원가입")
        click("회원가입")
        waitForText("약관 동의")
        click("서비스 이용약관 동의")
        click("개인정보 수집 동의")
        click("연령 확인 및 보호자 동의")
        click("다음 단계로")
        waitForText("본인 인증")
        compose.onNodeWithText("UI 테스트 · 실제 발송 없음 · 샘플 번호 123456").assertExists()
        sendCode()
        input("인증번호 6자리", "123456")
        click("다음 단계로")
        waitForText("닉네임 / 비밀번호 설정")
        input("비밀번호", "ValidPass123!")
        input("비밀번호 확인", "ValidPass123!")
        input("닉네임 설정", "tester")
        click("회원가입 완료하기")
        waitForText("회원가입이 완료되었습니다!")
        compose.onNodeWithText("UI 테스트 · 실제 가입·보호 활성화 없음").performScrollTo().assertIsDisplayed()
        click("든든이 시작하기 (홈으로)")
        waitForText("빠른 검사")
        assertTrue(AuthSessionRuntime.store(InstrumentationRegistry.getInstrumentation().targetContext).read() == null)
    }

    private fun sendCode() {
        input("이메일 주소", "qa@kau.kr")
        click("인증번호 받기")
        waitForText("새 번호 발송")
        compose.waitUntil(10_000) {
            !compose.onNodeWithContentDescription("인증번호 6자리").fetchSemanticsNode().config
                .contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)
        }
    }

    @Test fun signupErrorsResendCorrectionCompletionAndHome() {
        openEmail()
        click("인증번호 받기")
        compose.onNodeWithText("이메일을 입력해 주세요.").performScrollTo().assertIsDisplayed()
        sendCode()
        input("인증번호 6자리", "654321")
        click("다음 단계로")
        waitForText("인증번호가 일치하지 않습니다.")
        compose.onNodeWithText("다음 단계로").assertIsNotEnabled()
        click("새 번호 발송")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("인증번호가 일치하지 않습니다.").fetchSemanticsNodes().isEmpty() }
        // The error clears before the asynchronous resend completes; wait for editable input.
        compose.waitUntil(10_000) {
            !compose.onNodeWithContentDescription("인증번호 6자리").fetchSemanticsNode().config
                .contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)
        }
        input("인증번호 6자리", "123456")
        click("다음 단계로")
        waitForText("닉네임 / 비밀번호 설정")
        input("비밀번호", "ValidPass123!")
        input("비밀번호 확인", "WrongPass123!")
        input("닉네임 설정", "abcdefghijk")
        compose.onNodeWithText("비밀번호가 일치하지 않습니다.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("글자 제한수를 초과하였습니다.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("회원가입 완료하기").assertIsNotEnabled()
        input("비밀번호 확인", "ValidPass123!")
        input("닉네임 설정", "tester")
        click("회원가입 완료하기")
        waitForText("회원가입이 완료되었습니다!")
        compose.onNodeWithText("tester 님, 환영합니다!").assertIsDisplayed()
        compose.onNodeWithText("qa@kau.kr").assertIsDisplayed()
        click("든든이 시작하기 (홈으로)")
        waitForText("빠른 검사")
    }

    @Test fun verificationSurvivesRotationAndBackDoesNotReopenCredentials() {
        openEmail()
        sendCode()
        input("인증번호 6자리", "123456")
        compose.onNodeWithContentDescription("인증번호 6자리").assertTextEquals("123456")
        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        compose.onNodeWithContentDescription("이메일 주소").assertTextEquals("qa@kau.kr")
        compose.onNodeWithContentDescription("인증번호 6자리").assertTextEquals("123456")
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        click("다음 단계로")
        waitForText("닉네임 / 비밀번호 설정")
        input("비밀번호", "ValidPass123!")
        input("비밀번호 확인", "ValidPass123!")
        input("닉네임 설정", "tester")
        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        compose.onNodeWithContentDescription("닉네임 설정").assertTextEquals("tester")
        compose.onNodeWithText("회원가입 완료하기").assertIsEnabled()
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        back()
        waitForText("본인 인증")
        compose.onNodeWithText("이메일 인증을 완료했습니다.").performScrollTo().assertIsDisplayed()
        back()
        waitForText("약관 동의")
        compose.onNodeWithContentDescription("서비스 이용약관 동의").assertIsOn()
        compose.onNodeWithContentDescription("개인정보 수집 동의").assertIsOn()
        compose.onNodeWithContentDescription("연령 확인 및 보호자 동의").assertIsOn()
    }

    @Test fun loginKeyboardNextDoneAndButtonRemainAccessible() {
        click("My 쉼터")
        waitForText("회원가입")
        input("이메일 계정", "qa@kau.kr")
        openKeyboard("이메일 계정")
        assertAboveKeyboard(compose.onNodeWithContentDescription("이메일 계정"))
        assertAboveKeyboard(compose.onNodeWithText("로그인"))
        compose.onNodeWithContentDescription("이메일 계정").performImeAction()
        compose.onNodeWithContentDescription("비밀번호").assertIsFocused()
        input("비밀번호", "ValidPass123!")
        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        openKeyboard("비밀번호")
        assertAboveKeyboard(compose.onNodeWithContentDescription("비밀번호"))
        assertAboveKeyboard(compose.onNodeWithText("로그인"))
        compose.onNodeWithContentDescription("비밀번호").performImeAction()
        waitForText("빠른 검사")
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }

    @Test fun signupKeyboardDoesNotCoverFieldsOrFooterAndDoneSubmits() {
        openEmail()
        input("이메일 주소", "qa@kau.kr")
        openKeyboard("이메일 주소")
        assertAboveKeyboard(compose.onNodeWithContentDescription("이메일 주소"))
        assertAboveKeyboard(compose.onNodeWithText("다음 단계로"))
        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        openKeyboard("이메일 주소")
        assertAboveKeyboard(compose.onNodeWithContentDescription("이메일 주소"))
        assertAboveKeyboard(compose.onNodeWithText("다음 단계로"))
        // 발송 전 이메일 Done은 번호 발송을 요청한다.
        compose.onNodeWithContentDescription("이메일 주소").performImeAction()
        waitForText("새 번호 발송")
        input("인증번호 6자리", "123456")
        openKeyboard("인증번호 6자리")
        assertAboveKeyboard(compose.onNodeWithContentDescription("인증번호 6자리"))
        assertAboveKeyboard(compose.onNodeWithText("다음 단계로"))
        compose.onNodeWithContentDescription("인증번호 6자리").performImeAction()
        waitForText("닉네임 / 비밀번호 설정")
        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        input("비밀번호", "ValidPass123!")
        openKeyboard("비밀번호")
        assertAboveKeyboard(compose.onNodeWithContentDescription("비밀번호"))
        assertAboveKeyboard(compose.onNodeWithText("회원가입 완료하기"))
        compose.onNodeWithContentDescription("비밀번호").performImeAction()
        compose.onNodeWithContentDescription("비밀번호 확인").assertIsFocused()
        input("비밀번호 확인", "ValidPass123!")
        compose.onNodeWithContentDescription("비밀번호 확인").performImeAction()
        compose.onNodeWithContentDescription("닉네임 설정").assertIsFocused()
        input("닉네임 설정", "tester")
        openKeyboard("닉네임 설정")
        assertAboveKeyboard(compose.onNodeWithContentDescription("닉네임 설정"))
        assertAboveKeyboard(compose.onNodeWithText("회원가입 완료하기"))
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        openKeyboard("닉네임 설정")
        assertAboveKeyboard(compose.onNodeWithContentDescription("닉네임 설정"))
        assertAboveKeyboard(compose.onNodeWithText("회원가입 완료하기"))
        compose.onNodeWithContentDescription("닉네임 설정").performImeAction()
        waitForText("회원가입이 완료되었습니다!")
        click("든든이 시작하기 (홈으로)")
        waitForText("빠른 검사")
    }
}
