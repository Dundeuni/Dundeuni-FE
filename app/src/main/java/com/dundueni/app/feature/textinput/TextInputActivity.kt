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
import com.dundueni.app.feature.floatingbutton.FloatingButtonManager
import com.dundueni.app.ui.theme.DundueniFETheme
import com.google.gson.Gson

/**
 * 텍스트 입력 및 검사 화면을 관리하는 Activity.
 * 검사 실행 시 입력된 텍스트를 담아 분석 결과 화면(AnalysisResultActivity)으로 진입한다.
 */
class TextInputActivity : ComponentActivity() {
    private val overlayOwner = "text-input:${java.util.UUID.randomUUID()}"

    override fun onStart() {
        super.onStart()
        FloatingButtonManager.suspendForActivity(overlayOwner)
    }

    override fun onStop() {
        FloatingButtonManager.resumeForActivity(overlayOwner)
        super.onStop()
    }

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
                                summary = "입력하신 텍스트에 대한 분석 결과입니다 (Mock 데이터).",
                                keyFinding = "작성하신 내용에서 주의가 필요한 표현이 감지되었습니다.",
                                fraudScore = 50.0,
                                aiLikelihood = AiLikelihood.MEDIUM,
                                sourceText = text,
                                evidence = listOf(
                                    RiskEvidence("입력 텍스트 점검 (Mock)", "사용자가 직접 입력한 텍스트 내용을 기반으로 사기 및 피싱 위험도를 가상 분석했습니다."),
                                    RiskEvidence("주의 필요 표현", "금전 요구나 개인정보 요청 문구가 있는지 주의 깊게 확인하세요.")
                                ),
                                targetLabel = "입력한 텍스트 내용"
                            )
                            val json = Gson().toJson(result)
                            val intent = AnalysisResultActivity.createIntent(
                                this@TextInputActivity, json, isDemo = true, suspendFloatingButton = true,
                            )
                            startActivity(intent)
                        },
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}
