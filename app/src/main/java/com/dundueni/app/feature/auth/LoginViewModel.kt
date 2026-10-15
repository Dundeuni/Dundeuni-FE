package com.dundueni.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val submitter: LoginSubmitter,
    initialState: LoginUiState = LoginUiState(),
) : ViewModel() {
    private val state = MutableStateFlow(initialState)
    val uiState = state.asStateFlow()
    private var submission: Job? = null

    fun changeEmail(value: String) {
        if (state.value.submitting) return
        state.update { it.copy(email = value, emailError = null, message = null, credentialsRejected = false) }
    }

    fun changePassword(value: String) {
        if (state.value.submitting) return
        state.update { it.copy(password = value, passwordError = null, message = null, credentialsRejected = false) }
    }

    fun togglePasswordVisibility() {
        if (state.value.submitting) return
        state.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun login(onSuccess: (com.dundueni.app.data.auth.AuthSession) -> Unit) {
        if (state.value.submitting || submission?.isActive == true) return
        val input = state.value
        val emailError = LoginInputValidator.emailError(input.email)
        val passwordError = LoginInputValidator.passwordError(input.password)
        if (emailError != null || passwordError != null) {
            state.update { it.copy(emailError = emailError, passwordError = passwordError, message = null, credentialsRejected = false) }
            return
        }
        state.update { it.copy(submitting = true, message = null, credentialsRejected = false) }
        submission = viewModelScope.launch {
            val outcome = try {
                submitter.submit(input.email.trim(), input.password)
            } catch (cancelled: CancellationException) {
                state.update { it.copy(submitting = false) }
                throw cancelled
            } catch (_: Exception) {
                LoginOutcome.ConnectionError
            }
            when (outcome) {
                is LoginOutcome.Success -> {
                    state.update { LoginUiState(email = it.email) }
                    onSuccess(outcome.session)
                }
                LoginOutcome.InvalidCredentials -> state.update {
                    it.copy(submitting = false, credentialsRejected = true, message = "이메일 또는 비밀번호가 일치하지 않습니다.")
                }
                LoginOutcome.Unavailable -> state.update {
                    it.copy(submitting = false, message = "로그인 연결을 준비 중입니다.")
                }
                LoginOutcome.ConnectionError -> state.update {
                    it.copy(submitting = false, message = "연결하지 못했습니다. 다시 시도해 주세요.")
                }
            }
        }
    }
}
