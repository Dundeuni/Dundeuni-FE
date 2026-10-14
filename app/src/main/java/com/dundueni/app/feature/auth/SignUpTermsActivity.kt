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

class SignUpTermsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
}

private val termsStateSaver = listSaver<TermsUiState, Boolean>(
    save = { listOf(it.service, it.privacy, it.age, it.marketing) },
    restore = { TermsUiState(it[0], it[1], it[2], it[3]) },
)
