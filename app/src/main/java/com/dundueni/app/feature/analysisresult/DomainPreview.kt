package com.dundueni.app.feature.analysisresult

import java.net.URI
import java.util.Locale

/** Parses only. This function never opens a connection or follows redirects. */
internal fun domainForPreview(url: String?): String? = runCatching {
    val uri = URI(url ?: return null)
    if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("https", "http")) return null
    uri.host?.takeIf { it.isNotBlank() }
}.getOrNull()
