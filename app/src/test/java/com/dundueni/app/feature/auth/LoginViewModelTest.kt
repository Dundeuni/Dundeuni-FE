package com.dundueni.app.feature.auth

import androidx.lifecycle.ViewModelStore
import com.dundueni.app.data.auth.AuthSession
import com.dundueni.app.data.auth.AuthSessionSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }
    private fun model(submitter: LoginSubmitter) = LoginViewModel(submitter).also { store.put("login", it) }

    @Test fun malformedOrMissingInputDoesNotCallApi() = runTest {
        var calls = 0
        val vm = model { _, _ -> calls++; success() }
        vm.login()
        assertNotNull(vm.uiState.value.emailError)
        assertNotNull(vm.uiState.value.passwordError)
        vm.changeEmail("example")
        vm.changePassword("a")
        vm.login()
        runCurrent()
        assertEquals(0, calls)
        assertEquals("이메일 형식을 확인해 주세요.", vm.uiState.value.emailError)
    }

    @Test fun duplicateSubmissionsAreBlockedAndPasswordIsNotTrimmed() = runTest {
        val response = CompletableDeferred<LoginOutcome>()
        var calls = 0
        val vm = model { email, password ->
            calls++
            assertEquals("example@kau.kr", email)
            assertEquals(" secret ", password)
            response.await()
        }
        vm.changeEmail(" example@kau.kr ")
        vm.changePassword(" secret ")
        vm.login()
        vm.login()
        vm.changePassword("replaced")
        vm.togglePasswordVisibility()
        runCurrent()
        assertEquals(1, calls)
        assertEquals(" secret ", vm.uiState.value.password)
        assertTrue(vm.uiState.value.submitting)
        assertFalse(vm.uiState.value.passwordVisible)
        response.complete(LoginOutcome.InvalidCredentials)
        runCurrent()
        assertTrue(vm.uiState.value.credentialsRejected)
        assertFalse(vm.uiState.value.submitting)
        vm.changePassword("corrected")
        assertFalse(vm.uiState.value.credentialsRejected)
        assertNull(vm.uiState.value.message)
    }

    @Test fun successClearsPasswordAndNavigatesOnce() = runTest {
        val vm = model { _, _ -> success() }
        vm.changeEmail("example@kau.kr")
        vm.changePassword("test-password")

        vm.login()
        runCurrent()
        assertTrue(vm.uiState.value.navigationPending)
        vm.consumeNavigation()
        assertFalse(vm.uiState.value.navigationPending)
        assertEquals("", vm.uiState.value.password)
        assertFalse(vm.uiState.value.passwordVisible)
    }

    @Test fun connectionErrorDoesNotBecomeWrongCredentials() = runTest {
        val vm = model { _, _ -> throw java.io.IOException() }
        vm.changeEmail("example@kau.kr")
        vm.changePassword("test-password")
        vm.login()
        runCurrent()
        assertFalse(vm.uiState.value.credentialsRejected)
        assertFalse(vm.uiState.value.submitting)
        assertEquals("연결하지 못했습니다. 다시 시도해 주세요.", vm.uiState.value.message)
    }

    @Test fun storageFailureStaysOnLoginAndCanRetry() = runTest {
        var saves = 0
        val vm = LoginViewModel(LoginSubmitter { _, _ -> success() }, saveSession = {
            if (++saves == 1) throw java.io.IOException("storage unavailable")
        }).also { store.put("login", it) }
        vm.changeEmail("qa@kau.kr")
        vm.changePassword("test-password")
        vm.login()
        runCurrent()
        assertFalse(vm.uiState.value.submitting)
        assertFalse(vm.uiState.value.navigationPending)
        assertFalse(vm.uiState.value.credentialsRejected)
        assertEquals("로그인 정보를 저장하지 못했습니다. 다시 시도해 주세요.", vm.uiState.value.message)
        vm.login()
        runCurrent()
        assertEquals(2, saves)
        assertTrue(vm.uiState.value.navigationPending)
        assertEquals("", vm.uiState.value.password)
    }

    @Test fun pendingNavigationPreventsDuplicateLoginAndSave() = runTest {
        var saves = 0
        val vm = LoginViewModel(LoginSubmitter { _, _ -> success() }, saveSession = { saves++ })
            .also { store.put("login", it) }
        vm.changeEmail("qa@kau.kr")
        vm.changePassword("test-password")
        vm.login()
        runCurrent()
        vm.changePassword("another-password")
        vm.login()
        runCurrent()
        assertEquals(1, saves)
        assertTrue(vm.uiState.value.navigationPending)
        vm.consumeNavigation()
        assertFalse(vm.uiState.value.navigationPending)
    }

    private fun success(): LoginOutcome.Success {
        val exp = System.currentTimeMillis() / 1_000L + 600L
        fun encode(value: String) = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray())
        return LoginOutcome.Success(AuthSession("${encode("{}")}.${encode("{\"exp\":$exp}")}.sig", AuthSessionSource.MOCK))
    }
}
