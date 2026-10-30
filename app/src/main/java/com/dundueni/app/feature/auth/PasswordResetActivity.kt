package com.dundueni.app.feature.auth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dundueni.app.BuildConfig
import com.dundueni.app.feature.floatingbutton.FloatingButtonManager
import com.dundueni.app.ui.theme.DundueniFETheme

/** One Activity retains both steps across rotation; process recreation requires fresh verification. */
class PasswordResetActivity : ComponentActivity() {
    private val overlayOwner = "password-reset:${java.util.UUID.randomUUID()}"
    override fun onStart() { super.onStart(); FloatingButtonManager.suspendForActivity(overlayOwner) }
    override fun onStop() { FloatingButtonManager.resumeForActivity(overlayOwner); super.onStop() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val demo = BuildConfig.DEBUG
        val provider = ViewModelProvider(this, object : ViewModelProvider.Factory {
            // Both ViewModels share a purpose-specific gateway instance, including after rotation.
            private val gateway: PasswordResetGateway by lazy {
                if (demo) Class.forName("com.dundueni.app.feature.auth.PreviewPasswordResetGateway")
                    .getDeclaredConstructor().newInstance() as PasswordResetGateway
                else UnconnectedPasswordResetGateway
            }
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
                EmailVerificationViewModel::class.java -> EmailVerificationViewModel(gateway)
                PasswordResetViewModel::class.java -> PasswordResetViewModel(gateway)
                else -> error("Unknown reset ViewModel")
            } as T
        })
        val verification = provider[EmailVerificationViewModel::class.java]
        val reset = provider[PasswordResetViewModel::class.java]
        setContent { DundueniFETheme {
            val email by verification.uiState.collectAsStateWithLifecycle()
            val credentials by reset.uiState.collectAsStateWithLifecycle()
            DisposableEffect(verification) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) verification.refreshTimer()
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }
            val back: () -> Unit = {
                if (credentials.settingPassword && !credentials.completed) {
                    reset.returnToVerification()
                    // A new verification is required when leaving the credential step.
                    verification.changeEmail("")
                } else finish()
            }
            BackHandler(onBack = back)
            LaunchedEffect(email.navigationPending) {
                if (email.navigationPending) lifecycle.withResumed {
                    if (verification.uiState.value.navigationPending) {
                        reset.begin(verification.uiState.value.email, verification.resetGrant)
                        verification.consumeNavigation()
                    }
                }
            }
            if (credentials.settingPassword) {
                PasswordResetScreen(credentials, reset::changePassword, reset::changeConfirmation,
                    reset::togglePassword, reset::submit, back, ::finish, demo)
            } else {
                EmailVerificationScreen(email, verification::changeEmail, verification::changeCode,
                    verification::sendCode, { verification.checkCode {} }, back, demo, passwordReset = true)
            }
        } }
    }
}
