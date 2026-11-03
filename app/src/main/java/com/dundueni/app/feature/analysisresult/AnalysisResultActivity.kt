package com.dundueni.app.feature.analysisresult

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dundueni.app.ui.theme.DundueniFETheme
import com.dundueni.app.feature.home.returnToHome
import com.dundueni.app.feature.floatingbutton.FloatingButtonManager

class AnalysisResultActivity : ComponentActivity() {
    private val overlayOwner = "text-result:${java.util.UUID.randomUUID()}"

    companion object {
        const val EXTRA_RESULT_JSON = "com.dundueni.app.analysis.RESULT_JSON"
        const val EXTRA_IS_DEMO = "com.dundueni.app.analysis.IS_DEMO"
        const val EXTRA_SUSPEND_FLOATING_BUTTON = "com.dundueni.app.analysis.SUSPEND_FLOATING_BUTTON"

        /** Prefer AnalysisResultScreen(result, onAction) when integrating an existing navigation graph. */
        fun createIntent(
            context: Context,
            resultJson: String,
            isDemo: Boolean = false,
            suspendFloatingButton: Boolean = false,
        ): Intent =
            Intent(context, AnalysisResultActivity::class.java)
                .putExtra(EXTRA_RESULT_JSON, resultJson)
                .putExtra(EXTRA_IS_DEMO, isDemo)
                .putExtra(EXTRA_SUSPEND_FLOATING_BUTTON, suspendFloatingButton)
    }

    override fun onStart() {
        super.onStart()
        if (intent.getBooleanExtra(EXTRA_SUSPEND_FLOATING_BUTTON, false)) {
            FloatingButtonManager.suspendForActivity(overlayOwner)
        }
    }

    override fun onStop() {
        if (intent.getBooleanExtra(EXTRA_SUSPEND_FLOATING_BUTTON, false)) {
            FloatingButtonManager.resumeForActivity(overlayOwner)
        }
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val result = runCatching {
            AnalysisResultJsonAdapter.decode(requireNotNull(intent.getStringExtra(EXTRA_RESULT_JSON)))
        }.getOrNull()
        val isDemo = intent.getBooleanExtra(EXTRA_IS_DEMO, false)
        setContent {
            DundueniFETheme {
                if (result != null) {
                    AnalysisResultRoute(result, onReturn = ::finish, onHome = ::returnToHome, isDemo = isDemo)
                } else {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                        Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("분석 결과를 불러오지 못했어요.", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(12.dp))
                            Text("결과 데이터가 없거나 형식이 올바르지 않아요. 이전 화면에서 다시 확인해주세요.")
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = ::returnToHome) { Text("홈으로 돌아가기") }
                        }
                    }
                }
            }
        }
    }
}
