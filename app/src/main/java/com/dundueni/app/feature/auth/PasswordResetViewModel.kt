package com.dundueni.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI integration boundary only; the BE reset-token contract has not yet been agreed. */
interface PasswordResetGateway : EmailVerificationGateway {
    suspend fun resetPassword(grant: String, password: String): PasswordResetOutcome
}
sealed interface PasswordResetOutcome {
    data object Changed : PasswordResetOutcome
    data object Unavailable : PasswordResetOutcome
    data object VerificationExpired : PasswordResetOutcome
    data object Failed : PasswordResetOutcome
}

data class PasswordResetUiState(
    val fields: SignUpCredentialsUiState = SignUpCredentialsUiState(),
    val settingPassword: Boolean = false,
    val submitting: Boolean = false,
    val completed: Boolean = false,
    val needsVerification: Boolean = false,
    val message: String? = null,
) {
    val canSubmit get() = settingPassword && !submitting && !completed && !needsVerification &&
        fields.lengthSatisfied && fields.combinationSatisfied && fields.passwordsMatch
}

class PasswordResetViewModel(private val gateway: PasswordResetGateway) : ViewModel() {
    private val state = MutableStateFlow(PasswordResetUiState())
    val uiState = state.asStateFlow()
    private var operation: Job? = null
    private var revision = 0
    private var grant: String? = null
    private val editable get() = !state.value.submitting && !state.value.completed && !state.value.needsVerification

    fun begin(email: String, verificationGrant: String?) {
        if (LoginInputValidator.emailError(email) != null || verificationGrant.isNullOrBlank()) return
        grant = verificationGrant
        state.value = PasswordResetUiState(fields = SignUpCredentialsUiState(email = email), settingPassword = true)
    }
    fun returnToVerification() {
        revision++
        operation?.cancel()
        grant = null
        state.value = PasswordResetUiState()
    }
    fun changePassword(value: String) { if (editable) state.update { it.copy(fields = it.fields.copy(password = value, passwordTouched = true), message = null) } }
    fun changeConfirmation(value: String) { if (editable) state.update { it.copy(fields = it.fields.copy(confirmation = value, confirmationTouched = true), message = null) } }
    fun togglePassword() { if (editable) state.update { it.copy(fields = it.fields.copy(passwordVisible = !it.fields.passwordVisible)) } }

    fun submit() {
        if (!editable) return
        state.update { it.copy(fields = it.fields.copy(passwordTouched = true, confirmationTouched = true)) }
        val token = grant ?: return
        if (!state.value.canSubmit) return
        val password = state.value.fields.password
        state.update { it.copy(submitting = true, message = null) }
        val generation = revision
        operation = viewModelScope.launch {
            val outcome = try { gateway.resetPassword(token, password) }
            catch (cancelled: CancellationException) {
                if (generation == revision) state.update { it.copy(submitting = false) }; throw cancelled
            } catch (_: Exception) { PasswordResetOutcome.Failed }
            if (generation != revision) return@launch
            state.update {
                when (outcome) {
                    PasswordResetOutcome.Changed -> {
                        grant = null
                        it.copy(submitting = false, completed = true, fields = SignUpCredentialsUiState(email = it.fields.email))
                    }
                    PasswordResetOutcome.VerificationExpired -> {
                        grant = null
                        it.copy(submitting = false, needsVerification = true,
                            fields = SignUpCredentialsUiState(email = it.fields.email), message = "인증이 만료되었습니다. 이메일 인증을 다시 진행해 주세요.")
                    }
                    PasswordResetOutcome.Unavailable -> it.copy(submitting = false, message = "비밀번호 변경 연결을 준비 중입니다.")
                    PasswordResetOutcome.Failed -> it.copy(submitting = false, message = "비밀번호를 변경하지 못했습니다. 다시 시도해 주세요.")
                }
            }
        }
    }
}

internal object UnconnectedPasswordResetGateway : PasswordResetGateway {
    override suspend fun sendCode(email: String) = SendCodeOutcome.Unavailable
    override suspend fun verifyCode(challengeId: String, code: String) = VerifyCodeOutcome.Unavailable
    override suspend fun resetPassword(grant: String, password: String) = PasswordResetOutcome.Unavailable
}
