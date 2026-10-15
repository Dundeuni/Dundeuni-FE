package com.dundueni.app.feature.linkphone

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
 * 링크 및 전화번호 입력 및 검사 화면을 관리하는 Activity.
 * 검사 실행 시 입력된 링크와 전화번호를 담아 분석 결과 화면(AnalysisResultActivity)으로 진입한다.
 */
class LinkPhoneInputActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DundueniFETheme {
                Box(modifier = Modifier.safeDrawingPadding()) {
                    LinkPhoneInputScreen(
                        onInspect = { link, phone ->
                            val descBuilder = StringBuilder("입력된 URL과 발신자 번호의 패턴을 분석했습니다.")
                            if (phone.isNotBlank()) {
                                descBuilder.append(" (발신 번호: $phone)")
                            }
                            val result = AnalysisResult(
                                id = "link-phone-scan-${System.currentTimeMillis()}",
                                riskLevel = RiskLevel.CAUTION,
                                summary = "입력하신 링크 및 전화번호에 대한 분석 결과입니다.",
                                keyFinding = "제공된 링크나 발신 번호에서 주의가 필요한 항목이 감지되었습니다.",
                                fraudScore = 55.0,
                                aiLikelihood = AiLikelihood.MEDIUM,
                                sourceUrl = link.takeIf { it.isNotBlank() },
                                evidence = listOf(
                                    RiskEvidence("링크 및 전화번호 검증", descBuilder.toString()),
                                    RiskEvidence("발신 번호 및 링크 주의", "공식 기관 사칭 번호나 비정상적인 단축 URL인지 재확인하세요.")
                                ),
                                targetLabel = "입력된 링크 및 전화번호"
                            )
                            val json = Gson().toJson(result)
                            val intent = AnalysisResultActivity.createIntent(this@LinkPhoneInputActivity, json)
                            startActivity(intent)
                        },
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}
