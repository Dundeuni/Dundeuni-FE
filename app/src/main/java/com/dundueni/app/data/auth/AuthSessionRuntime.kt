package com.dundueni.app.data.auth

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small process-wide bridge used by app entry points and OkHttp's worker thread. */
object AuthSessionRuntime {
    @Volatile private var store: AuthSessionStore? = null
    private val unauthorizedRevisionMutable = MutableStateFlow(0L)
    val unauthorizedRevision: StateFlow<Long> = unauthorizedRevisionMutable.asStateFlow()

    fun store(context: Context): AuthSessionStore = store ?: synchronized(this) {
        store ?: AndroidEncryptedAuthSessionStore(context.applicationContext).also { store = it }
    }

    fun onUnauthorized(sessionToken: String) {
        val sessionStore = store ?: return
        if (sessionStore.read()?.let { it.source == AuthSessionSource.BACKEND && it.accessToken == sessionToken } == true) {
            sessionStore.clear()
            unauthorizedRevisionMutable.value += 1L
        }
    }

    fun onExpired(session: AuthSession) {
        val sessionStore = store ?: return
        if (sessionStore.read()?.accessToken == session.accessToken) {
            sessionStore.clear()
            unauthorizedRevisionMutable.value += 1L
        }
    }
}
