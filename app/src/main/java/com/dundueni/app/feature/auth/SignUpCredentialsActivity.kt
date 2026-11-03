package com.dundueni.app.feature.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dundueni.app.BuildConfig
import com.dundueni.app.MainActivity
import com.dundueni.app.data.auth.AuthDependencies
import com.dundueni.app.data.auth.AuthSessionRuntime
import com.dundueni.app.ui.theme.DundueniFETheme

class SignUpCredentialsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val email = intent.getStringExtra(EXTRA_EMAIL)
        if (email == null || LoginInputValidator.emailError(email) != null) { finish(); return }
        val demo = BuildConfig.DEBUG && intent.getBooleanExtra(EmailVerificationActivity.EXTRA_DEMO, false)
        val flowId = intent.getStringExtra(SignUpFlowMemory.EXTRA_FLOW_ID)
        if (!demo && !SignUpFlowMemory.contains(flowId)) {
            startActivity(Intent(this, SignUpTermsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            finish()
            return
        }
        val submitter = if (demo) Class.forName("com.dundueni.app.feature.auth.PreviewSignUpSubmitter")
            .getDeclaredConstructor().newInstance() as SignUpSubmitter
        else AuthDependencies.signUpSubmitter(flowId)
        val viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == SignUpCredentialsViewModel::class.java)
                @Suppress("UNCHECKED_CAST")
                return SignUpCredentialsViewModel(email, intent.getBooleanExtra("marketing_agreed", false), submitter,
                    policy = if (demo) AuthInputPolicy.PREVIEW_RESET else AuthInputPolicy.DRAFT,
                    saveSession = AuthSessionRuntime.store(applicationContext)::save) as T
            }
        })[SignUpCredentialsViewModel::class.java]
        setContent { DundueniFETheme {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            SignUpCredentialsScreen(state, viewModel::changePassword, viewModel::changeConfirmation, viewModel::changeNickname,
                viewModel::togglePassword, viewModel::submit, ::finish,
                onHome = {
                    startActivity(Intent(this, MainActivity::class.java).addFlags(
                        if (demo) Intent.FLAG_ACTIVITY_CLEAR_TOP else Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                    finish()
                },
                onProfile = { Toast.makeText(this, "프로필·보호자 설정 기능을 준비 중입니다.", Toast.LENGTH_SHORT).show() }, demo = demo)
        } }
    }
    companion object { const val EXTRA_EMAIL = "verified_signup_email" }
}
