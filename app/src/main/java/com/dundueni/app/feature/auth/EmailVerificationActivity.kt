package com.dundueni.app.feature.auth

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.withResumed
import com.dundueni.app.BuildConfig
import com.dundueni.app.ui.theme.DundueniFETheme

class EmailVerificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val demo = BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_DEMO, false)
        val gateway = if (demo) {
            Class.forName("com.dundueni.app.feature.auth.PreviewEmailVerificationGateway")
                .getDeclaredConstructor().newInstance() as EmailVerificationGateway
        } else UnconnectedEmailVerificationGateway
        val viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == EmailVerificationViewModel::class.java)
                @Suppress("UNCHECKED_CAST")
                return EmailVerificationViewModel(gateway) as T
            }
        })[EmailVerificationViewModel::class.java]
        setContent {
            DundueniFETheme {
                DisposableEffect(viewModel) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshTimer()
                    }
                    lifecycle.addObserver(observer)
                    onDispose { lifecycle.removeObserver(observer) }
                }
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(state.navigationPending) {
                    if (state.navigationPending) lifecycle.withResumed {
                        // 회전·백그라운드 복귀 후 현재 Activity에서 한 번만 이동한다.
                        if (viewModel.uiState.value.navigationPending) {
                            startActivity(Intent(this@EmailVerificationActivity, SignUpCredentialsActivity::class.java).apply {
                                putExtra(SignUpCredentialsActivity.EXTRA_EMAIL, viewModel.uiState.value.email)
                                putExtra(EXTRA_DEMO, demo)
                                putExtra("marketing_agreed", intent.getBooleanExtra("marketing_agreed", false))
                                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            })
                            viewModel.consumeNavigation()
                        }
                    }
                }
                EmailVerificationScreen(state, viewModel::changeEmail, viewModel::changeCode,
                    viewModel::sendCode, { viewModel.checkCode {} }, ::finish, demo)
            }
        }
    }

    companion object { const val EXTRA_DEMO = "auth_ui_demo" }
}

private object UnconnectedEmailVerificationGateway : EmailVerificationGateway {
    override suspend fun sendCode(email: String) = SendCodeOutcome.Unavailable
    override suspend fun verifyCode(challengeId: String, code: String) = VerifyCodeOutcome.Unavailable
}
