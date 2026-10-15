package com.dundueni.app.feature.textinput

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import com.dundueni.app.feature.analysisresult.AnalysisResult
import com.dundueni.app.feature.analysisresult.AnalysisResultActivity
import com.dundueni.app.feature.analysisresult.RiskEvidence
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.feature.analysisresult.AiLikelihood
import com.dundueni.app.ui.theme.DundueniFETheme
import com.google.gson.Gson

/**
 * 텍스트 입력 및 검사 화면을 관리하는 Activity.
 * 검사 실행 시 입력된 텍스트를 담아 분석 결과 화면(AnalysisResultActivity)으로 진입한다.
 */
class TextInputActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DundueniFETheme {
                Box(modifier = Modifier.safeDrawingPadding()) {
                    TextInputScreen(
                        onInspect = { text ->
                            val result = AnalysisResult(
                                id = "text-scan-${System.currentTimeMillis()}",
                                riskLevel = RiskLevel.CAUTION,
                                summary = "모의 분석 결과입니다. 실제 텍스트 분석은 수행되지 않았습니다.",
                                keyFinding = "표시된 위험 단계와 점수는 예시 데이터이며, 입력한 텍스트를 분석한 결과가 아닙니다.",
                                fraudScore = 50.0,
                                aiLikelihood = AiLikelihood.MEDIUM,
                                sourceText = text,
                                evidence = listOf(
                                    RiskEvidence("모의 텍스트 검사", "실제 분석 API를 호출하지 않은 고정 예시 항목입니다."),
                                    RiskEvidence("예시 안내", "표시된 주의 단계와 점수는 입력 내용과 무관한 샘플 값입니다.")
                                ),
                                targetLabel = "입력한 텍스트 내용"
                            )
                            val json = Gson().toJson(result)
                            val intent = AnalysisResultActivity.createIntent(this@TextInputActivity, json)
                            startActivity(intent)
                            finish()
                        },
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}
