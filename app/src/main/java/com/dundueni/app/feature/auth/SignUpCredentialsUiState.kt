package com.dundueni.app.feature.auth

data class SignUpCredentialsUiState(
    val email: String = "",
    val password: String = "",
    val confirmation: String = "",
    val nickname: String = "",
    val passwordVisible: Boolean = false,
    val passwordTouched: Boolean = false,
    val confirmationTouched: Boolean = false,
    val nicknameTouched: Boolean = false,
    val submitting: Boolean = false,
    val completed: Boolean = false,
    val message: String? = null,
    val serverNicknameError: String? = null,
) {
    val lengthSatisfied get() = password.length in 8..20
    val combinationSatisfied get() = password.any { it in 'a'..'z' || it in 'A'..'Z' } && password.any { it in '0'..'9' }
    val specialSatisfied get() = password.any { !it.isLetterOrDigit() && !it.isWhitespace() }
    val passwordsMatch get() = password.isNotEmpty() && password == confirmation
    val nicknameLength get() = nickname.codePointCount(0, nickname.length)
    val passwordError: String? get() = when {
        !passwordTouched -> null
        password.isEmpty() -> "비밀번호를 입력해 주세요."
        !lengthSatisfied -> "비밀번호는 8~20자로 입력해 주세요."
        !combinationSatisfied -> "영문과 숫자를 함께 입력해 주세요."
        else -> null
    }
    val confirmationError: String? get() = when {
        !confirmationTouched -> null
        confirmation.isEmpty() -> "비밀번호를 다시 입력해 주세요."
        !passwordsMatch -> "비밀번호가 일치하지 않습니다."
        else -> null
    }
    val nicknameError: String? get() = serverNicknameError ?: when {
        !nicknameTouched -> null
        nickname.isBlank() -> "닉네임을 입력해 주세요."
        nicknameLength > 10 -> "글자 제한수를 초과하였습니다."
        else -> null
    }
    val canSubmit get() = lengthSatisfied && combinationSatisfied && passwordsMatch && nickname.isNotBlank() && nicknameLength <= 10 && nicknameError == null && !submitting && !completed
}

sealed interface SignUpOutcome {
    data object Registered : SignUpOutcome
    data object Unavailable : SignUpOutcome
    data object NicknameTaken : SignUpOutcome
    data object Failed : SignUpOutcome
}

fun interface SignUpSubmitter {
    suspend fun submit(email: String, password: String, nickname: String, marketingAgreed: Boolean): SignUpOutcome
}
