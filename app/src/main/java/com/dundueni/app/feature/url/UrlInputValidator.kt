package com.dundueni.app.feature.url

import android.util.Patterns

object UrlInputValidator {
    fun urlError(url: String): String? {
        if (url.isBlank()) return null
        val trimmed = url.trim()
        val hasScheme = trimmed.startsWith("http://") || trimmed.startsWith("https://")
        val candidate = if (hasScheme) trimmed else "https://$trimmed"

        return try {
            val uri = java.net.URI(candidate)
            val host = uri.host
            if (host.isNullOrBlank() || !Patterns.WEB_URL.matcher(candidate).matches()) {
                "올바른 웹 링크(URL) 형식을 확인해 주세요 (예: https://example.com)."
            } else {
                null
            }
        } catch (_: Exception) {
            "올바른 웹 링크(URL) 형식을 확인해 주세요."
        }
    }
}
