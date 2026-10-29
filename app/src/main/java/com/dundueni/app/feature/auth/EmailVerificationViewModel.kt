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
    private var challengeId: String? = null
    private var expiresAt = 0L
    private var operation: Job? = null
    private var timer: Job? = null
    private var revision = 0

    fun changeEmail(value: String) {
        if (value == state.value.email) return
        revision++
        operation?.cancel()
        timer?.cancel()
        resetGrant = null
        challengeId = null
        expiresAt = 0
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
        if (state.value.busy || state.value.verified) return
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
                        require(response.validForSeconds in 1..86400 && response.challengeId.isNotBlank())
                        challengeId = response.challengeId
                        expiresAt = now() + response.validForSeconds * 1000L
                        state.value = EmailVerificationUiState(email = input, hasSentCode = true, remainingSeconds = response.validForSeconds)
                        timer?.cancel()
                        timer = viewModelScope.launch {
                            while (state.value.active && !state.value.verified) {
                                delay(1000)
                                refreshTimer()
                            }
                        }
                    }
                    SendCodeOutcome.Unavailable -> state.update { it.copy(busy = false, notice = "이메일 인증 연결을 준비 중입니다.") }
                    is SendCodeOutcome.Failed -> state.update { it.copy(busy = false, notice = response.message) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (generation == revision) state.update { it.copy(busy = false, notice = "인증번호를 보내지 못했습니다. 다시 시도해 주세요.") }
            }
        }
    }

    fun refreshTimer() {
        if (!state.value.hasSentCode || state.value.verified) return
        val remaining = ceil((expiresAt - now()).coerceAtLeast(0) / 1000.0).toInt()
        state.update { it.copy(remainingSeconds = remaining,
            codeError = if (remaining == 0) "인증 시간이 만료되었습니다. 새 번호를 받아 주세요." else it.codeError) }
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
                if (!state.value.active) {
                    state.update { it.copy(busy = false) }
                    return@launch
                }
                when (response) {
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
