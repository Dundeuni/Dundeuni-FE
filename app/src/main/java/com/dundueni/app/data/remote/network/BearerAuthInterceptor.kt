package com.dundueni.app.data.remote.network

import com.dundueni.app.data.auth.AuthSessionSource
import com.dundueni.app.data.auth.AuthSessionStore
import okhttp3.Interceptor
import okhttp3.Response
import com.google.gson.JsonParser

class BearerAuthInterceptor(
    private val sessionStore: AuthSessionStore,
    private val onUnauthorized: (String) -> Unit,
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1_000L },
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requiresAuth = original.header(REQUIRES_AUTH_HEADER) == "true"
        val request = original.newBuilder().removeHeader(REQUIRES_AUTH_HEADER)
        var attachedToken: String? = null
        if (requiresAuth) {
            val session = sessionStore.read()
            if (session?.source == AuthSessionSource.BACKEND && session.isValid(nowEpochSeconds())) {
                attachedToken = session.accessToken
                request.header("Authorization", "Bearer ${session.accessToken}")
            } else if (session?.source == AuthSessionSource.BACKEND) {
                invalidate(session.accessToken)
            }
        }
        val response = chain.proceed(request.build())
        if (attachedToken != null && (response.code == 401 || response.isAuth401())) {
            invalidate(attachedToken!!)
        }
        return response
    }

    private fun Response.isAuth401(): Boolean = runCatching {
        val bodyText = peekBody(MAX_ERROR_BODY_BYTES).string()
        JsonParser.parseString(bodyText).asJsonObject.get("code")?.asString == "AUTH401"
    }.getOrDefault(false)

    private fun invalidate(token: String) {
        if (sessionStore.read()?.accessToken != token) return
        onUnauthorized(token)
        if (sessionStore.read()?.accessToken == token) sessionStore.clear()
    }

    companion object {
        const val REQUIRES_AUTH_HEADER = "X-Dundeuni-Auth-Required"
        private const val MAX_ERROR_BODY_BYTES = 64L * 1024L
    }
}
