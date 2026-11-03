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
    val resendRemainingSeconds: Int = 0,
    val verifyRetryRemainingSeconds: Int = 0,
) {
    val canSendCode: Boolean get() = !busy && !verified && resendRemainingSeconds == 0
    val active: Boolean get() = hasSentCode && remainingSeconds > 0
    val canCheckCode: Boolean get() = active && code.length == 6 && codeError == null && !busy && verifyRetryRemainingSeconds == 0
    val canContinue: Boolean get() = verified || canCheckCode
    val timerText: String get() = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)
}

sealed interface SendCodeOutcome {
    data class Sent(
        val challengeId: String, val validForSeconds: Int,
        val resendAfterSeconds: Int = 0,
        val expiresAtElapsedMillis: Long? = null,
        val resendAtElapsedMillis: Long? = null,
    ) : SendCodeOutcome
    data object Unavailable : SendCodeOutcome
    data class Failed(val message: String, val retryAfterSeconds: Int? = null) : SendCodeOutcome
}

sealed interface VerifyCodeOutcome {
    data object Verified : VerifyCodeOutcome
    data class VerifiedForReset(val grant: String) : VerifyCodeOutcome
    class VerifiedForSignUp(val token: String, val expiresAtElapsedMillis: Long) : VerifyCodeOutcome
    data class Failed(val reason: AuthFailure, val retryAfterSeconds: Int? = null) : VerifyCodeOutcome
    data object Mismatch : VerifyCodeOutcome
    data object Expired : VerifyCodeOutcome
    data object Unavailable : VerifyCodeOutcome
}

/** 실제 발송·인증은 이 계약을 구현하는 API 담당자에게 연결한다. */
interface EmailVerificationGateway {
    suspend fun sendCode(email: String): SendCodeOutcome
    suspend fun verifyCode(challengeId: String, code: String): VerifyCodeOutcome
}
