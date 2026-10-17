package com.dundueni.app.feature.url

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

class UrlInputActivity : ComponentActivity() {
    private val overlayOwner = "url-input:${UUID.randomUUID()}"

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
                    UrlInputScreen(
                        onInspect = { url ->
                            val result = AnalysisResult(
                                id = "url-scan-${System.currentTimeMillis()}",
                                riskLevel = RiskLevel.CAUTION,
                                summary = "입력하신 웹 링크(URL)에 대한 분석 결과입니다 (Mock 데이터).",
                                keyFinding = "제공된 링크 주소의 도메인 안전성을 가상 검사했습니다.",
                                fraudScore = 60.0,
                                aiLikelihood = AiLikelihood.MEDIUM,
                                sourceUrl = url,
                                evidence = listOf(
                                    RiskEvidence("웹 링크 안전성 검증 (Mock)", "입력된 URL이 피싱 및 스미싱 악성 도메인 목록과 가상 대조되었습니다."),
                                    RiskEvidence("단축 URL 및 리디렉션 주의", "실제 연결되는 목적지 주소가 변조되었는지 확인하세요.")
                                ),
                                targetLabel = "입력한 웹 링크"
                            )
                            val json = Gson().toJson(result)
                            val intent = AnalysisResultActivity.createIntent(
                                this@UrlInputActivity, json, isDemo = true, suspendFloatingButton = true,
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
