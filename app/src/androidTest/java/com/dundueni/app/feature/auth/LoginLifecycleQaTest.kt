package com.dundueni.app.feature.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import com.dundueni.app.ui.theme.DundueniFETheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class LoginLifecycleQaTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun successWhileBackgroundedWaitsForResumeAndNavigatesOnce() {
        val response = CompletableDeferred<LoginOutcome>()
        val vm = LoginViewModel(LoginSubmitter { _, _ -> response.await() },
            LoginUiState(email = "qa@kau.kr", password = "ValidPass123!"))
        var navigations = 0
        compose.setContent { DundueniFETheme {
            LoginRoute(vm, { navigations++ }, {}, {}, {}, {})
        } }
        compose.runOnIdle { vm.login() }
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        // Token parsing/storage is covered separately. This test isolates response delivery and lifecycle.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            response.complete(LoginOutcome.Success(com.dundueni.app.data.auth.AuthSession("qa", com.dundueni.app.data.auth.AuthSessionSource.MOCK)))
        }
        compose.waitUntil(5_000) { vm.uiState.value.navigationPending }
        assertEquals(0, navigations)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitUntil(5_000) { navigations == 1 }
        assertFalse(vm.uiState.value.navigationPending)
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
        assertEquals(1, navigations)
    }
}
