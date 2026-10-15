package com.dundueni.app.feature.auth

import com.dundueni.app.data.auth.AuthSession

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginRoute(
    viewModel: LoginViewModel,
    onLoginSuccess: (AuthSession) -> Unit,
    onSignUp: () -> Unit,
    onResetPassword: () -> Unit,
    onKakaoLogin: () -> Unit,
    onGoogleLogin: () -> Unit,
    onBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoginScreen(
        state, viewModel::changeEmail, viewModel::changePassword, viewModel::togglePasswordVisibility,
        onLogin = { viewModel.login(onLoginSuccess) },
        onBack = onBack,
        onSignUp = onSignUp, onResetPassword = onResetPassword,
        onKakaoLogin = onKakaoLogin, onGoogleLogin = onGoogleLogin,
    )
}
