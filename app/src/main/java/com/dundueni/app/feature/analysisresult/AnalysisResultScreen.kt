package com.dundueni.app.feature.analysisresult

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import com.dundueni.app.R
import com.dundueni.app.ui.theme.LocalEasyMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dundueni.app.ui.theme.Danger
import com.dundueni.app.ui.theme.ProtectiveBlue
import com.dundueni.app.ui.theme.BrandInk
import com.dundueni.app.ui.theme.Caution
import com.dundueni.app.ui.theme.CautionInk
import com.dundueni.app.ui.theme.PageBackground as AppPageBackground
import com.dundueni.app.ui.theme.PrimaryMidnight
import com.dundueni.app.ui.theme.Safe
import com.dundueni.app.ui.theme.SafeInk
import com.dundueni.app.ui.theme.SecondaryInk
import com.dundueni.app.ui.theme.SoftBlue as AppSoftBlue
import com.dundueni.app.ui.theme.SoftCaution
import com.dundueni.app.ui.theme.SoftDanger
import com.dundueni.app.ui.theme.SoftSafe
import com.dundueni.app.feature.history.HistoryType
import java.util.Locale

private val PageBackground = AppPageBackground
private val SoftBlue = AppSoftBlue
private val SoftRed = SoftDanger
private val Ink = BrandInk
private val Muted = SecondaryInk
private val CardOutline = Color(0xFFEDF0F8)

/** The host supplies the verdict, evidence and actions; this screen only presents them. */
@Composable
fun AnalysisResultScreen(
    result: AnalysisResult,
    onAction: (ResultAction) -> Unit,
    modifier: Modifier = Modifier,
    historySaved: Boolean = false,
    isDemo: Boolean = false,
    screenCropDecision: String? = null,
    imageAnalysisError: String? = null,
    screenCropError: String? = null,
) {
    Scaffold(modifier = modifier, containerColor = PageBackground, topBar = { ResultHeader(onAction) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (result.historyType == HistoryType.SCREEN &&
                    (screenCropDecision != null || imageAnalysisError != null || screenCropError != null)) {
                    ResultPanel {
                        Text("이미지 생성·변조 분석", fontWeight = FontWeight.SemiBold, color = Ink)
                        imageAnalysisError?.let { Text("분석 결과를 표시할 수 없습니다: $it", color = Muted) }
                        if (imageAnalysisError == null) Text("분석 완료", color = Muted)
                    }
                }
                if (isDemo) {
                    Surface(color = Color(0xFFFFF2D8), shape = RoundedCornerShape(12.dp)) {
                        Text("디자인 미리보기 · 샘플 데이터", Modifier.fillMaxWidth().padding(12.dp), color = Color(0xFF795700), fontSize = 12.sp)
                    }
                }
                if (!LocalEasyMode.current) result.sourceText?.let { MessageQuote(it) }
                if (LocalEasyMode.current) {
                    EasyResultContent(result, isDemo)
                } else when (result.riskLevel) {
                    RiskLevel.SAFE -> {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(Color.White, Color(0xFFF0F3FF), Color.White)))
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            ResultHero(result)
                            TargetRow(result.targetLabel)
                        }
                        MetricsRow(result)
                        EvidencePanel(result, isDemo)
                        if (isDemo) SafetyTip()
                    }
                    RiskLevel.CAUTION -> {
                        ResultHero(result)
                        Spacer(Modifier.height(8.dp))
                        SummaryBanner(if (isDemo) result.keyFinding ?: result.summary else result.summary, urgent = false)
                        ResultPanel {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("다차원 정밀 진단 결과", fontSize = 13.sp, color = Ink)
                                Text("분석 엔진 결과", fontSize = 10.sp, color = Muted)
                            }
                            MetricsRow(result, nested = true)
                        }
                        EvidencePanel(result, isDemo)
                    }
                    RiskLevel.DANGER -> {
                        ResultHero(result)
                        SummaryBanner(if (isDemo) "지금은 절대 돈을 보내지 마세요!" else result.summary, urgent = true)
                        Spacer(Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricsRow(result)
                            Text("* 사기 위험과 AI 생성·변조 가능성은 각각 독립적으로 분석됩니다.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
                        }
                        EvidencePanel(result, isDemo)
                    }
                    RiskLevel.UNKNOWN -> {
                        ResultHero(result)
                        UnclassifiedNotice()
                        MetricsRow(result)
                        EvidencePanel(result, isDemo)
                    }
                }
                if (!isDemo) {
                    SafetyTip(result.guidance)
                }
                if (result.historyType == HistoryType.SCREEN &&
                    (screenCropDecision != null || screenCropError != null)) {
                    ResultPanel {
                        Text("화면 문자 피싱 분석", fontWeight = FontWeight.SemiBold, color = Ink)
                        if (screenCropDecision != null) {
                            Text(screenCropDecision, color = Ink)
                            Text("AI 분류 결과이며 사기 확률이나 확정 위험도가 아닙니다.", color = Muted, fontSize = 12.sp)
                        } else {
                            Text("분석 결과를 표시할 수 없습니다: ${screenCropError ?: "응답 오류"}", color = Muted)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                ResultActions(result, historySaved, onAction)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ResultHeader(onAction: (ResultAction) -> Unit) {
    Surface(color = PageBackground, shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onAction(ResultAction.RETURN) }, modifier = Modifier.semantics { contentDescription = "뒤로 가기" }) {
                ResultGlyph(ResultIcon.BACK, Ink, 22.dp)
            }
            Box(Modifier.size(24.dp).background(Color(0xFF111B2B), CircleShape), contentAlignment = Alignment.Center) {
                ResultGlyph(ResultIcon.SHIELD, ProtectiveBlue, 14.dp)
            }
            Text("결과 화면", Modifier.weight(1f).padding(start = 10.dp), color = Ink, fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = { onAction(ResultAction.RETURN) }, modifier = Modifier.semantics { contentDescription = "결과 화면 닫기" }) {
                ResultGlyph(ResultIcon.CLOSE, Ink, 18.dp)
            }
            IconButton(onClick = { onAction(ResultAction.PROFILE) }, modifier = Modifier.semantics { contentDescription = "계정" }) {
                Box(Modifier.size(32.dp).background(Color.Black, CircleShape), contentAlignment = Alignment.Center) {
                    ResultGlyph(ResultIcon.PROFILE, Color.White, 16.dp)
                }
            }
        }
    }
}

@Composable
private fun ResultHero(result: AnalysisResult) {
    if (result.riskLevel == RiskLevel.UNKNOWN) {
        val hasTextDecision = result.historyType == HistoryType.TEXT && result.keyFinding != null
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(80.dp).background(SoftBlue, CircleShape), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.dundeuni_mascot_search), null, Modifier.size(80.dp))
            }
            Surface(color = Color(0xFFE8EEFF), shape = CircleShape) {
                Text(if (hasTextDecision) "분석 완료 · 텍스트 판정" else "분석 완료 · 판정 정보 없음",
                    Modifier.padding(horizontal = 12.dp, vertical = 5.dp), color = Ink, fontSize = 11.sp)
            }
            Text(if (hasTextDecision) result.keyFinding!! else "결과 정보를 확인해주세요",
                color = Ink, fontSize = 24.sp, lineHeight = 32.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(result.summary, color = Muted, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
        }
        return
    }
    val safe = result.riskLevel == RiskLevel.SAFE
    val statusColor = when (result.riskLevel) {
        RiskLevel.SAFE -> Safe
        RiskLevel.CAUTION -> Caution
        RiskLevel.DANGER -> Danger
        RiskLevel.UNKNOWN -> ProtectiveBlue
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(80.dp).background(when (result.riskLevel) {
                RiskLevel.SAFE -> SoftSafe
                RiskLevel.CAUTION -> SoftCaution
                else -> SoftRed
            }, CircleShape), contentAlignment = Alignment.Center) {
                Image(painterResource(when (result.riskLevel) {
                    RiskLevel.SAFE -> R.drawable.dundeuni_mascot_default
                    RiskLevel.CAUTION -> R.drawable.dundeuni_mascot_suspicious
                    RiskLevel.DANGER -> R.drawable.dundeuni_mascot_danger
                    RiskLevel.UNKNOWN -> R.drawable.dundeuni_mascot_search
                }), null, Modifier.size(80.dp))
            }
            if (!safe) {
                Surface(modifier = Modifier.align(Alignment.TopEnd), color = statusColor, shape = CircleShape, border = BorderStroke(2.dp, PageBackground)) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                        ResultGlyph(ResultIcon.ALERT, if (result.riskLevel == RiskLevel.CAUTION) Ink else Color.White, 10.dp)
                        Text(result.riskLevel.label, color = if (result.riskLevel == RiskLevel.CAUTION) Ink else Color.White,
                            fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (result.riskLevel != RiskLevel.DANGER) {
            Spacer(Modifier.height(12.dp))
            Surface(shape = CircleShape, color = if (safe) SoftSafe else SoftCaution) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(6.dp).background(statusColor, CircleShape))
                    Text(if (safe) "보안 정밀 분석 완료" else "주의 단계 경고", fontSize = 11.sp, lineHeight = 15.sp, color = if (safe) Ink else CautionInk)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(when (result.riskLevel) {
            RiskLevel.SAFE -> "현재는 안전해 보여요"
            RiskLevel.CAUTION -> "조금 더 확인해보세요"
            RiskLevel.DANGER -> "위험할 가능성이 높아요"
            RiskLevel.UNKNOWN -> "결과 정보를 확인해주세요"
        }, color = Ink, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(result.summary, color = Muted, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TargetRow(label: String) {
    Surface(color = SoftBlue, shape = RoundedCornerShape(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(30.dp).background(Color(0xFFE3E8F5), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                ResultGlyph(ResultIcon.MESSAGE, Muted, 17.dp)
            }
            Column(Modifier.weight(1f)) {
                Text("분석 대상", fontSize = 10.sp, lineHeight = 15.sp, color = Muted)
                Text(label, fontSize = 12.sp, lineHeight = 18.sp, color = Ink)
            }
        }
    }
}

@Composable
private fun SummaryBanner(message: String, urgent: Boolean) {
    val tint = if (urgent) Danger else CautionInk
    Surface(color = if (urgent) SoftRed else SoftCaution, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (urgent) ResultGlyph(ResultIcon.WARNING, Danger, 23.dp)
            else Box(Modifier.size(34.dp).background(SoftCaution, CircleShape), contentAlignment = Alignment.Center) {
                ResultGlyph(ResultIcon.LINK, tint, 20.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(if (urgent) "긴급 조치 권고" else "점검 핵심 요약", fontSize = 11.sp, lineHeight = 15.sp, color = tint)
                Text(message, fontSize = if (urgent) 16.sp else 14.sp, lineHeight = 21.sp,
                    fontWeight = if (urgent) FontWeight.Bold else FontWeight.Medium, color = if (urgent) Danger else Ink)
            }
        }
    }
}

@Composable
private fun UnclassifiedNotice() {
    Surface(color = SoftBlue, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("최종 위험 판정 미확정", fontSize = 14.sp, color = Ink, fontWeight = FontWeight.SemiBold)
            Text("사기 위험을 안전·주의·위험으로 판정할 정보는 아직 제공되지 않았어요.", fontSize = 12.sp, color = Muted, lineHeight = 19.sp)
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
    val fraudLabel = when (result.fraudLevel) {
        RiskLevel.SAFE -> "낮음"
        RiskLevel.CAUTION -> "주의"
        RiskLevel.DANGER -> "높음"
        RiskLevel.UNKNOWN -> "판정 미확정"
        null -> "정보 없음"
    }
    val fraudTint = when (result.fraudLevel) {
        RiskLevel.SAFE -> SafeInk
        RiskLevel.CAUTION -> CautionInk
        RiskLevel.DANGER -> Danger
        else -> ProtectiveBlue
    }
    val danger = result.riskLevel == RiskLevel.DANGER
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricCard(if (danger) "사기 위험" else "사기 위험도", fraudLabel, result.fraudScore, fraudTint,
            if (danger) ResultIcon.ALERT else ResultIcon.SHIELD_OUTLINE, Modifier.weight(1f), nested, result.riskLevel,
            isAi = false)
        MetricCard(if (result.riskLevel == RiskLevel.SAFE) "AI 변조 가능성" else "AI 생성·변조", result.aiRiskLabel ?: result.aiLikelihood.label,
            result.aiScore, if (danger) Ink else ProtectiveBlue, ResultIcon.AI, Modifier.weight(1f), nested, result.riskLevel,
            isAi = true)
    }
}

@Composable
private fun MetricCard(
    title: String, label: String, score: Double?, tint: Color, icon: ResultIcon, modifier: Modifier,
    nested: Boolean, level: RiskLevel, isAi: Boolean,
) {
    val danger = level == RiskLevel.DANGER
    Surface(modifier = modifier, color = if (nested) SoftBlue else Color.White, shape = RoundedCornerShape(14.dp),
        border = if (nested) null else BorderStroke(1.dp, CardOutline)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), fontSize = 11.sp, lineHeight = 16.sp, color = Muted)
                ResultGlyph(icon, if (level == RiskLevel.SAFE) Muted else tint, 16.dp)
            }
            if (danger) {
                Surface(shape = CircleShape, color = if (isAi) Color(0xFFE4E8F4) else SoftRed) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (!isAi) Box(Modifier.size(6.dp).background(tint, CircleShape))
                        Text(label, color = tint, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(score?.let { "분석 점수 ${formatScore(it)}%" } ?: "점수 정보 없음", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(label, color = tint, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold)
                    Text(when {
                        level == RiskLevel.SAFE && !isAi -> if (label == "정보 없음") "" else "Safe"
                        score == null -> "점수 정보 없음"
                        level == RiskLevel.SAFE -> "${formatScore(score)}%"
                        level == RiskLevel.UNKNOWN -> "분석 점수 ${formatScore(score)}%"
                        isAi -> "변조 점수 ${formatScore(score)}%"
                        else -> "위험 점수 ${formatScore(score)}%"
                    }, Modifier.weight(1f).padding(bottom = 2.dp), color = Muted, fontSize = 10.sp, lineHeight = 14.sp,
                        textAlign = if (nested) TextAlign.End else TextAlign.Start)
                }
                if (score != null) {
                    Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(Color(0xFFE4E9F7))
                        .progressSemantics((score / 100).toFloat())) {
                        Box(Modifier.fillMaxWidth((score / 100).toFloat()).fillMaxHeight().clip(CircleShape).background(tint))
                    }
                }
            }
        }
    }
}

internal fun formatScore(score: Double): String = if (score % 1.0 == 0.0) score.toInt().toString() else String.format(Locale.ROOT, "%.1f", score)

@Composable
private fun EvidencePanel(result: AnalysisResult, isDemo: Boolean) {
    val safe = result.riskLevel == RiskLevel.SAFE
    val danger = result.riskLevel == RiskLevel.DANGER
    ResultPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (!safe) ResultGlyph(if (danger) ResultIcon.SHIELD_OUTLINE else ResultIcon.SEARCH, if (danger) Danger else Ink, 18.dp)
            Text(when (result.riskLevel) {
                RiskLevel.SAFE -> "세부 항목 검증 내역"
                RiskLevel.CAUTION -> "발견된 주의 사항"
                RiskLevel.DANGER -> "왜 위험한가요?"
                RiskLevel.UNKNOWN -> "분석 근거"
            }, Modifier.weight(1f), fontSize = if (safe) 13.sp else 16.sp, lineHeight = 22.sp,
                fontWeight = if (safe) FontWeight.Medium else FontWeight.SemiBold, color = Ink)
            if (result.evidence.isNotEmpty() && (safe || danger)) {
                Surface(color = if (safe) Color(0xFFE7EEFF) else Color(0xFFE4E8F4), shape = CircleShape) {
                    Text(if (!isDemo) "${result.evidence.size}개 근거" else if (safe) "${result.evidence.size}건 모두 통과" else "${result.evidence.size}가지 단서 감지",
                        Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = if (safe) ProtectiveBlue else Muted,
                        fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
            if (safe) ResultGlyph(ResultIcon.CHECKLIST, Muted, 17.dp)
        }
        if (result.evidence.isEmpty()) {
            Text("세부 근거가 제공되지 않았어요. 요약과 대응 행동을 확인해주세요.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
        }
        result.evidence.forEachIndexed { index, item ->
            Surface(color = if (safe) Color.White else SoftBlue, shape = RoundedCornerShape(10.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = if (safe) 10.dp else 12.dp, horizontal = if (safe) 0.dp else 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Box(Modifier.size(if (safe) 24.dp else 30.dp).background(if (danger) Color(0xFFFFD9D5) else Color(0xFFE3EBFC), CircleShape), contentAlignment = Alignment.Center) {
                        when {
                            safe -> ResultGlyph(ResultIcon.CHECK, ProtectiveBlue, 15.dp)
                            danger -> ResultGlyph(when (index % 3) {
                                0 -> ResultIcon.CLOCK
                                1 -> ResultIcon.FLAG
                                else -> ResultIcon.HISTORY
                            }, Danger, 17.dp)
                            else -> Text("${index + 1}", fontSize = 12.sp, color = ProtectiveBlue, fontWeight = FontWeight.Medium)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.title, color = Ink, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                        Text(item.description, color = Muted, fontSize = 12.sp, lineHeight = 19.sp)
                    }
                }
            }
            if (safe && index < result.evidence.lastIndex) HorizontalDivider(color = CardOutline)
        }
    }
}

@Composable
private fun SafetyTip(guidance: List<String>? = null) {
    ResultPanel(color = SoftBlue) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(30.dp).background(Color(0xFFE3E8F5), CircleShape), contentAlignment = Alignment.Center) {
                ResultGlyph(ResultIcon.TIP, Ink, 17.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (guidance == null) "든든이 보안 수칙" else "대응 안내", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(guidance?.takeIf { it.isNotEmpty() }?.joinToString("\n")
                    ?: if (guidance != null) "서버에서 대응 안내를 제공하지 않았어요."
                    else "안전하더라도 개인정보나 비밀번호, 계좌 인증번호를 입력하라는 요청은 항상 주의하세요. 공식 창구를 재확인하는 것이 가장 안전합니다.", color = Muted, fontSize = 12.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun ResultActions(result: AnalysisResult, historySaved: Boolean, onAction: (ResultAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (result.riskLevel) {
            RiskLevel.SAFE -> {
                ActionButton("원래 화면으로 돌아가기", ResultIcon.FORWARD, PrimaryMidnight, Color.White, trailingIcon = true) { onAction(ResultAction.RETURN) }
                ActionButton(if (historySaved) "검사 결과가 기록에 보관되었어요" else "검사 결과 기록에 보관하기", ResultIcon.SAVE,
                    Color(0xFFE9EEFC), Ink, enabled = !historySaved) { onAction(ResultAction.SAVE_HISTORY) }
            }
            RiskLevel.CAUTION -> {
                ActionButton("링크를 열지 않고 인터넷 주소만 보기", ResultIcon.SEARCH, ProtectiveBlue, Color.White,
                    enabled = result.sourceUrl != null) { onAction(ResultAction.PREVIEW_DOMAIN) }
                if (result.sourceUrl == null) Text("확인할 링크가 제공되지 않았어요.", color = Muted, fontSize = 11.sp)
                ActionButton("가족·지인에게 물어보기", ResultIcon.FAMILY, Color(0xFFE4E9F7), Ink, iconColor = ProtectiveBlue) { onAction(ResultAction.ASK_FAMILY) }
                ReturnLink(onAction)
            }
            RiskLevel.DANGER -> {
                ActionButton("가족에게 직접 전화해 확인하기", ResultIcon.PHONE, PrimaryMidnight, Color.White) { onAction(ResultAction.CALL_FAMILY) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryAction("결과 가족에게 공유", ResultIcon.SHARE, ProtectiveBlue, Modifier.weight(1f)) { onAction(ResultAction.SHARE) }
                    SecondaryAction("신고 (112/118)", ResultIcon.PHONE, Danger, Modifier.weight(1f)) { onAction(ResultAction.REPORT) }
                }
                ReturnLink(onAction)
            }
            RiskLevel.UNKNOWN -> ReturnLink(onAction)
        }
        ActionButton("든든이 홈으로", ResultIcon.SHIELD, ProtectiveBlue, Color.White) {
            onAction(ResultAction.HOME)
        }
    }
}

@Composable
private fun SecondaryAction(text: String, icon: ResultIcon, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardOutline), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)) {
        ResultGlyph(icon, tint, 14.dp)
        Text(text, Modifier.padding(start = 5.dp), fontSize = if (LocalEasyMode.current) 18.sp else 11.sp,
            lineHeight = if (LocalEasyMode.current) 26.sp else 16.sp, color = Ink, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ReturnLink(onAction: (ResultAction) -> Unit) {
    TextButton(onClick = { onAction(ResultAction.RETURN) }, modifier = Modifier.fillMaxWidth().heightIn(min = if (LocalEasyMode.current) 48.dp else 0.dp)) {
        Text("원래 화면으로 돌아가기", color = Muted, fontSize = if (LocalEasyMode.current) 18.sp else 12.sp)
    }
}

@Composable
private fun ActionButton(
    text: String, icon: ResultIcon, background: Color, foreground: Color, enabled: Boolean = true,
    trailingIcon: Boolean = false, iconColor: Color = foreground, onClick: () -> Unit,
) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = background, contentColor = foreground),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)) {
        if (!trailingIcon) {
            ResultGlyph(icon, if (enabled) iconColor else Muted, 16.dp)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontSize = if (LocalEasyMode.current) 18.sp else 13.sp, lineHeight = if (LocalEasyMode.current) 26.sp else 18.sp, textAlign = TextAlign.Center)
        if (trailingIcon) {
            Spacer(Modifier.width(6.dp))
            ResultGlyph(icon, foreground, 16.dp)
        }
    }
}

@Composable
private fun ResultPanel(color: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = color, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
        border = if (color == Color.White) BorderStroke(1.dp, CardOutline) else null) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

/** Same service result, with a larger summary, independent metrics and readable evidence. */
@Composable
private fun EasyResultContent(result: AnalysisResult, isDemo: Boolean) {
    val tint = when (result.riskLevel) {
        RiskLevel.SAFE -> ProtectiveBlue
        RiskLevel.CAUTION -> CautionInk
        RiskLevel.DANGER -> Danger
        RiskLevel.UNKNOWN -> Ink
    }
    ResultPanel {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(color = SoftBlue, shape = CircleShape) {
                Text("분석 결과 · ${result.riskLevel.label}", Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = tint, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Image(painterResource(when (result.riskLevel) {
                RiskLevel.SAFE -> R.drawable.dundeuni_mascot_default
                RiskLevel.CAUTION -> R.drawable.dundeuni_mascot_suspicious
                RiskLevel.DANGER -> R.drawable.dundeuni_mascot_danger
                RiskLevel.UNKNOWN -> R.drawable.dundeuni_mascot_search
            }), null, Modifier.size(112.dp))
            Text(when (result.riskLevel) {
                RiskLevel.SAFE -> "지금 보신 내용은\n안전해 보여요"
                RiskLevel.CAUTION -> "조금 더\n확인해보세요"
                RiskLevel.DANGER -> "위험할 가능성이\n높아요"
                RiskLevel.UNKNOWN -> "결과 정보를\n확인해주세요"
            }, color = tint, fontSize = 28.sp, lineHeight = 38.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(result.summary, color = Ink, fontSize = 20.sp, lineHeight = 30.sp, textAlign = TextAlign.Center)
            result.keyFinding?.takeIf { it.isNotBlank() && it != result.summary }?.let {
                Text(it, color = Ink, fontSize = 20.sp, lineHeight = 30.sp, textAlign = TextAlign.Center)
            }
            Text("분석 대상: ${result.targetLabel}", color = Muted, fontSize = 16.sp, lineHeight = 24.sp)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        EasyMetric("사기 위험도", when (result.fraudLevel) {
            RiskLevel.SAFE -> "낮음"
            RiskLevel.CAUTION -> "주의"
            RiskLevel.DANGER -> "높음"
            RiskLevel.UNKNOWN -> "판정 미확정"
            null -> "정보 없음"
        }, result.fraudScore, Modifier.weight(1f))
        EasyMetric("AI 생성·변조 가능성", result.aiRiskLabel ?: result.aiLikelihood.label,
            result.aiScore, Modifier.weight(1f))
    }
    ResultPanel {
        Text("분석에서 확인한 내용", color = Ink, fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
        if (result.evidence.isEmpty()) {
            Text("세부 근거가 제공되지 않았어요. 위의 요약과 아래 안내를 확인해주세요.",
                color = Muted, fontSize = 18.sp, lineHeight = 28.sp)
        }
        result.evidence.forEachIndexed { index, evidence ->
            Surface(color = SoftBlue, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${index + 1}. ${evidence.title}", color = Ink, fontSize = 20.sp,
                        lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
                    Text(evidence.description, color = Muted, fontSize = 18.sp, lineHeight = 28.sp)
                }
            }
        }
    }
    result.sourceText?.takeIf { it.isNotBlank() }?.let {
        ResultPanel {
            Text("분석된 내용", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(it, color = Ink, fontSize = 18.sp, lineHeight = 28.sp)
        }
    }
    if (isDemo) ResultPanel(color = SoftBlue) {
        Text("이렇게 확인하세요", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(when (result.riskLevel) {
            RiskLevel.SAFE -> "낯선 요청은 한 번 더 확인하세요. 비밀번호나 인증번호는 알려주지 마세요."
            RiskLevel.CAUTION -> "의심스러운 링크는 누르지 말고, 공식 연락처나 믿을 수 있는 사람에게 확인하세요."
            RiskLevel.DANGER -> "송금을 멈추고, 가족이나 기관의 공식 연락처로 직접 확인하세요."
            RiskLevel.UNKNOWN -> "판정 정보가 충분하지 않아요. 요청한 사람이나 기관의 공식 연락처로 확인하세요."
        }, color = Ink, fontSize = 20.sp, lineHeight = 30.sp)
        Text("분석은 참고 정보이며 확정 판정이 아니에요. 사기 위험과 AI 생성·변조 가능성은 서로 다른 지표예요.",
            color = Muted, fontSize = 18.sp, lineHeight = 28.sp)
    }
}

@Composable
private fun EasyMetric(title: String, label: String, score: Double?, modifier: Modifier) {
    Surface(modifier = modifier, color = Color.White, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = Muted, fontSize = 16.sp, lineHeight = 24.sp)
            Text(label, color = Ink, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
            Text(score?.let { "분석 점수 ${formatScore(it)}%" } ?: "점수 정보 없음",
                color = Muted, fontSize = 16.sp, lineHeight = 24.sp)
        }
    }
}
