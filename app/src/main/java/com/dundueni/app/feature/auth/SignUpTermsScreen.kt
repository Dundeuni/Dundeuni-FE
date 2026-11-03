package com.dundueni.app.feature.auth

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dundueni.app.R

@Composable
fun SignUpTermsScreen(
    state: TermsUiState,
    onTermChange: (SignUpTerm, Boolean) -> Unit,
    onAllChange: (Boolean) -> Unit,
    onShowTerm: (SignUpTerm) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(
        fontFamily = FontFamily(Font(R.font.pretendard_medium)), letterSpacing = 0.sp,
    )) {
        Scaffold(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            containerColor = TermsColors.Background,
            topBar = { TermsHeader(onBack) },
            bottomBar = { TermsFooter(state.canContinue, onNext) },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("1단계", Modifier.background(TermsColors.Link, CircleShape).padding(horizontal = 10.dp, vertical = 2.dp),
                            color = Color.White, fontSize = 12.sp, lineHeight = 16.sp)
                        Text("약관 동의", Modifier.padding(start = 6.dp), color = TermsColors.Body, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Row(Modifier.semantics { contentDescription = "회원가입 3단계 중 1단계" }, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(24.dp, 6.dp).background(TermsColors.Link, CircleShape))
                            repeat(2) { Box(Modifier.size(6.dp).background(TermsColors.Disabled, CircleShape)) }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.background(TermsColors.LightBlue, CircleShape).padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        TermsIcon(R.drawable.auth_terms_shield, 12, 15)
                        Text("안전한 일상 지킴이, 든든이", Modifier.padding(start = 6.dp), color = TermsColors.Link, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("든든이 서비스 이용을 위해\n약관에 동의해 주세요", color = TermsColors.Ink, fontSize = 24.sp, lineHeight = 30.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("개인정보는 안전하게 암호화되어 보호됩니다.", color = TermsColors.Body, fontSize = 14.sp, lineHeight = 23.sp)
                    Spacer(Modifier.height(24.dp))
                    Row(
                        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(48.dp).background(Color(0xFFDCE3FF), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                            TermsIcon(R.drawable.auth_terms_protection, 17, 22)
                            Box(Modifier.align(Alignment.BottomEnd).offset(x = 3.dp, y = 3.dp)
                                .size(14.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                                TermsIcon(R.drawable.auth_terms_badge, 10, 10)
                            }
                        }
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text("금융 사기 & 보이스피싱 자동 차단", color = TermsColors.Ink, fontSize = 12.sp, lineHeight = 16.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("안심 탐지 기능을 위해 꼭 필요한 기본 필수 약관이에요.", color = TermsColors.Body, fontSize = 10.sp, lineHeight = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 72.dp)
                            .background(TermsColors.LightBlue, RoundedCornerShape(20.dp))
                            .toggleable(state.allSelected, role = Role.Checkbox, onValueChange = onAllChange)
                            .semantics { contentDescription = "약관 전체 동의하기" }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SelectionMark(state.allSelected, 24)
                        Column(Modifier.weight(1f).padding(start = 16.dp)) {
                            Text("약관 전체 동의하기", color = TermsColors.Ink, fontSize = 16.sp, lineHeight = 24.sp)
                            Text("필수 3종 및 선택 약관 일괄 동의", color = TermsColors.Body, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                        Text(if (state.allSelected) "선택 해제" else "모두 선택",
                            Modifier.background(Color.White, CircleShape).padding(horizontal = 8.dp, vertical = 4.dp),
                            color = TermsColors.Body, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    SignUpTerm.entries.forEachIndexed { index, term ->
                        if (index > 0) Spacer(Modifier.height(8.dp))
                        TermsRow(term, state.selected(term), { onTermChange(term, it) }, { onShowTerm(term) })
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(
                        Modifier.fillMaxWidth().background(Color(0xFFE9EEFB), RoundedCornerShape(16.dp)).padding(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        TermsIcon(R.drawable.auth_terms_notice, 13, 20)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("불필요한 마케팅 알림은 보내지 않습니다", color = TermsColors.Ink, fontSize = 12.sp, lineHeight = 17.sp)
                            Spacer(Modifier.height(2.dp))
                            Text("든든이는 안전 탐지 및 위기 보호 목적 외 광고성 마케팅 정보 수신을 강제하지 않습니다.",
                                color = TermsColors.Body, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TermsHeader(onBack: () -> Unit, backLabel: String = "로그인으로 돌아가기", compact: Boolean = false, title: String = "회원가입") {
    Row(Modifier.fillMaxWidth().height(if (compact) 48.dp else 64.dp).shadow(1.dp).background(TermsColors.Background).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clickable(role = Role.Button, onClick = onBack)
            .semantics { contentDescription = backLabel }, contentAlignment = Alignment.Center) {
            TermsIcon(R.drawable.auth_terms_back, 12, 20)
        }
        Text(title, Modifier.padding(start = 8.dp), color = TermsColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TermsRow(term: SignUpTerm, selected: Boolean, onChange: (Boolean) -> Unit, onDetails: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).background(Color.White, RoundedCornerShape(12.dp)), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).heightIn(min = 60.dp).toggleable(selected, role = Role.Checkbox, onValueChange = onChange)
            .semantics { contentDescription = term.title }.padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            SelectionMark(selected, 20)
            Text(if (term.required) "[필수]" else "[선택]", Modifier.padding(start = 12.dp, end = 4.dp), color = TermsColors.Link, fontSize = 12.sp)
            Text(term.title, Modifier.weight(1f), color = TermsColors.Ink, fontSize = 14.sp, lineHeight = 20.sp)
        }
        Box(Modifier.size(48.dp).clickable(role = Role.Button, onClick = onDetails)
            .semantics { contentDescription = "${term.title} 전문 보기" }, contentAlignment = Alignment.Center) {
            TermsIcon(R.drawable.auth_terms_chevron, 6, 9)
        }
    }
}

@Composable
private fun SelectionMark(selected: Boolean, size: Int) {
    Box(Modifier.size(size.dp).background(if (selected) TermsColors.Link else TermsColors.Disabled, CircleShape), contentAlignment = Alignment.Center) {
        TermsIcon(R.drawable.auth_terms_check, if (size == 24) 12 else 10, if (size == 24) 9 else 8)
    }
}

@Composable
private fun TermsFooter(enabled: Boolean, onNext: () -> Unit) {
    Box(Modifier.fillMaxWidth().background(TermsColors.Background), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().height(56.dp).background(if (enabled) TermsColors.Blue else TermsColors.Disabled, CircleShape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onNext),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Text("다음 단계로", color = if (enabled) Color.White else TermsColors.Muted, fontSize = 16.sp)
                Image(painterResource(R.drawable.auth_terms_next), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(if (enabled) Color.White else TermsColors.Muted))
            }
            Spacer(Modifier.height(16.dp))
            Text("모든 필수 약관에 동의해야 본인인증으로 진행할 수 있습니다.",
                color = TermsColors.Body, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
internal fun TermsIcon(@DrawableRes resource: Int, width: Int, height: Int) {
    Image(painterResource(resource), null, Modifier.size(width.dp, height.dp))
}

internal object TermsColors {
    val Background = Color(0xFFF8F9FF)
    val LightBlue = Color(0xFFEFF4FF)
    val Link = Color(0xFF0050CC)
    val Blue = Color(0xFF0366FF)
    val Disabled = Color(0xFFDDE3EF)
    val Ink = Color(0xFF161C25)
    val Body = Color(0xFF45474C)
    val Muted = Color(0xFF75777D)
}
