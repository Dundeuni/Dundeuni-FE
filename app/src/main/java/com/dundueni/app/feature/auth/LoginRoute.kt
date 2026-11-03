package com.dundueni.app.feature.auth

import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.withResumed

@Composable
fun LoginRoute(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onSignUp: () -> Unit,
    onResetPassword: () -> Unit,
    onKakaoLogin: () -> Unit,
    onGoogleLogin: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    if (onBack != null) BackHandler(onBack = onBack)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle, state.navigationPending) {
        if (state.navigationPending) lifecycle.withResumed {
            if (viewModel.uiState.value.navigationPending) {
                viewModel.consumeNavigation()
                onLoginSuccess()
            }
        }
    }
    LoginScreen(
        state, viewModel::changeEmail, viewModel::changePassword, viewModel::togglePasswordVisibility,
        onLogin = viewModel::login,
        onSignUp = onSignUp, onResetPassword = onResetPassword,
        onKakaoLogin = onKakaoLogin, onGoogleLogin = onGoogleLogin,
    )
}
