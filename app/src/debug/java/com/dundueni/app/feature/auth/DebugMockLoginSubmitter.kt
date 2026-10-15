package com.dundueni.app.feature.auth

import android.util.Base64
import com.dundueni.app.data.auth.AuthSession
import com.dundueni.app.data.auth.AuthSessionSource

/** Local-only login for debug builds. Its fake JWT is never attached to backend requests. */
class DebugMockLoginSubmitter : LoginSubmitter {
    override suspend fun submit(email: String, password: String): LoginOutcome {
        val expiresAt = System.currentTimeMillis() / 1_000L + MOCK_LIFETIME_SECONDS
        val header = encode("""{"alg":"none","typ":"JWT"}""")
        val payload = encode("""{"sub":"debug-user","exp":$expiresAt,"mock":true}""")
        return LoginOutcome.Success(AuthSession("$header.$payload.debug-only", AuthSessionSource.MOCK))
    }

    private fun encode(value: String): String = Base64.encodeToString(
        value.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )

    private companion object { const val MOCK_LIFETIME_SECONDS = 15 * 60L }
}
