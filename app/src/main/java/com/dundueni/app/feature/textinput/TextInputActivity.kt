package com.dundueni.app.feature.textinput

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import com.dundueni.app.feature.analysisresult.AiLikelihood
import com.dundueni.app.feature.analysisresult.AnalysisResult
import com.dundueni.app.feature.analysisresult.AnalysisResultActivity
import com.dundueni.app.feature.analysisresult.RiskEvidence
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.ui.theme.DundueniFETheme
import com.google.gson.Gson

/**
 * 텍스트 입력 및 검사 화면을 관리하는 Activity.
 * 검사 실행 시 입력된 텍스트를 담아 분석 결과 화면(AnalysisResultActivity)으로 진입한다.
 * 현재 서버 API 미연동 상태임을 사용자에게 명확히 알린다.
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
                            Toast.makeText(
                                this@TextInputActivity,
                                "텍스트 검사 API가 아직 연동되지 않았습니다. (서버 API 준비 중)",
                                Toast.LENGTH_SHORT
                            ).show()

                            // 서버 미연동 상태임을 명시한 UI 확인용 결과 생성 (고정 50점 등의 임의 판정 제거)
                            val result = AnalysisResult(
                                id = "text-scan-${System.currentTimeMillis()}",
                                riskLevel = RiskLevel.SAFE,
                                summary = "[서버 API 미연동] 현재 텍스트 검사 API가 연동되지 않아 UI 확인용으로 결과가 표시됩니다.",
                                keyFinding = "텍스트 분석 서버 API가 아직 구현/연동되지 않은 상태입니다.",
                                fraudScore = null,
                                aiLikelihood = AiLikelihood.UNKNOWN,
                                aiScore = null,
                                sourceText = text,
                                evidence = listOf(
                                    RiskEvidence(
                                        "텍스트 검사 요청",
                                        "사용자가 직접 입력한 텍스트 내용 기반 검사 요청입니다. (현재 서버 API 미연동)"
                                    )
                                ),
                                targetLabel = "입력한 텍스트 내용"
                            )
                            val json = Gson().toJson(result)
                            val intent = AnalysisResultActivity.createIntent(this@TextInputActivity, json)
                            startActivity(intent)
                        },
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}
