package com.dundueni.app.feature.auth

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dundueni.app.ui.theme.DundueniFETheme
import com.dundueni.app.MainActivity
import com.dundueni.app.data.auth.AuthSessionRuntime
import com.dundueni.app.data.auth.AuthDependencies

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val submitter = AuthDependencies.loginSubmitter()
        val viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == LoginViewModel::class.java)
                @Suppress("UNCHECKED_CAST")
                return LoginViewModel(submitter, saveSession = AuthSessionRuntime.store(applicationContext)::save) as T
            }
        })[LoginViewModel::class.java]
        setContent {
            DundueniFETheme {
                LoginRoute(
                    viewModel = viewModel,
                    onBack = ::finish,
                    onLoginSuccess = {
                        startActivity(Intent(this, MainActivity::class.java).addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
                        ))
                    },
                    onSignUp = {
                        startActivity(Intent(this, SignUpTermsActivity::class.java).apply {
                            putExtra(EmailVerificationActivity.EXTRA_DEMO, AuthDependencies.defaultDebugMock)
                        })
                    },
                    onResetPassword = { startActivity(Intent(this, PasswordResetActivity::class.java)) },
                    onKakaoLogin = { pending("카카오 로그인") },
                    onGoogleLogin = { pending("Google 로그인") },
                )
            }
        }
    }

    private fun pending(feature: String) {
        Toast.makeText(this, "$feature 기능을 준비 중입니다.", Toast.LENGTH_SHORT).show()
    }
}
