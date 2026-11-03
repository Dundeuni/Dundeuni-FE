package com.dundueni.app.feature.linkphone

/**
 * 링크(URL) 및 전화번호 입력값의 유효성을 검사하는 검증기이다.
 */
object LinkPhoneInputValidator {

    fun linkError(link: String): String? {
        if (link.isBlank()) return null
        val trimmed = link.trim()
        val urlPattern = Regex("^(https?://)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(/.*)?$")
        val ipPattern = Regex("^(https?://)?\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(:\\d+)?(/.*)?$")
        return if (!urlPattern.matches(trimmed) && !ipPattern.matches(trimmed) && !trimmed.startsWith("http")) {
            "올바른 웹 링크(URL) 형식을 확인해 주세요."
        } else {
            null
        }
    }

    fun phoneError(phone: String): String? {
        if (phone.isBlank()) return null
        val cleaned = phone.replace(Regex("[^0-9]"), "")
        return when {
            cleaned.length < 8 || cleaned.length > 12 -> "올바른 전화번호 형식을 입력해 주세요 (9~12자리)."
            else -> null
        }
    }
}
