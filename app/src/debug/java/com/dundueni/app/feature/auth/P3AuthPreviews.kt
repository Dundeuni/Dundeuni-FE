package com.dundueni.app.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dundueni.app.ui.theme.DundueniFETheme

@Composable
private fun P3LoginPreview(state: LoginUiState) {
    DundueniFETheme {
        LoginScreen(
            state = state, onEmailChange = {}, onPasswordChange = {},
            onTogglePassword = {}, onLogin = {}, onSignUp = {},
            onResetPassword = {}, onKakaoLogin = {}, onGoogleLogin = {},
        )
    }
}

@Preview(name = "P3 로그인 · 제출 중", widthDp = 390, heightDp = 854, showBackground = true)
@Composable
private fun LoginSubmittingPreview() = P3LoginPreview(
    LoginUiState(email = "preview@example.com", password = "Preview123!", submitting = true),
)

@Preview(name = "P3 로그인 · 서버 연결 오류", widthDp = 390, heightDp = 854, showBackground = true)
@Composable
private fun LoginConnectionErrorPreview() = P3LoginPreview(
    LoginUiState(
        email = "preview@example.com", password = "Preview123!",
        message = "연결하지 못했습니다. 다시 시도해 주세요.",
    ),
)

@Preview(name = "P3 비밀번호 재설정 · 이메일 인증 단계", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun ResetVerificationPreview() = DundueniFETheme {
    EmailVerificationScreen(
        state = EmailVerificationUiState(
            email = "preview@example.com", hasSentCode = true, remainingSeconds = 180,
        ),
        onEmailChange = {}, onCodeChange = {}, onSendCode = {},
        onCheckCode = {}, onBack = {}, passwordReset = true,
    )
}

@Composable
private fun P3PasswordResetPreview(state: PasswordResetUiState) {
    DundueniFETheme {
        PasswordResetScreen(
            state = state, onPasswordChange = {}, onConfirmationChange = {},
            onTogglePassword = {}, onSubmit = {}, onBack = {}, onLogin = {},
        )
    }
}

private fun resetFields() = SignUpCredentialsUiState(
    email = "preview@example.com", password = "Preview123!", confirmation = "Preview123!",
    policy = AuthInputPolicy.PREVIEW_RESET,
)

@Preview(name = "P3 비밀번호 재설정 · 변경 처리 중", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun ResetSubmittingPreview() = P3PasswordResetPreview(
    PasswordResetUiState(fields = resetFields(), settingPassword = true, submitting = true),
)

@Preview(name = "P3 비밀번호 재설정 · 재인증 필요", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun ResetNeedsVerificationPreview() = P3PasswordResetPreview(
    PasswordResetUiState(
        fields = SignUpCredentialsUiState(email = "preview@example.com"),
        settingPassword = true, needsVerification = true,
        message = "인증이 만료되었습니다. 이메일 인증을 다시 진행해 주세요.",
    ),
)

@Preview(name = "P3 비밀번호 재설정 · 변경 실패", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun ResetFailedPreview() = P3PasswordResetPreview(
    PasswordResetUiState(
        fields = resetFields(), settingPassword = true,
        message = "비밀번호를 변경하지 못했습니다. 다시 시도해 주세요.",
    ),
)

@Preview(name = "P3 비밀번호 재설정 · 변경 완료", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun ResetCompletedPreview() = P3PasswordResetPreview(
    PasswordResetUiState(
        fields = SignUpCredentialsUiState(email = "preview@example.com"),
        settingPassword = true, completed = true,
    ),
)
