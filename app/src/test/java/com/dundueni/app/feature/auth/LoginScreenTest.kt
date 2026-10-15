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
class LoginScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun passwordToggleAndInputEventsWork() {
        val state = mutableStateOf(LoginUiState())
        compose.setContent { DundueniFETheme {
            LoginScreen(state.value,
                { state.value = state.value.copy(email = it) },
                { state.value = state.value.copy(password = it) },
                { state.value = state.value.copy(passwordVisible = !state.value.passwordVisible) },
                {}, {}, {}, {}, {})
        } }
        compose.onNodeWithContentDescription("이메일 계정").performScrollTo().performTextInput("example@kau.kr")
        compose.onNodeWithContentDescription("비밀번호").performScrollTo().performTextInput("test-password")
        compose.onNodeWithContentDescription("비밀번호 보기").performScrollTo().performClick()
        compose.onNodeWithContentDescription("비밀번호 숨기기").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals("example@kau.kr", state.value.email)
            assertEquals("test-password", state.value.password)
            assertTrue(state.value.passwordVisible)
        }
    }

    @Test fun loadingBlocksLoginAndSecondaryActions() {
        compose.setContent { DundueniFETheme {
            LoginScreen(LoginUiState(submitting = true), {}, {}, {}, {}, {}, {}, {}, {})
        } }
        // 키보드가 열렸을 때 고정 버튼은 스크롤 영역 밖에 있다.
        compose.onNodeWithText("로그인 중").assertIsNotEnabled()
        compose.onNodeWithText("회원가입").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("카카오로 시작하기").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("비밀번호 보기").performScrollTo().assertIsNotEnabled()
    }

    @Test fun serverFailureIsShownAndUtilityActionsAreForwarded() {
        var signup = 0
        var reset = 0
        compose.setContent { DundueniFETheme {
            LoginScreen(LoginUiState(credentialsRejected = true, message = "이메일 또는 비밀번호가 일치하지 않습니다."),
                {}, {}, {}, {}, { signup++ }, { reset++ }, {}, {})
        } }
        compose.onNodeWithText("이메일 또는 비밀번호가 일치하지 않습니다.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("회원가입").performScrollTo().performClick()
        compose.onNodeWithText("비밀번호 재설정").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, signup); assertEquals(1, reset) }
    }
}
