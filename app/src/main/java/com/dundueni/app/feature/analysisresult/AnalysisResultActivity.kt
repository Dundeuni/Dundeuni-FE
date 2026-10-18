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

class AnalysisResultActivity : ComponentActivity() {
    companion object {
        const val EXTRA_RESULT_JSON = "com.dundueni.app.analysis.RESULT_JSON"

        /** Prefer AnalysisResultScreen(result, onAction) when integrating an existing navigation graph. */
        fun createIntent(context: Context, resultJson: String): Intent =
            Intent(context, AnalysisResultActivity::class.java).putExtra(EXTRA_RESULT_JSON, resultJson)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val result = runCatching {
            AnalysisResultJsonAdapter.decode(requireNotNull(intent.getStringExtra(EXTRA_RESULT_JSON)))
        }.getOrNull()
        setContent {
            DundueniFETheme {
                if (result != null) {
                    AnalysisResultRoute(result, onReturn = { finish() })
                } else {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                        Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("분석 결과를 불러오지 못했어요.", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(12.dp))
                            Text("결과 데이터가 없거나 형식이 올바르지 않아요. 이전 화면에서 다시 확인해주세요.")
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = { finish() }) { Text("이전 화면으로 돌아가기") }
                        }
                    }
                }
            }
        }
    }
}
