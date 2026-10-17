package com.dundueni.app.feature.auth

import android.app.Activity
import android.os.SystemClock
import android.util.Base64
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.lifecycle.Lifecycle
import com.dundueni.app.MainActivity
import com.dundueni.app.data.auth.*
import org.junit.*
import org.junit.Assert.*

/** 실제 Keystore 저장소와 Activity 연결을 사용하는 로컬 샘플 인증 QA. */
class AuthSessionQaTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val store get() = AuthSessionRuntime.store(context)
    private var previous: AuthSession? = null

    @Before fun prepare() { previous = store.read(); store.clear() }
    @After fun cleanUp() {
        instrumentation.runOnMainSync {
            Stage.entries.filter { it != Stage.DESTROYED }.flatMap {
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it).toList()
            }.distinct().forEach(Activity::finish)
        }
        store.clear()
        previous?.takeIf { it.isValid() }?.let(store::save)
    }

    private fun waitFor(text: String) = compose.waitUntil(10_000) {
        runCatching { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
    }

    private fun openAccount() {
        compose.waitUntil(10_000) {
            var ready = false
            instrumentation.runOnMainSync {
                val activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()
                ready = activity is MainActivity && activity.window.decorView.hasWindowFocus() &&
                    activity.window.decorView.rootWindowInsets?.isVisible(android.view.WindowInsets.Type.ime()) != true
            }
            ready
        }
        compose.onNodeWithText("My 쉼터").performClick()
    }

    private fun session(lifetime: Long = 600, source: AuthSessionSource = AuthSessionSource.MOCK): AuthSession {
        fun encode(value: String) = Base64.encodeToString(value.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val expiry = System.currentTimeMillis() / 1000 + lifetime
        return AuthSession("${encode("{}")}.${encode("""{"exp":$expiry,"sub":"qa-local"}""")}.qa", source)
    }

    @Test fun encryptedSessionRestoresAcrossStoreInstancesAndActivityRecreation() {
        val sample = session()
        store.save(sample)
        assertEquals(sample, AndroidEncryptedAuthSessionStore(context).read())
        val encrypted = context.getSharedPreferences("auth_session", 0).getString("encrypted_session", "")!!
        assertFalse(encrypted.contains(sample.accessToken))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitFor("빠른 검사")
            scenario.recreate()
            waitFor("빠른 검사")
            openAccount()
            onView(withText("로그인된 상태입니다.")).inRoot(isDialog()).check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()))
            onView(withText("취소")).inRoot(isDialog()).perform(click())
        }
    }

    @Test fun mockLoginReturnsHomeAndLogoutClearsSessionAndTask() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor("빠른 검사")
            openAccount()
            waitFor("회원가입")
            compose.onNodeWithContentDescription("이메일 계정").performScrollTo().performTextReplacement("qa@kau.kr")
            compose.onNodeWithContentDescription("비밀번호").performScrollTo().performTextReplacement("ValidPass123!")
            compose.onNodeWithContentDescription("비밀번호").performImeAction()
            waitFor("빠른 검사")
            assertEquals(AuthSessionSource.MOCK, store.read()?.source)
            openAccount()
            onView(withText("로그아웃")).inRoot(isDialog()).perform(click())
            waitFor("회원가입")
            assertNull(store.read())
            compose.onAllNodesWithText("빠른 검사").assertCountEquals(0)
        }
    }

    @Test fun expiryOnResumeClearsSessionAndShowsLogin() {
        store.save(session(lifetime = 3))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitFor("빠른 검사")
            scenario.moveToState(Lifecycle.State.CREATED)
            SystemClock.sleep(3_200)
            scenario.moveToState(Lifecycle.State.RESUMED)
            waitFor("회원가입")
            assertNull(store.read())
            compose.onAllNodesWithText("빠른 검사").assertCountEquals(0)
        }
    }

    @Test fun unauthorizedOnBackgroundResumeClearsBackendSession() {
        val sample = session(source = AuthSessionSource.BACKEND)
        store.save(sample)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitFor("빠른 검사")
            scenario.moveToState(Lifecycle.State.CREATED)
            AuthSessionRuntime.onUnauthorized(sample.accessToken)
            scenario.moveToState(Lifecycle.State.RESUMED)
            waitFor("회원가입")
            assertNull(store.read())
        }
    }
}
