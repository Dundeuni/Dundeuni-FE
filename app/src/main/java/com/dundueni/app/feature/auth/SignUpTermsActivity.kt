package com.dundueni.app.feature.auth

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.dundueni.app.ui.theme.DundueniFETheme
import com.dundueni.app.BuildConfig
import com.dundueni.app.data.auth.AuthDependencies
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier

class SignUpTermsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val demo = BuildConfig.DEBUG && intent.getBooleanExtra(EmailVerificationActivity.EXTRA_DEMO, false)
        if (!demo) {
            showHttpTerms()
            return
        }
        setContent {
            DundueniFETheme {
                var state by rememberSaveable(stateSaver = termsStateSaver) { mutableStateOf(TermsUiState()) }
                var detail by rememberSaveable { mutableStateOf<String?>(null) }
                SignUpTermsScreen(
                    state = state,
                    onTermChange = { term, selected -> state = state.setSelected(term, selected) },
                    onAllChange = { state = state.setAll(it) },
                    onShowTerm = { detail = it.title },
                    onBack = ::finish,
                    onNext = {
                        if (state.canContinue) startActivity(Intent(this, EmailVerificationActivity::class.java).apply {
                            putExtra(EmailVerificationActivity.EXTRA_DEMO, intent.getBooleanExtra(EmailVerificationActivity.EXTRA_DEMO, false))
                            putExtra("marketing_agreed", state.marketing)
                        })
                    },
                )
                detail?.let { title ->
                    AlertDialog(
                        onDismissRequest = { detail = null },
                        title = { Text(title) },
                        text = { Text("약관 전문은 아직 제공되지 않았습니다. 등록 후 이 화면에서 확인할 수 있습니다.") },
                        confirmButton = { TextButton(onClick = { detail = null }) { Text("확인") } },
                    )
                }
            }
        }
    }

    private fun showHttpTerms() {
        val model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SignUpTermsViewModel(AuthDependencies.termsGateway()) as T
            }
        })[SignUpTermsViewModel::class.java]
        setContent { DundueniFETheme {
            val loaded by model.uiState.collectAsStateWithLifecycle()
            var detail by remember { mutableStateOf<SignUpTermsDocument?>(null) }
            SignUpTermsScreen(
                state = loaded.selection, onTermChange = model::select, onAllChange = model::selectAll,
                onShowTerm = { type -> detail = loaded.terms.find { it.type == type } }, onBack = ::finish,
                onNext = {
                    model.beginFlow()?.let { id -> startActivity(Intent(this, EmailVerificationActivity::class.java)
                        .putExtra(SignUpFlowMemory.EXTRA_FLOW_ID, id)) }
                },
                documents = loaded.terms, ready = loaded.ready && !loaded.loading,
                message = if (loaded.loading) "약관을 불러오고 있습니다." else loaded.message,
                onRetry = if (!loaded.loading) model::load else null,
            )
            detail?.let { document -> AlertDialog(
                onDismissRequest = { detail = null }, title = { Text(document.title) },
                text = { Text(document.content, Modifier.verticalScroll(rememberScrollState())) },
                confirmButton = { TextButton(onClick = { detail = null }) { Text("확인") } },
            ) }
        } }
    }
}

private val termsStateSaver = listSaver<TermsUiState, Boolean>(
    save = { listOf(it.service, it.privacy, it.age, it.marketing) },
    restore = { TermsUiState(it[0], it[1], it[2], it[3]) },
)
