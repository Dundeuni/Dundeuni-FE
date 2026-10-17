package com.dundueni.app.feature.auth

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.WindowInsets as AndroidWindowInsets
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.view.WindowCompat
import com.dundueni.app.R
import com.dundueni.app.ui.theme.DundueniFETheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

/** 발표용 상태 렌더링. 실제 인증·메일·계정 API를 호출하지 않는다. */
@SdkSuppress(minSdkVersion = 30)
class AuthPresentationCaptureTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private data class Capture(val file: String, val title: String, val anchor: String)
    private val captures = listOf(
        Capture("01-login-empty", "로그인 · 입력 전", "로그인"),
        Capture("02-login-filled", "로그인 · 입력 후", "example@kau.kr"),
        Capture("03-login-error", "로그인 · 인증 오류", "이메일 또는 비밀번호가 일치하지 않습니다."),
        Capture("04-terms-initial", "약관 동의 · 입력 전", "약관 전체 동의하기"),
        Capture("05-terms-required", "약관 동의 · 필수 항목 선택", "서비스 이용약관 동의"),
        Capture("06-email-initial", "이메일 인증 · 입력 전", "인증번호 받기"),
        Capture("07-email-sent", "이메일 인증 · 발송 후", "03:00"),
        Capture("08-email-error", "이메일 인증 · 번호 불일치", "인증번호가 일치하지 않습니다."),
        Capture("09-email-expired", "이메일 인증 · 시간 만료", "인증 시간이 만료되었습니다. 새 번호를 받아 주세요."),
        Capture("10-credentials-valid", "가입 정보 · 입력 조건 충족", "비밀번호가 안전하게 일치합니다"),
        Capture("11-credentials-error", "가입 정보 · 입력 오류", "글자 제한수를 초과하였습니다."),
        Capture("12-signup-completed", "회원가입 · 완료 팝업", "회원가입이 완료되었습니다!"),
    )

    @Test fun capturePresentationStatesWithoutKeyboardOrClipboardSuggestions() {
        val selected = mutableIntStateOf(0)
        compose.runOnUiThread {
            WindowCompat.getInsetsController(compose.activity.window, compose.activity.window.decorView).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
        compose.setContent {
            DundueniFETheme {
                val keyboard = LocalSoftwareKeyboardController.current
                val focus = LocalFocusManager.current
                LaunchedEffect(selected.intValue) { focus.clearFocus(); keyboard?.hide() }
                Column(Modifier.fillMaxSize().background(Color(0xFFF8F9FF)).safeDrawingPadding()) {
                    // 바깥 프레임이 처리한 시스템 여백을 화면 안에서 중복 적용하지 않는다.
                    Box(Modifier.weight(1f).fillMaxWidth().consumeWindowInsets(WindowInsets.safeDrawing)) {
                        key(selected.intValue) { SampleScreen(selected.intValue) }
                    }
                    Column(Modifier.fillMaxWidth().background(Color(0xFF102649)).padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("UI 미리보기 · 샘플 인증", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily(Font(R.font.pretendard_medium)))
                        Text("${captures[selected.intValue].title} · 실제 API 미연결", color = Color(0xFFD9E5FA), fontSize = 10.sp,
                            fontFamily = FontFamily(Font(R.font.pretendard_medium)))
                    }
                }
            }
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "pb09-presentation")
        assertTrue(directory.mkdirs() || directory.isDirectory)
        captures.forEachIndexed { index, capture ->
            compose.runOnIdle { selected.intValue = index }
            compose.waitForIdle()
            compose.onNodeWithText(capture.anchor).assertIsDisplayed()
            // semantics 갱신과 SurfaceFlinger 반영 사이의 차이 및 Dialog 전환 애니메이션을 기다린다.
            instrumentation.waitForIdleSync()
            SystemClock.sleep(600)
            instrumentation.runOnMainSync {
                assertFalse("소프트 키보드가 표시돼 캡처를 중단합니다.",
                    compose.activity.window.decorView.rootWindowInsets.isVisible(AndroidWindowInsets.Type.ime()))
            }
            val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            FileOutputStream(File(directory, "${capture.file}.png")).use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
            bitmap.recycle()
        }
    }

    @Composable
    private fun SampleScreen(index: Int) {
        when (index) {
            in 0..2 -> LoginScreen(
                when (index) {
                    0 -> LoginUiState()
                    1 -> LoginUiState(email = "example@kau.kr", password = "ValidPass123!")
                    else -> LoginUiState(email = "example@kau.kr", password = "WrongPass123!", credentialsRejected = true,
                        message = "이메일 또는 비밀번호가 일치하지 않습니다.")
                }, {}, {}, {}, {}, {}, {}, {}, {})
            3, 4 -> SignUpTermsScreen(
                if (index == 3) TermsUiState() else TermsUiState(service = true, privacy = true, age = true),
                { _, _ -> }, {}, {}, {}, {})
            in 5..8 -> EmailVerificationScreen(
                when (index) {
                    5 -> EmailVerificationUiState()
                    6 -> EmailVerificationUiState(email = "example@kau.kr", hasSentCode = true, remainingSeconds = 180)
                    7 -> EmailVerificationUiState(email = "example@kau.kr", code = "654321", hasSentCode = true, remainingSeconds = 110,
                        codeError = "인증번호가 일치하지 않습니다.")
                    else -> EmailVerificationUiState(email = "example@kau.kr", hasSentCode = true, remainingSeconds = 0,
                        codeError = "인증 시간이 만료되었습니다. 새 번호를 받아 주세요.")
                }, {}, {}, {}, {}, {}, demo = true)
            else -> SignUpCredentialsScreen(
                when (index) {
                    9 -> SignUpCredentialsUiState(email = "example@kau.kr", password = "ValidPass123!", confirmation = "ValidPass123!", nickname = "든든한지킴이")
                    10 -> SignUpCredentialsUiState(email = "example@kau.kr", password = "ValidPass123!", confirmation = "WrongPass123!",
                        nickname = "든든한지킴이일까요아닐까요", passwordTouched = true, confirmationTouched = true, nicknameTouched = true)
                    else -> SignUpCredentialsUiState(email = "example@kau.kr", nickname = "든든한지킴이", completed = true)
                }, {}, {}, {}, {}, {}, {}, {}, {}, demo = true)
        }
    }
}
