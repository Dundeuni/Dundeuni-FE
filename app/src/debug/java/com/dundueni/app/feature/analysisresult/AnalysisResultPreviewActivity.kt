package com.dundueni.app.feature.analysisresult

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dundueni.app.ui.theme.DundueniFETheme

class AnalysisResultPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DundueniFETheme {
                var selected by rememberSaveable { mutableStateOf("SAFE") }
                Column {
                    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp)) {
                        RiskLevel.entries.forEach { level ->
                            FilterChip(selected = selected == level.name, onClick = { selected = level.name },
                                label = { Text("${level.label} 샘플") }, modifier = Modifier.padding(end = 8.dp))
                        }
                    }
                    key(selected) {
                        AnalysisResultRoute(AnalysisResultSamples.forLevel(RiskLevel.valueOf(selected)), onReturn = { finish() }, isDemo = true)
                    }
                }
            }
        }
    }
}

@Preview(name = "PB-05 안전", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun SafeResultPreview() = DundueniFETheme {
    AnalysisResultScreen(AnalysisResultSamples.forLevel(RiskLevel.SAFE), onAction = {})
}

@Preview(name = "PB-05 주의", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun CautionResultPreview() = DundueniFETheme {
    AnalysisResultScreen(AnalysisResultSamples.forLevel(RiskLevel.CAUTION), onAction = {})
}

@Preview(name = "PB-05 위험", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun DangerResultPreview() = DundueniFETheme {
    AnalysisResultScreen(AnalysisResultSamples.forLevel(RiskLevel.DANGER), onAction = {})
}

@Preview(name = "PB-05 작은 화면·큰 글씨", widthDp = 320, heightDp = 720, fontScale = 1.3f, showBackground = true)
@Composable
private fun AccessibleResultPreview() = DundueniFETheme {
    AnalysisResultScreen(AnalysisResultSamples.forLevel(RiskLevel.DANGER), onAction = {})
}
