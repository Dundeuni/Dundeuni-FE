package com.dundueni.app.feature.phoneinput

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
 * 전화번호 입력 및 검사 화면을 관리하는 Activity.
 * 검사 실행 시 서버 API 미연동 상태임을 사용자에게 명확히 알린다.
 */
class PhoneInputActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DundueniFETheme {
                Box(modifier = Modifier.safeDrawingPadding()) {
                    PhoneInputScreen(
                        onInspect = { phoneNumber ->
                            Toast.makeText(
                                this@PhoneInputActivity,
                                "전화번호 검사 API가 아직 연동되지 않았습니다. (서버 API 준비 중)",
                                Toast.LENGTH_SHORT
                            ).show()

                            // 서버 미연동 상태임을 명시한 UI 확인용 결과 생성 (임의의 고정 위험 점수 생성 금지)
                            val result = AnalysisResult(
                                id = "phone-scan-${System.currentTimeMillis()}",
                                riskLevel = RiskLevel.SAFE,
                                summary = "[서버 API 미연동] 현재 전화번호 검사 API가 연동되지 않아 UI 확인용으로 결과가 표시됩니다.",
                                keyFinding = "전화번호 분석 서버 API가 아직 구현/연동되지 않은 상태입니다.",
                                fraudScore = null,
                                aiLikelihood = AiLikelihood.UNKNOWN,
                                aiScore = null,
                                sourceText = "입력한 전화번호: $phoneNumber",
                                evidence = listOf(
                                    RiskEvidence(
                                        "전화번호 검사 요청",
                                        "사용자가 입력한 번호($phoneNumber)를 기반으로 검사를 요청했습니다. (현재 서버 API 미연동)"
                                    )
                                ),
                                targetLabel = "입력한 전화번호 ($phoneNumber)"
                            )
                            val json = Gson().toJson(result)
                            val intent = AnalysisResultActivity.createIntent(this@PhoneInputActivity, json)
                            startActivity(intent)
                        },
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}
