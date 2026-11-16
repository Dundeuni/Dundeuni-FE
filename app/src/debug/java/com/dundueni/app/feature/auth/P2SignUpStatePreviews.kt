package com.dundueni.app.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dundueni.app.ui.theme.DundueniFETheme

private val previewTerms = listOf(
    SignUpTermsDocument(
        id = "preview-service", type = SignUpTerm.SERVICE, version = "preview",
        title = "서비스 이용약관 동의", content = "서비스 이용약관 샘플", required = true,
    ),
    SignUpTermsDocument(
        id = "preview-privacy", type = SignUpTerm.PRIVACY, version = "preview",
        title = "개인정보 수집 동의", content = "개인정보 수집 약관 샘플", required = true,
    ),
)

@Composable
private fun TermsPreview(loadState: SignUpTermsLoadState) {
    DundueniFETheme {
        SignUpTermsScreen(
            state = loadState.selection,
            onTermChange = { _, _ -> }, onAllChange = {}, onShowTerm = {},
            onBack = {}, onNext = {}, documents = loadState.terms,
            ready = loadState.ready && !loadState.loading,
            message = if (loadState.loading) "약관을 불러오고 있습니다." else loadState.message,
            onRetry = if (loadState.loading) null else ({}),
        )
    }
}

@Preview(name = "P2 회원가입 약관 · 조회 중", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun TermsLoadingPreview() = TermsPreview(SignUpTermsLoadState(loading = true))

@Preview(name = "P2 회원가입 약관 · 조회 실패", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun TermsLoadErrorPreview() = TermsPreview(
    SignUpTermsLoadState(message = AuthFailure.NETWORK.message),
)

@Preview(name = "P2 회원가입 약관 · 조회 성공", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun TermsLoadedPreview() = TermsPreview(SignUpTermsLoadState(terms = previewTerms))

@Composable
private fun VerificationPreview(state: EmailVerificationUiState) {
    DundueniFETheme {
        EmailVerificationScreen(
            state = state, onEmailChange = {}, onCodeChange = {},
            onSendCode = {}, onCheckCode = {}, onBack = {},
        )
    }
}

private fun sentCodeState() = EmailVerificationUiState(
    email = "preview@example.com", hasSentCode = true, remainingSeconds = 180,
)

@Preview(name = "P2 이메일 인증 · 번호 발송 중", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun VerificationSendingPreview() = VerificationPreview(
    EmailVerificationUiState(email = "preview@example.com", busy = true),
)

@Preview(name = "P2 이메일 인증 · 번호 발송 성공", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun VerificationSentPreview() = VerificationPreview(sentCodeState())

@Preview(name = "P2 이메일 인증 · 번호 확인 중", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun VerificationCheckingPreview() = VerificationPreview(
    sentCodeState().copy(code = "123456", busy = true),
)

@Preview(name = "P2 이메일 인증 · 번호 불일치", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun VerificationMismatchPreview() = VerificationPreview(
    sentCodeState().copy(code = "123456", codeError = "인증번호가 일치하지 않습니다."),
)

@Preview(name = "P2 이메일 인증 · 번호 만료", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun VerificationExpiredPreview() = VerificationPreview(
    sentCodeState().copy(
        remainingSeconds = 0,
        codeError = "인증 시간이 만료되었습니다. 새 번호를 받아 주세요.",
    ),
)

@Preview(name = "P2 이메일 인증 · 재전송 제한", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun VerificationResendLimitPreview() = VerificationPreview(
    sentCodeState().copy(resendRemainingSeconds = 30),
)

@Preview(name = "P2 이메일 인증 · 완료", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun VerificationCompletedPreview() = VerificationPreview(
    sentCodeState().copy(
        remainingSeconds = 0, verified = true, navigationPending = true,
        notice = "이메일 인증을 완료했습니다.",
    ),
)

@Composable
private fun CredentialsPreview(state: SignUpCredentialsUiState) {
    DundueniFETheme {
        SignUpCredentialsScreen(
            state = state,
            onPasswordChange = {}, onConfirmationChange = {}, onNicknameChange = {},
            onTogglePassword = {}, onSubmit = {}, onBack = {}, onHome = {}, onProfile = {},
        )
    }
}

private fun validCredentialsState() = SignUpCredentialsUiState(
    email = "preview@example.com", password = "Preview123!",
    confirmation = "Preview123!", nickname = "든든이",
    policy = AuthInputPolicy.DRAFT,
)

@Preview(name = "P2 회원가입 · 처리 중", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun SignUpSubmittingPreview() = CredentialsPreview(
    validCredentialsState().copy(submitting = true),
)

@Preview(name = "P2 회원가입 · 닉네임 중복", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun SignUpNicknameTakenPreview() = CredentialsPreview(
    validCredentialsState().copy(serverNicknameError = "이미 사용 중인 닉네임입니다."),
)

@Preview(name = "P2 회원가입 · 가입 실패", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun SignUpFailedPreview() = CredentialsPreview(
    validCredentialsState().copy(message = "회원가입을 완료하지 못했습니다. 다시 시도해 주세요."),
)

@Preview(name = "P2 회원가입 · 완료 대화상자", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun SignUpCompletedPreview() = CredentialsPreview(
    validCredentialsState().copy(password = "", confirmation = "", completed = true),
)
