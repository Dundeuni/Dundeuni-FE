package com.dundueni.app.data.auth

import android.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class JwtAccessTokenTest {
    @Test fun readsJwtExpAndDeterminesValidityWithoutChoosingAServiceLifetime() {
        val token = jwt(EXP)
        val session = AuthSession(token, AuthSessionSource.BACKEND)
        assertEquals(EXP, session.expiresAtEpochSeconds())
        assertTrue(session.isValid(EXP - 1))
        assertFalse(session.isValid(EXP))
    }

    @Test fun rejectsMalformedTokensAndMissingExp() {
        assertNull(JwtAccessToken.expiresAtEpochSeconds("not-a-jwt"))
        assertNull(JwtAccessToken.expiresAtEpochSeconds("${encode("{}")}.${encode("{\"sub\":\"x\"}")}.sig"))
    }

    private fun jwt(exp: Long) = "${encode("{}")}.${encode("{\"exp\":$exp}")}.signature"
    private fun encode(value: String) = Base64.encodeToString(
        value.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )
    private companion object { const val EXP = 2_000_000_000L }
}
