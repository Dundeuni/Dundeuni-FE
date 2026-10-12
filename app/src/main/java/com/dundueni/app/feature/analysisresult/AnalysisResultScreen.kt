package com.dundueni.app.feature.analysisresult

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dundueni.app.ui.theme.Danger
import com.dundueni.app.ui.theme.ProtectiveBlue
import java.util.Locale

private val PageBackground = Color(0xFFF7F8FF)
private val SoftBlue = Color(0xFFEEF3FF)
private val SoftRed = Color(0xFFFFE7E5)
private val Ink = Color(0xFF202532)
private val Muted = Color(0xFF646979)

/** Stateless production screen. Supply the completed analysis and route actions in the host. */
@Composable
fun AnalysisResultScreen(
    result: AnalysisResult,
    onAction: (ResultAction) -> Unit,
    modifier: Modifier = Modifier,
    historySaved: Boolean = false,
    isDemo: Boolean = false,
) {
    Scaffold(
        modifier = modifier,
        containerColor = PageBackground,
        topBar = { ResultHeader(onAction) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isDemo) {
                    Surface(color = Color(0xFFFFF2D8), shape = RoundedCornerShape(12.dp)) {
                        Text("디자인 미리보기 · 샘플 데이터", Modifier.fillMaxWidth().padding(12.dp), color = Color(0xFF795700), fontSize = 12.sp)
                    }
                }
                if (result.riskLevel == RiskLevel.SAFE) {
                    ResultPanel { ResultHero(result); TargetRow(result.targetLabel) }
                } else {
                    ResultHero(result)
                }
                when (result.riskLevel) {
                    RiskLevel.SAFE -> {
                        MetricsRow(result)
                        EvidencePanel(result)
                        SafetyTip()
                    }
                    RiskLevel.CAUTION -> {
                        SummaryBanner(result.keyFinding ?: result.summary, urgent = false)
                        ResultPanel {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("다차원 정밀 진단 결과", fontSize = 13.sp, color = Ink)
                                Text("분석 엔진 결과", fontSize = 10.sp, color = Muted)
                            }
                            MetricsRow(result, nested = true)
                        }
                        EvidencePanel(result)
                    }
                    RiskLevel.DANGER -> {
                        SummaryBanner("지금은 절대 돈을 보내지 마세요!", urgent = true)
                        result.sourceText?.let { MessageQuote(it) }
                        MetricsRow(result)
                        Text("* 사기 위험과 AI 생성·변조 가능성은 각각 독립적으로 분석됩니다.", color = Muted, fontSize = 10.sp)
                        EvidencePanel(result)
                    }
                }
                ResultActions(result, historySaved, onAction)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ResultHeader(onAction: (ResultAction) -> Unit) {
    Surface(color = PageBackground) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onAction(ResultAction.RETURN) }, modifier = Modifier.semantics { contentDescription = "뒤로 가기" }) {
                ResultGlyph(ResultIcon.BACK, Ink)
            }
            Box(Modifier.size(24.dp).background(Color(0xFF111B2B), CircleShape), contentAlignment = Alignment.Center) {
                ResultGlyph(ResultIcon.SHIELD, ProtectiveBlue, 14.dp)
            }
            Text("Inspection Result", Modifier.weight(1f).padding(start = 10.dp), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { onAction(ResultAction.RETURN) }, modifier = Modifier.semantics { contentDescription = "결과 화면 닫기" }) {
                ResultGlyph(ResultIcon.CLOSE, Ink, 18.dp)
            }
            IconButton(onClick = { onAction(ResultAction.PROFILE) }, modifier = Modifier.semantics { contentDescription = "계정" }) {
                Box(Modifier.size(32.dp).background(Color.Black, CircleShape), contentAlignment = Alignment.Center) {
                    ResultGlyph(ResultIcon.PROFILE, Color.White, 18.dp)
                }
            }
        }
    }
}

@Composable
private fun ResultHero(result: AnalysisResult) {
    val safe = result.riskLevel == RiskLevel.SAFE
    val caution = result.riskLevel == RiskLevel.CAUTION
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(80.dp).background(if (safe) SoftBlue else Color(0xFFFFDAD6), CircleShape), contentAlignment = Alignment.Center) {
            ResultGlyph(if (caution) ResultIcon.WARNING else ResultIcon.SHIELD, if (safe) ProtectiveBlue else Danger, 34.dp)
        }
        Surface(shape = CircleShape, color = if (safe) Color(0xFFE8EEFF) else SoftRed) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(6.dp).background(if (safe) ProtectiveBlue else Danger, CircleShape))
                Text(when (result.riskLevel) {
                    RiskLevel.SAFE -> "분석 완료 · 안전 판정"
                    RiskLevel.CAUTION -> "주의 단계 경고"
                    RiskLevel.DANGER -> "위험 단계 경고"
                }, fontSize = 11.sp, color = if (safe) Ink else Danger)
            }
        }
        Text(when (result.riskLevel) {
            RiskLevel.SAFE -> "현재는 안전해 보여요"
            RiskLevel.CAUTION -> "조금 더 확인해보세요"
            RiskLevel.DANGER -> "위험할 가능성이 높아요"
        }, color = Ink, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
        Text(result.summary, color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun TargetRow(label: String) {
    Surface(color = SoftBlue, shape = RoundedCornerShape(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResultGlyph(ResultIcon.MESSAGE, Muted, 18.dp)
            Column(Modifier.weight(1f)) {
                Text("분석 대상", fontSize = 10.sp, color = Muted)
                Text(label, fontSize = 12.sp, color = Ink)
            }
        }
    }
}

@Composable
private fun SummaryBanner(message: String, urgent: Boolean) {
    Surface(color = if (urgent) Color(0xFFFFDAD6) else SoftRed, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ResultGlyph(if (urgent) ResultIcon.WARNING else ResultIcon.LINK, Danger, 24.dp)
            Column {
                Text(if (urgent) "긴급 조치 권고" else "점검 핵심 요약", fontSize = 11.sp, color = Danger)
                Text(message, fontSize = if (urgent) 16.sp else 13.sp, lineHeight = 20.sp, fontWeight = if (urgent) FontWeight.Bold else FontWeight.Normal, color = if (urgent) Danger else Ink)
            }
        }
    }
}

@Composable
private fun MessageQuote(message: String) {
    ResultPanel(color = SoftBlue) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ResultGlyph(ResultIcon.MESSAGE, Muted, 16.dp)
            Text("분석된 메시지 내용", color = Muted, fontSize = 11.sp)
        }
        Surface(color = Color.White, shape = RoundedCornerShape(10.dp)) {
            Text("“$message”", Modifier.fillMaxWidth().padding(12.dp), color = Ink, fontSize = 14.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun MetricsRow(result: AnalysisResult, nested: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard("사기 위험도", when (result.fraudLevel) {
            RiskLevel.SAFE -> "낮음"
            RiskLevel.CAUTION -> "주의"
            RiskLevel.DANGER -> "높음"
            null -> "정보 없음"
        }, result.fraudScore, if (result.fraudLevel == null || result.fraudLevel == RiskLevel.SAFE) ProtectiveBlue else Danger, ResultIcon.SHIELD,
            Modifier.weight(1f), nested)
        MetricCard("AI 생성·변조", result.aiLikelihood.label, result.aiScore, ProtectiveBlue, ResultIcon.AI,
            Modifier.weight(1f), nested)
    }
}

@Composable
private fun MetricCard(title: String, label: String, score: Double?, tint: Color, icon: ResultIcon, modifier: Modifier, nested: Boolean) {
    Surface(modifier = modifier, color = if (nested) SoftBlue else Color.White, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), fontSize = 11.sp, color = Muted)
                ResultGlyph(icon, tint, 16.dp)
            }
            Text(label, color = tint, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(score?.let { "분석 점수 ${formatScore(it)}%" } ?: "점수 정보 없음", fontSize = 10.sp, color = Muted)
            if (score != null) {
                LinearProgressIndicator(progress = { (score / 100).toFloat() }, modifier = Modifier.fillMaxWidth().height(5.dp), color = tint, trackColor = Color(0xFFE5EAF5))
            }
        }
    }
}

internal fun formatScore(score: Double): String = if (score % 1.0 == 0.0) score.toInt().toString() else String.format(Locale.ROOT, "%.1f", score)

@Composable
private fun EvidencePanel(result: AnalysisResult) {
    val safe = result.riskLevel == RiskLevel.SAFE
    ResultPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultGlyph(if (safe) ResultIcon.CHECK else if (result.riskLevel == RiskLevel.DANGER) ResultIcon.SHIELD else ResultIcon.SEARCH,
                if (result.riskLevel == RiskLevel.DANGER) Danger else Ink, 18.dp)
            Text(when (result.riskLevel) {
                RiskLevel.SAFE -> "세부 항목 점검 내역"
                RiskLevel.CAUTION -> "발견된 주의 사항"
                RiskLevel.DANGER -> "왜 위험한가요?"
            }, Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text("${result.evidence.size}건", color = Muted, fontSize = 11.sp)
        }
        if (result.evidence.isEmpty()) {
            Text("세부 근거가 제공되지 않았어요. 요약과 대응 행동을 확인해주세요.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
        }
        result.evidence.forEachIndexed { index, item ->
            Surface(color = if (safe) Color.White else SoftBlue, shape = RoundedCornerShape(10.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = if (safe) 0.dp else 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(26.dp).background(if (safe) SoftBlue else SoftRed, CircleShape), contentAlignment = Alignment.Center) {
                        if (safe) ResultGlyph(ResultIcon.CHECK, ProtectiveBlue, 16.dp)
                        else Text("${index + 1}", fontSize = 12.sp, color = Danger, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(item.title, color = Ink, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                        Text(item.description, color = Muted, fontSize = 12.sp, lineHeight = 19.sp)
                    }
                }
            }
            if (safe && index < result.evidence.lastIndex) HorizontalDivider(color = SoftBlue)
        }
    }
}

@Composable
private fun SafetyTip() {
    ResultPanel(color = SoftBlue) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ResultGlyph(ResultIcon.TIP, Ink, 20.dp)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("든든이 보안 수칙", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("안전 판정이어도 개인정보나 비밀번호, 계좌 인증번호를 입력하라는 요청은 항상 주의하세요. 공식 창구를 통해 확인하는 것이 가장 안전합니다.", color = Muted, fontSize = 12.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun ResultActions(result: AnalysisResult, historySaved: Boolean, onAction: (ResultAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (result.riskLevel) {
            RiskLevel.SAFE -> {
                ActionButton("원래 화면으로 돌아가기", ResultIcon.BACK, Color.Black, Color.White) { onAction(ResultAction.RETURN) }
                ActionButton(if (historySaved) "검사 결과가 기록에 보관되었어요" else "검사 결과 기록에 보관하기", ResultIcon.SAVE,
                    Color(0xFFE9EEFC), Ink, enabled = !historySaved) { onAction(ResultAction.SAVE_HISTORY) }
            }
            RiskLevel.CAUTION -> {
                ActionButton("링크 열지 않고 도메인만 미리보기", ResultIcon.SEARCH, ProtectiveBlue, Color.White,
                    enabled = result.sourceUrl != null) { onAction(ResultAction.PREVIEW_DOMAIN) }
                if (result.sourceUrl == null) Text("확인할 링크가 제공되지 않았어요.", color = Muted, fontSize = 11.sp)
                ActionButton("가족·지인에게 물어보기", ResultIcon.FAMILY, Color(0xFFE4E9F7), Ink) { onAction(ResultAction.ASK_FAMILY) }
                ReturnLink(onAction)
            }
            RiskLevel.DANGER -> {
                ActionButton("가족에게 직접 전화해 확인하기", ResultIcon.PHONE, Color.Black, Color.White) { onAction(ResultAction.CALL_FAMILY) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAction(ResultAction.SHARE) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(10.dp)) {
                        ResultGlyph(ResultIcon.SHARE, ProtectiveBlue, 15.dp)
                        Text("결과 가족에게 공유", Modifier.padding(start = 5.dp), fontSize = 11.sp, color = Ink)
                    }
                    OutlinedButton(onClick = { onAction(ResultAction.REPORT) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(10.dp)) {
                        ResultGlyph(ResultIcon.PHONE, Danger, 15.dp)
                        Text("신고 (112/118)", Modifier.padding(start = 5.dp), fontSize = 11.sp, color = Ink)
                    }
                }
                ReturnLink(onAction)
            }
        }
    }
}

@Composable
private fun ReturnLink(onAction: (ResultAction) -> Unit) {
    TextButton(onClick = { onAction(ResultAction.RETURN) }, modifier = Modifier.fillMaxWidth()) {
        Text("원래 화면으로 돌아가기", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun ActionButton(text: String, icon: ResultIcon, background: Color, foreground: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = background, contentColor = foreground), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        ResultGlyph(icon, if (enabled) foreground else Muted, 16.dp)
        Text(text, Modifier.padding(start = 8.dp), fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun ResultPanel(color: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = color, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
