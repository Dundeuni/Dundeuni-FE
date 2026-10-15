package com.dundueni.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignUpCredentialsViewModel(
    email: String,
    private val marketingAgreed: Boolean,
    private val submitter: SignUpSubmitter,
) : ViewModel() {
    private val state = MutableStateFlow(SignUpCredentialsUiState(email = email))
    val uiState = state.asStateFlow()
    private val editable get() = !state.value.submitting && !state.value.completed

    fun changePassword(value: String) { if (editable) state.update { it.copy(password = value, passwordTouched = true, message = null) } }
    fun changeConfirmation(value: String) { if (editable) state.update { it.copy(confirmation = value, confirmationTouched = true, message = null) } }
    fun changeNickname(value: String) { if (editable) state.update { it.copy(nickname = value, nicknameTouched = true, serverNicknameError = null, message = null) } }
    fun togglePassword() { if (editable) state.update { it.copy(passwordVisible = !it.passwordVisible) } }

    fun submit() {
        if (!editable) return
        state.update { it.copy(passwordTouched = true, confirmationTouched = true, nicknameTouched = true) }
        if (!state.value.canSubmit) return
        val input = state.value
        state.update { it.copy(submitting = true, message = null) }
        viewModelScope.launch {
            val outcome = try {
                submitter.submit(input.email, input.password, input.nickname.trim(), marketingAgreed)
            } catch (cancelled: CancellationException) {
                state.update { it.copy(submitting = false) }
                throw cancelled
            } catch (_: Exception) { SignUpOutcome.Failed }
            state.update {
                when (outcome) {
                    SignUpOutcome.Registered -> it.copy(submitting = false, completed = true, password = "", confirmation = "", passwordVisible = false,
                        passwordTouched = false, confirmationTouched = false, nickname = input.nickname.trim())
                    SignUpOutcome.Unavailable -> it.copy(submitting = false, message = "회원가입 연결을 준비 중입니다.")
                    SignUpOutcome.NicknameTaken -> it.copy(submitting = false, serverNicknameError = "이미 사용 중인 닉네임입니다.")
                    SignUpOutcome.Failed -> it.copy(submitting = false, message = "회원가입을 완료하지 못했습니다. 다시 시도해 주세요.")
                }
            }
        }
    }
}
