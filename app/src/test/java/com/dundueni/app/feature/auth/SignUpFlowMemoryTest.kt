package com.dundueni.app.feature.auth

import android.os.SystemClock
import com.dundueni.app.data.remote.dto.TermAgreement
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class SignUpFlowMemoryTest {
    @Test fun memoryGrantRetainsExactAgreementsAndIsRemovedOnConsumptionExpiryOrAbandonment() {
        val agreements = listOf(TermAgreement("service", "2.3", true), TermAgreement("privacy", "4.1", true))
        val flow = SignUpFlowMemory.create(agreements)
        try {
            assertNull(SignUpFlowMemory.authorization(flow))
            assertTrue(SignUpFlowMemory.verify(flow, "user@example.com",
                VerifyCodeOutcome.VerifiedForSignUp("private-token", SystemClock.elapsedRealtime() + 1000)))
            val grant = requireNotNull(SignUpFlowMemory.authorization(flow))
            assertEquals("user@example.com", grant.email)
            assertEquals(agreements, grant.agreements)
            assertFalse(grant.toString().contains("private-token"))
            ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
            assertNull(SignUpFlowMemory.authorization(flow))
            assertTrue(SignUpFlowMemory.verify(flow, "other@example.com",
                VerifyCodeOutcome.VerifiedForSignUp("new-token", SystemClock.elapsedRealtime() + 1000)))
            SignUpFlowMemory.clearVerification(flow)
            assertNull(SignUpFlowMemory.authorization(flow))
        } finally { SignUpFlowMemory.remove(flow) }
        assertFalse(SignUpFlowMemory.contains(flow))
        assertFalse(SignUpFlowMemory.verify(flow, "user@example.com",
            VerifyCodeOutcome.VerifiedForSignUp("token", SystemClock.elapsedRealtime() + 1000)))
        assertNull(SignUpFlowMemory.authorization("process-death-stale-handle"))
    }
}
