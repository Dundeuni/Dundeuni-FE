package com.dundueni.app.feature.phone

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
import java.util.UUID

class PhoneInputActivity : ComponentActivity() {
    private val overlayOwner = "phone-input:${UUID.randomUUID()}"

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
                    PhoneInputScreen(
                        onInspect = { phone ->
                            val result = AnalysisResult(
                                id = "phone-scan-${System.currentTimeMillis()}",
                                riskLevel = RiskLevel.CAUTION,
                                summary = "입력하신 전화번호에 대한 분석 결과입니다 (Mock 데이터).",
                                keyFinding = "발신자 번호(발신처)의 신고 이력 및 스팸 여부를 가상 조회했습니다.",
                                fraudScore = 50.0,
                                aiLikelihood = AiLikelihood.UNKNOWN,
                                evidence = listOf(
                                    RiskEvidence("발신 번호 조회 (Mock)", "입력된 전화번호($phone)가 사기 신고 및 스미싱 의심 번호 데이터베이스와 가상 대조되었습니다."),
                                    RiskEvidence("사칭 주의", "공공기관이나 금융사 번호로 위장한 발신 번호인지 확인이 필요합니다.")
                                ),
                                targetLabel = "입력한 전화번호"
                            )
                            val json = Gson().toJson(result)
                            val intent = AnalysisResultActivity.createIntent(
                                this@PhoneInputActivity, json, isDemo = true, suspendFloatingButton = true,
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
