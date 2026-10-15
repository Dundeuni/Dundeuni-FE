package com.dundueni.app.feature.phone

object PhoneInputValidator {
    fun phoneError(phone: String): String? {
        if (phone.isBlank()) return null
        val cleaned = phone.replace(Regex("[^0-9]"), "")
        return when {
            cleaned.length < 8 || cleaned.length > 12 -> "올바른 전화번호 형식을 입력해 주세요 (9~12자리)."
            else -> null
        }
    }
}
