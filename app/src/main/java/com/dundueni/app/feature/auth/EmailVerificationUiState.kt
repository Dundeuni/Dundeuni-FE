package com.dundueni.app.feature.auth

data class EmailVerificationUiState(
    val email: String = "",
    val code: String = "",
    val emailError: String? = null,
    val codeError: String? = null,
    val notice: String? = null,
    val remainingSeconds: Int = 0,
    val hasSentCode: Boolean = false,
    val busy: Boolean = false,
    val verified: Boolean = false,
    val navigationPending: Boolean = false,
) {
    val active: Boolean get() = hasSentCode && remainingSeconds > 0
    val canCheckCode: Boolean get() = active && code.length == 6 && codeError == null && !busy
    val canContinue: Boolean get() = verified || canCheckCode
    val timerText: String get() = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)
}

sealed interface SendCodeOutcome {
    data class Sent(val challengeId: String, val validForSeconds: Int) : SendCodeOutcome
    data object Unavailable : SendCodeOutcome
    data class Failed(val message: String) : SendCodeOutcome
}

sealed interface VerifyCodeOutcome {
    data object Verified : VerifyCodeOutcome
    data object Mismatch : VerifyCodeOutcome
    data object Expired : VerifyCodeOutcome
    data object Unavailable : VerifyCodeOutcome
}

/** 실제 발송·인증은 이 계약을 구현하는 API 담당자에게 연결한다. */
interface EmailVerificationGateway {
    suspend fun sendCode(email: String): SendCodeOutcome
    suspend fun verifyCode(challengeId: String, code: String): VerifyCodeOutcome
}
