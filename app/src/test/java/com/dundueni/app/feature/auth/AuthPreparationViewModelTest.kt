package com.dundueni.app.feature.auth

import androidx.lifecycle.ViewModelStore
import com.dundueni.app.data.auth.AuthSession
import com.dundueni.app.data.auth.AuthSessionSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthPreparationViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private var clock = 0L
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun close() { models.clear(); Dispatchers.resetMain() }
    private fun model(gateway: EmailVerificationGateway) = EmailVerificationViewModel(gateway) { clock + dispatcher.scheduler.currentTime }
        .also { models.put("email", it) }

    @Test fun resendUsesServerCooldownAndBackgroundResumeRecalculatesBothTimers() = runTest {
        var sends = 0
        val vm = model(object : EmailVerificationGateway {
            override suspend fun sendCode(email: String): SendCodeOutcome {
                sends++
                return SendCodeOutcome.Sent("id-$sends", 180, 60, clock + 180_000, clock + 60_000)
            }
            override suspend fun verifyCode(challengeId: String, code: String) = VerifyCodeOutcome.Mismatch
        })
        vm.changeEmail("user@example.com"); vm.sendCode(); runCurrent()
        assertFalse(vm.uiState.value.canSendCode)
        vm.sendCode(); runCurrent(); assertEquals(1, sends)
        clock = 59_500; vm.refreshTimer()
        assertEquals(1, vm.uiState.value.resendRemainingSeconds)
        assertEquals(121, vm.uiState.value.remainingSeconds)
        clock = 60_000; vm.sendCode(); runCurrent()
        assertEquals(2, sends)
        assertEquals(180, vm.uiState.value.remainingSeconds)
    }

    @Test fun send429BeforeAnyChallengeStillStartsRetryTimer() = runTest {
        var sends = 0
        val vm = model(object : EmailVerificationGateway {
            override suspend fun sendCode(email: String): SendCodeOutcome { sends++; return SendCodeOutcome.Failed(AuthFailure.RATE_LIMITED.message, 90) }
            override suspend fun verifyCode(challengeId: String, code: String) = VerifyCodeOutcome.Unavailable
        })
        vm.changeEmail("user@example.com"); vm.sendCode(); runCurrent()
        assertFalse(vm.uiState.value.hasSentCode)
        vm.sendCode(); runCurrent(); assertEquals(1, sends)
        clock = 90_000; vm.refreshTimer()
        assertTrue(vm.uiState.value.canSendCode)
    }

    @Test fun verifiedGrantExpiresIndependentlyOfCodeAndEmailChangeClearsIt() = runTest {
        val vm = model(object : EmailVerificationGateway {
            override suspend fun sendCode(email: String) = SendCodeOutcome.Sent("id", 180)
            override suspend fun verifyCode(challengeId: String, code: String) = VerifyCodeOutcome.VerifiedForSignUp("secret", 600_000)
        })
        vm.changeEmail("user@example.com"); vm.sendCode(); runCurrent()
        vm.changeCode("123456"); vm.checkCode {}; runCurrent()
        vm.consumeNavigation()
        assertNotNull(vm.signUpGrant)
        assertFalse(vm.uiState.value.toString().contains("secret"))
        clock = 180_000; vm.refreshTimer(); assertTrue(vm.uiState.value.verified)
        clock = 600_000; vm.checkCode {}; runCurrent()
        assertFalse(vm.uiState.value.verified)
        assertFalse(vm.uiState.value.navigationPending)
        assertNull(vm.signUpGrant)
        clock = 0; vm.sendCode(); runCurrent(); vm.changeCode("123456"); vm.checkCode {}; runCurrent()
        vm.changeEmail("other@example.com")
        assertNull(vm.signUpGrant)
        assertFalse(vm.uiState.value.verified)
    }

    @Test fun verification429BlocksRepeatedAttemptsUntilServerRetryAfter() = runTest {
        var attempts = 0
        val vm = model(object : EmailVerificationGateway {
            override suspend fun sendCode(email: String) = SendCodeOutcome.Sent("id", 180)
            override suspend fun verifyCode(challengeId: String, code: String): VerifyCodeOutcome {
                attempts++; return VerifyCodeOutcome.Failed(AuthFailure.RATE_LIMITED, 30)
            }
        })
        vm.changeEmail("user@example.com"); vm.sendCode(); runCurrent()
        vm.changeCode("123456"); vm.checkCode {}; runCurrent()
        vm.checkCode {}; runCurrent(); assertEquals(1, attempts)
        clock = 30_000; vm.refreshTimer(); vm.checkCode {}; runCurrent()
        assertEquals(2, attempts)
    }

    @Test fun signupSaveFailureRetriesPersistenceWithoutCreatingAccountTwice() = runTest {
        var requests = 0
        var saves = 0
        val session = AuthSession("private.jwt.value", AuthSessionSource.BACKEND)
        val vm = SignUpCredentialsViewModel("user@example.com", false,
            SignUpSubmitter { _, _, _, _ -> requests++; SignUpOutcome.Authenticated(session) },
            AuthInputPolicy.DRAFT, { saves++; if (saves == 1) error("disk full") })
            .also { models.put("signup", it) }
        vm.changePassword("abcdefgh"); vm.changeConfirmation("abcdefgh"); vm.changeNickname("가".repeat(50))
        vm.submit(); runCurrent()
        assertFalse(vm.uiState.value.completed)
        assertTrue(vm.uiState.value.canSubmit)
        assertTrue(vm.uiState.value.savingSession)
        assertEquals("", vm.uiState.value.password)
        vm.submit(); runCurrent()
        assertTrue(vm.uiState.value.completed)
        assertEquals(1, requests)
        assertEquals(2, saves)
    }

    @Test fun draftInputLimitsAreSeparatedFromResetAndLoginPolicy() {
        val state = SignUpCredentialsUiState(policy = AuthInputPolicy.DRAFT, password = "a".repeat(64), confirmation = "a".repeat(64), nickname = "가".repeat(50))
        assertTrue(state.canSubmit)
        assertFalse(state.copy(password = "a".repeat(65), confirmation = "a".repeat(65)).canSubmit)
        assertFalse(state.copy(nickname = "가".repeat(51)).canSubmit)
        assertFalse(state.copy(policy = AuthInputPolicy.PREVIEW_RESET).canSubmit)
        assertNull(LoginInputValidator.passwordError("old"))
        assertNotNull(LoginInputValidator.emailError("a".repeat(250) + "@x.com"))
    }

    @Test fun emptyIncompleteOrFailedTermsNeverAllowSignupAndRetryResetsConsent() = runTest {
        var result: TermsOutcome = TermsOutcome.Loaded(emptyList())
        val vm = SignUpTermsViewModel { result }.also { models.put("terms", it) }
        runCurrent(); vm.selectAll(true)
        assertFalse(vm.uiState.value.ready)
        assertNull(vm.beginFlow())
        val service = SignUpTermsDocument("service-id", SignUpTerm.SERVICE, "1.0", "service", "body", true)
        result = TermsOutcome.Loaded(listOf(service))
        vm.load(); runCurrent(); assertFalse(vm.uiState.value.ready)
        result = TermsOutcome.Loaded(listOf(service, service.copy(id = "privacy-id", type = SignUpTerm.PRIVACY)))
        vm.load(); runCurrent(); vm.selectAll(true)
        assertTrue(vm.uiState.value.ready)
        assertTrue(vm.uiState.value.selection.canContinue)
        result = TermsOutcome.Failed(AuthFailure.SERVER)
        vm.load(); runCurrent()
        assertFalse(vm.uiState.value.ready)
        assertFalse(vm.uiState.value.selection.canContinue)
    }
}
