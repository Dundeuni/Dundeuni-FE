package com.dundueni.app.feature.auth

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil

class EmailVerificationViewModel(
    private val gateway: EmailVerificationGateway,
    private val now: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {
    private val state = MutableStateFlow(EmailVerificationUiState())
    val uiState = state.asStateFlow()
    internal var resetGrant: String? = null
        private set
    internal var signUpGrant: VerifyCodeOutcome.VerifiedForSignUp? = null
        private set
    private var challengeId: String? = null
    private var expiresAt = 0L
    private var resendAt = 0L
    private var verifyRetryAt = 0L
    private var operation: Job? = null
    private var timer: Job? = null
    private var revision = 0

    fun changeEmail(value: String) {
        if (value == state.value.email) return
        revision++
        operation?.cancel()
        timer?.cancel()
        resetGrant = null
        signUpGrant = null
        challengeId = null
        expiresAt = 0
        resendAt = 0
        verifyRetryAt = 0
        state.value = EmailVerificationUiState(email = value)
    }

    fun changeCode(value: String) {
        if (state.value.busy || state.value.verified) return
        // 문자를 임의로 제거해 다른 번호를 만들지 않고 숫자 6자리 입력만 허용한다.
        if (value.length <= 6 && value.all { it in '0'..'9' }) {
            state.update { it.copy(code = value, codeError = null, notice = null) }
        }
    }

    fun sendCode() {
        refreshTimer()
        if (!state.value.canSendCode) return
        val input = state.value.email.trim()
        val error = LoginInputValidator.emailError(input)
        if (error != null) {
            state.update { it.copy(emailError = error) }
            return
        }
        val generation = revision
        state.update { it.copy(emailError = null, notice = null, busy = true) }
        operation = viewModelScope.launch {
            try {
                val response = gateway.sendCode(input)
                if (generation != revision) return@launch
                when (response) {
                    is SendCodeOutcome.Sent -> {
                        require(response.validForSeconds > 0 && response.challengeId.isNotBlank())
                        challengeId = response.challengeId
                        signUpGrant = null
                        expiresAt = response.expiresAtElapsedMillis ?: (now() + response.validForSeconds * 1000L)
                        resendAt = response.resendAtElapsedMillis ?: (now() + response.resendAfterSeconds * 1000L)
                        verifyRetryAt = 0
                        state.value = EmailVerificationUiState(email = input, hasSentCode = true, remainingSeconds = response.validForSeconds)
                        refreshTimer()
                        startTimer()
                    }
                    SendCodeOutcome.Unavailable -> state.update { it.copy(busy = false, notice = "이메일 인증 연결을 준비 중입니다.") }
                    is SendCodeOutcome.Failed -> {
                        response.retryAfterSeconds?.let { resendAt = now() + it * 1000L }
                        state.update { it.copy(busy = false, notice = response.message) }
                        refreshTimer()
                        startTimer()
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (generation == revision) state.update { it.copy(busy = false, notice = "인증번호를 보내지 못했습니다. 다시 시도해 주세요.") }
            }
        }
    }

    fun refreshTimer() {
        if (signUpGrant?.let { it.expiresAtElapsedMillis <= now() } == true) invalidateSignUpGrant()
        state.update { it.copy(resendRemainingSeconds = secondsUntil(resendAt), verifyRetryRemainingSeconds = secondsUntil(verifyRetryAt)) }
        if (!state.value.hasSentCode || state.value.verified) return
        val remaining = ceil((expiresAt - now()).coerceAtLeast(0) / 1000.0).toInt()
        state.update { it.copy(remainingSeconds = remaining,
            codeError = if (remaining == 0) "인증 시간이 만료되었습니다. 새 번호를 받아 주세요." else it.codeError) }
    }

    private fun secondsUntil(deadline: Long) = ceil((deadline - now()).coerceAtLeast(0) / 1000.0).toInt()

    private fun startTimer() {
        timer?.cancel()
        timer = viewModelScope.launch {
            while ((state.value.active && !state.value.verified) || signUpGrant != null || state.value.resendRemainingSeconds > 0 || state.value.verifyRetryRemainingSeconds > 0) {
                delay(1000)
                refreshTimer()
            }
        }
    }

    fun invalidateSignUpGrant() {
        signUpGrant = null
        challengeId = null
        state.update { it.copy(verified = false, navigationPending = false, hasSentCode = false, code = "", notice = AuthFailure.VERIFICATION_EXPIRED.message) }
    }

    fun checkCode(onVerified: (String) -> Unit) {
        refreshTimer()
        if (state.value.verified) {
            state.update { it.copy(navigationPending = true) }
            onVerified(state.value.email)
            return
        }
        if (!state.value.canCheckCode) return
        val challenge = challengeId ?: return
        val code = state.value.code
        val generation = revision
        state.update { it.copy(busy = true, notice = null) }
        operation = viewModelScope.launch {
            try {
                val response = gateway.verifyCode(challenge, code)
                if (generation != revision) return@launch
                refreshTimer()
                when (response) {
                    is VerifyCodeOutcome.VerifiedForSignUp -> {
                        require(response.token.isNotBlank() && response.expiresAtElapsedMillis > now())
                        signUpGrant = response
                        state.update { it.copy(busy = false, verified = true, navigationPending = true, code = "", codeError = null, notice = "이메일 인증을 완료했습니다.") }
                        startTimer()
                        onVerified(state.value.email)
                    }
                    is VerifyCodeOutcome.Failed -> {
                        response.retryAfterSeconds?.let { verifyRetryAt = now() + it * 1000L }
                        state.update { it.copy(busy = false, notice = response.reason.message) }
                        refreshTimer()
                    }
                    VerifyCodeOutcome.Verified, is VerifyCodeOutcome.VerifiedForReset -> {
                        resetGrant = (response as? VerifyCodeOutcome.VerifiedForReset)?.grant?.also { require(it.isNotBlank()) }
                        timer?.cancel()
                        state.update { it.copy(busy = false, verified = true, navigationPending = true, code = "", codeError = null, notice = "이메일 인증을 완료했습니다.") }
                        onVerified(state.value.email)
                    }
                    VerifyCodeOutcome.Mismatch -> state.update { it.copy(busy = false, codeError = "인증번호가 일치하지 않습니다.") }
                    VerifyCodeOutcome.Expired -> {
                        expiresAt = now()
                        refreshTimer()
                        state.update { it.copy(busy = false) }
                    }
                    VerifyCodeOutcome.Unavailable -> state.update { it.copy(busy = false, notice = "이메일 인증 연결을 준비 중입니다.") }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (generation == revision) state.update { it.copy(busy = false, notice = "확인하지 못했습니다. 다시 시도해 주세요.") }
            }
        }
    }

    fun consumeNavigation() {
        state.update { it.copy(navigationPending = false) }
    }
}
