package com.dundueni.app.feature.auth

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val message: String? = null,
    val credentialsRejected: Boolean = false,
    val submitting: Boolean = false,
)

sealed interface LoginOutcome {
    data object Success : LoginOutcome
    data object InvalidCredentials : LoginOutcome
    data object Unavailable : LoginOutcome
    data object ConnectionError : LoginOutcome
}

/** 인증 API 담당자가 연결할 제출 계약. 비밀번호를 저장하거나 로그에 남기지 않는다. */
fun interface LoginSubmitter {
    suspend fun submit(email: String, password: String): LoginOutcome
}
