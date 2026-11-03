package com.dundueni.app.data.auth

import android.util.Base64
import com.google.gson.JsonParser

enum class AuthSessionSource { BACKEND, MOCK }

data class AuthSession(val accessToken: String, val source: AuthSessionSource) {
    fun expiresAtEpochSeconds(): Long? = JwtAccessToken.expiresAtEpochSeconds(accessToken)

    fun isValid(nowEpochSeconds: Long = System.currentTimeMillis() / 1_000L): Boolean =
        expiresAtEpochSeconds()?.let { it > nowEpochSeconds } == true
}

interface AuthSessionStore {
    fun read(): AuthSession?
    fun save(session: AuthSession)
    fun clear()
}

object JwtAccessToken {
    fun expiresAtEpochSeconds(token: String): Long? = runCatching {
        val parts = token.split('.')
        require(parts.size == 3)
        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8)
        val exp = JsonParser.parseString(payload).asJsonObject.get("exp")
        require(exp != null && exp.isJsonPrimitive && exp.asJsonPrimitive.isNumber)
        exp.asLong.takeIf { it > 0L }
    }.getOrNull()
}
