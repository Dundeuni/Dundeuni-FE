package com.dundueni.app.feature.auth

import android.os.SystemClock
import com.dundueni.app.data.remote.dto.TermAgreement
import java.util.UUID

/** A random handle may travel in Intents; credentials and grants never do. Process death restarts signup. */
object SignUpFlowMemory {
    const val EXTRA_FLOW_ID = "signup_flow_id"
    private class Flow(val terms: List<TermAgreement>, var grant: SignUpAuthorization? = null)
    private val flows = mutableMapOf<String, Flow>()

    @Synchronized fun create(agreements: List<TermAgreement>): String = UUID.randomUUID().toString().also {
        flows[it] = Flow(agreements.toList())
    }
    @Synchronized fun contains(id: String?): Boolean = flows.containsKey(id)
    @Synchronized fun verify(id: String?, email: String, grant: VerifyCodeOutcome.VerifiedForSignUp): Boolean {
        val flow = flows[id] ?: return false
        if (grant.expiresAtElapsedMillis <= SystemClock.elapsedRealtime()) return false
        flow.grant = SignUpAuthorization(email, grant.token, grant.expiresAtElapsedMillis, flow.terms)
        return true
    }
    @Synchronized fun authorization(id: String?): SignUpAuthorization? {
        val flow = flows[id] ?: return null
        if (flow.grant?.let { it.expiresAtElapsedMillis <= SystemClock.elapsedRealtime() } == true) flow.grant = null
        return flow.grant
    }
    @Synchronized fun clearVerification(id: String?) { flows[id]?.grant = null }
    @Synchronized fun remove(id: String?) { flows.remove(id) }
}
