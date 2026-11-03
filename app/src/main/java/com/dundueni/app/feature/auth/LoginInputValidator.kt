package com.dundueni.app.feature.auth

object LoginInputValidator {
    fun emailError(email: String): String? = when {
        email.isBlank() -> "이메일을 입력해 주세요."
        !EMAIL_PATTERN.matches(email.trim()) -> "이메일 형식을 확인해 주세요."
        else -> null
    }

    // 로그인은 기존 계정의 자격 증명을 제출하므로 회원가입 비밀번호 정책을 적용하지 않는다.
    fun passwordError(password: String): String? =
        if (password.isEmpty()) "비밀번호를 입력해 주세요." else null

    private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
}
