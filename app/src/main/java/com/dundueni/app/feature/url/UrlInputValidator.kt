package com.dundueni.app.feature.url

object UrlInputValidator {
    fun urlError(url: String): String? {
        if (url.isBlank()) return null
        val trimmed = url.trim()
        val urlPattern = Regex("^(https?://)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(/.*)?$")
        val ipPattern = Regex("^(https?://)?\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(:\\d+)?(/.*)?$")
        return if (!urlPattern.matches(trimmed) && !ipPattern.matches(trimmed) && !trimmed.startsWith("http")) {
            "올바른 웹 링크(URL) 형식을 확인해 주세요."
        } else {
            null
        }
    }
}
