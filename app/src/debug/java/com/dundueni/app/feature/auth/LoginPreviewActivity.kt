package com.dundueni.app.feature.auth

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dundueni.app.ui.theme.DundueniFETheme
import kotlinx.coroutines.delay

/** 샘플 상태는 debug에만 포함한다. 실제 계정을 사용하지 않는다. */
class LoginPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DundueniFETheme {
                var selected by rememberSaveable { mutableStateOf<Int?>(null) }
                BackHandler(selected != null) { selected = null }
                if (selected == null) {
                    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
                        Text("PB-09 로그인 UI 미리보기 · 실제 인증 없음")
                        listOf("입력 전", "입력 후", "로그인 오류").forEachIndexed { index, name ->
                            Button(onClick = { selected = index }) { Text(name) }
                        }
                        Text("이메일 형식이 맞는 입력을 제출하면 0.8초 뒤 샘플 오류를 표시합니다.")
                    }
                } else {
                    val scenario = selected!!
                    val viewModel = ViewModelProvider(this@LoginPreviewActivity, object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return LoginViewModel(LoginSubmitter { _, _ ->
                                delay(800)
                                LoginOutcome.InvalidCredentials
                            }, sampleState(scenario)) as T
                        }
                    })["login-preview-$scenario", LoginViewModel::class.java]
                    val pending = { Toast.makeText(this@LoginPreviewActivity, "미리보기에서는 화면 이동을 실행하지 않습니다.", Toast.LENGTH_SHORT).show(); Unit }
                    LoginRoute(viewModel, { selected = null },
                        { startActivity(Intent(this@LoginPreviewActivity, SignUpTermsActivity::class.java).apply {
                            putExtra(EmailVerificationActivity.EXTRA_DEMO, true)
                        }) },
                        pending, pending, pending)
                }
            }
        }
    }
}

private fun sampleState(scenario: Int): LoginUiState = when (scenario) {
    1 -> LoginUiState(email = "example@kau.kr", password = "ValidPass123!", passwordVisible = true)
    2 -> LoginUiState(email = "example", password = "ValidPass123!", credentialsRejected = true,
        message = "이메일 또는 비밀번호가 일치하지 않습니다.")
    else -> LoginUiState()
}

@Preview(widthDp = 390, heightDp = 854, showBackground = true)
@Composable
private fun LoginEmptyPreview() = PreviewScreen(0)

@Preview(widthDp = 390, heightDp = 854, showBackground = true)
@Composable
private fun LoginFilledPreview() = PreviewScreen(1)

@Preview(widthDp = 390, heightDp = 854, showBackground = true)
@Composable
private fun LoginErrorPreview() = PreviewScreen(2)

@Composable
private fun PreviewScreen(scenario: Int) {
    DundueniFETheme { LoginScreen(sampleState(scenario), {}, {}, {}, {}, {}, {}, {}, {}) }
}
