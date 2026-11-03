package com.dundueni.app.data.auth

import com.dundueni.app.BuildConfig
import com.dundueni.app.data.remote.network.RetrofitClient
import com.dundueni.app.feature.auth.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object AuthDependencies {
    val http: HttpAuthGateway? by lazy {
        if (isHttpConfiguration(BuildConfig.AUTH_API_MODE, BuildConfig.AUTH_API_BASE_URL)) {
            HttpAuthGateway(RetrofitClient(BuildConfig.AUTH_API_BASE_URL).authApiService)
        } else null
    }

    // The default debug experience is preserved. An invalid explicit HTTP config never falls back to a mock.
    val defaultDebugMock: Boolean get() = BuildConfig.DEBUG && BuildConfig.AUTH_API_MODE == "UNCONNECTED"
    fun loginSubmitter(): LoginSubmitter = http ?: if (defaultDebugMock) {
        Class.forName("com.dundueni.app.feature.auth.DebugMockLoginSubmitter")
            .getDeclaredConstructor().newInstance() as LoginSubmitter
    } else LoginSubmitter { _, _ -> LoginOutcome.Unavailable }

    fun emailGateway(): EmailVerificationGateway = http ?: UnconnectedEmailGateway
    fun termsGateway(): TermsGateway = http ?: TermsGateway { TermsOutcome.Unavailable }

    fun signUpSubmitter(flowId: String?): SignUpSubmitter = http?.let {
        HttpSignUpSubmitter(it, { SignUpFlowMemory.authorization(flowId) }, { SignUpFlowMemory.clearVerification(flowId) })
    } ?: SignUpSubmitter { _, _, _, _ -> SignUpOutcome.Unavailable }

    internal fun isHttpConfiguration(mode: String, baseUrl: String): Boolean {
        val url = baseUrl.toHttpUrlOrNull() ?: return false
        return mode == "HTTP" && url.isHttps && url.encodedPath == "/" &&
            url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null
    }
}

private object UnconnectedEmailGateway : EmailVerificationGateway {
    override suspend fun sendCode(email: String) = SendCodeOutcome.Unavailable
    override suspend fun verifyCode(challengeId: String, code: String) = VerifyCodeOutcome.Unavailable
}
