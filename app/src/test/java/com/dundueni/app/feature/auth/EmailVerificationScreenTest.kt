package com.dundueni.app.feature.auth

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
class EmailVerificationScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyStateShowsSendActionAndBlocksCodeAndNext() {
        var sends = 0
        compose.setContent { DundueniFETheme {
            EmailVerificationScreen(EmailVerificationUiState(), {}, {}, { sends++ }, {}, {})
        } }
        compose.onNodeWithText("다음 단계로").assertIsNotEnabled()
        compose.onNodeWithContentDescription("인증번호 6자리").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("인증번호 받기").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, sends) }
    }

    @Test fun sentStateShowsCountdownAndResendAndAllowsSixDigitSubmission() {
        var clears = 0
        var checks = 0
        var resends = 0
        compose.setContent { DundueniFETheme {
            EmailVerificationScreen(EmailVerificationUiState(email = "example@kau.kr", code = "123456", hasSentCode = true, remainingSeconds = 110),
                {}, { if (it.isEmpty()) clears++ }, { resends++ }, { checks++ }, {})
        } }
        compose.onNodeWithText("인증번호 받기").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("01:50").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("인증번호 6자리 지우기").performScrollTo().performClick()
        compose.onNodeWithText("새 번호 발송").performScrollTo().performClick()
        compose.onNodeWithText("다음 단계로").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, clears); assertEquals(1, resends); assertEquals(1, checks) }
    }

    @Test fun mismatchBlocksNextUntilEditedAndDisplaysError() {
        compose.setContent { DundueniFETheme {
            EmailVerificationScreen(EmailVerificationUiState(code = "654321", hasSentCode = true, remainingSeconds = 110,
                codeError = "인증번호가 일치하지 않습니다."), {}, {}, {}, {}, {})
        } }
        compose.onNodeWithText("다음 단계로").assertIsNotEnabled()
        compose.onNodeWithText("인증번호가 일치하지 않습니다.").performScrollTo().assertIsDisplayed()
    }
}
