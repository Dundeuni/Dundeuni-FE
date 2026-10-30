package com.dundueni.app.feature.auth

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.dundueni.app.ui.theme.DundueniFETheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SignUpCredentialsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun mismatchAndOverLimitShowErrorsAndDisableCompletion() {
        compose.setContent { DundueniFETheme {
            SignUpCredentialsScreen(SignUpCredentialsUiState(password = "ValidPass123", confirmation = "ValidPass123!",
                nickname = "가".repeat(13), confirmationTouched = true, nicknameTouched = true), {}, {}, {}, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithText("회원가입 완료하기").assertIsNotEnabled()
        compose.onNodeWithText("비밀번호가 일치하지 않습니다.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("13 / 10자").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("글자 제한수를 초과하였습니다.").performScrollTo().assertIsDisplayed()
    }

    @Test fun matchingValuesEnableCompletionAndVisibilityCanToggle() {
        val state = mutableStateOf(SignUpCredentialsUiState(password = "ValidPass123!", confirmation = "ValidPass123!", nickname = "tester"))
        var submissions = 0
        compose.setContent { DundueniFETheme {
            SignUpCredentialsScreen(state.value, {}, {}, {}, { state.value = state.value.copy(passwordVisible = !state.value.passwordVisible) },
                { submissions++ }, {}, {}, {})
        } }
        compose.onNodeWithContentDescription("비밀번호 보기").performScrollTo().performClick()
        compose.onNodeWithContentDescription("비밀번호 숨기기").assertIsDisplayed()
        compose.onNodeWithText("회원가입 완료하기").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, submissions) }
    }

    @Test fun completionDialogUsesSubmittedIdentityAndForwardsHomeAction() {
        var home = 0
        compose.setContent { DundueniFETheme {
            SignUpCredentialsScreen(SignUpCredentialsUiState(email = "example@kau.kr", nickname = "tester", completed = true),
                {}, {}, {}, {}, {}, {}, { home++ }, {}, demo = true)
        } }
        compose.onNodeWithText("tester 님, 환영합니다!").assertIsDisplayed()
        compose.onNodeWithText("example@kau.kr").assertIsDisplayed()
        compose.onNodeWithText("UI 테스트 · 실제 가입·보호 활성화 없음").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("든든이 시작하기 (홈으로)").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, home) }
    }
}
