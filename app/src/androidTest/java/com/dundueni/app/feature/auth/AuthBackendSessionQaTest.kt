package com.dundueni.app.feature.auth

import android.app.Activity
import android.util.Base64
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.dundueni.app.MainActivity
import com.dundueni.app.data.auth.*
import org.junit.*
import org.junit.Assert.*

/** Local fixture JWT only; no API server is contacted. Exercises real Android Keystore storage. */
class AuthBackendSessionQaTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val store get() = AuthSessionRuntime.store(context)
    private var previous: AuthSession? = null
    private lateinit var onboarding: AuthQaOnboardingState
    @Before fun prepare() {
        previous = store.read(); store.clear()
        onboarding = AuthQaOnboardingState(context).also { it.skip() }
    }
    @After fun cleanup() {
        instrumentation.runOnMainSync {
            Stage.entries.filter { it != Stage.DESTROYED }.flatMap {
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it).toList()
            }.distinct().forEach(Activity::finish)
        }
        store.clear(); previous?.takeIf { it.isValid() }?.let(store::save)
        onboarding.restore()
    }
    private fun session(completed: Boolean): AuthSession {
        fun encode(text: String) = Base64.encodeToString(text.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val expiry = System.currentTimeMillis() / 1000 + 600
        return AuthSession("${encode("{}")}.${encode("""{"exp":$expiry,"sub":"local-qa"}""")}.signature",
            AuthSessionSource.BACKEND, expiry - 60, completed)
    }
    private fun waitFor(text: String) = compose.waitUntil(10_000) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
    @Test fun backendExpiryAndOnboardingSurviveEncryptedStoreAndActivityRecreation() {
        val sample = session(true)
        store.save(sample)
        assertEquals(sample, AndroidEncryptedAuthSessionStore(context).read())
        assertEquals(sample.serverExpiresAtEpochSeconds, store.read()?.expiresAtEpochSeconds())
        val encrypted = context.getSharedPreferences("auth_session", 0).getString("encrypted_session", "")!!
        assertFalse(encrypted.contains(sample.accessToken))
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor("빠른 검사")
            it.recreate()
            waitFor("빠른 검사")
            compose.onAllNodesWithText("든든이 안내").assertCountEquals(0)
        }
    }
    @Test fun serverFalseOnboardingOverridesLocalCompletedTour() {
        store.save(session(false))
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor("든든이 안내")
            compose.onNodeWithText("1 / 4").assertIsDisplayed()
            it.recreate()
            waitFor("든든이 안내")
            compose.onAllNodesWithText("빠른 검사").assertCountEquals(0)
        }
    }
    @Test fun corruptEncryptedSessionFailsClosed() {
        context.getSharedPreferences("auth_session", 0).edit().putString("encrypted_session", "corrupt:ciphertext").commit()
        assertNull(AndroidEncryptedAuthSessionStore(context).read())
        assertFalse(context.getSharedPreferences("auth_session", 0).contains("encrypted_session"))
    }
}
