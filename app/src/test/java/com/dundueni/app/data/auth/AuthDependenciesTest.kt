package com.dundueni.app.data.auth

import com.dundueni.app.BuildConfig
import com.dundueni.app.feature.auth.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AuthDependenciesTest {
    @Test fun defaultWiringNeverUsesSampleAuthenticationInRelease() = runBlocking {
        assertEquals("UNCONNECTED", BuildConfig.AUTH_API_MODE)
        assertNull(AuthDependencies.http)
        if (BuildConfig.DEBUG) {
            assertTrue(AuthDependencies.defaultDebugMock)
            assertEquals("DebugMockLoginSubmitter", AuthDependencies.loginSubmitter().javaClass.simpleName)
        } else {
            assertFalse(AuthDependencies.defaultDebugMock)
            assertEquals(LoginOutcome.Unavailable, AuthDependencies.loginSubmitter().submit("user@example.com", "password"))
            assertEquals(SendCodeOutcome.Unavailable, AuthDependencies.emailGateway().sendCode("user@example.com"))
            assertEquals(VerifyCodeOutcome.Unavailable, AuthDependencies.emailGateway().verifyCode("id", "123456"))
            assertEquals(SignUpOutcome.Unavailable, AuthDependencies.signUpSubmitter(null).submit("user@example.com", "password", "name", false))
            assertEquals(TermsOutcome.Unavailable, AuthDependencies.termsGateway().loadTerms())
        }
    }
}
