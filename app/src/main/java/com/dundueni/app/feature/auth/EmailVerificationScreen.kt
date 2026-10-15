package com.dundueni.app.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dundueni.app.R
import com.dundueni.app.ui.theme.Danger

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun EmailVerificationScreen(
    state: EmailVerificationUiState,
    onEmailChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onSendCode: () -> Unit,
    onCheckCode: () -> Unit,
    onBack: () -> Unit,
    demo: Boolean = false,
) {
    val focus = LocalFocusManager.current
    val keyboardVisible = WindowInsets.isImeVisible
    val font = FontFamily(Font(R.font.pretendard_medium))
    val check = { focus.clearFocus(); onCheckCode() }
    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(fontFamily = font, letterSpacing = 0.sp)) {
        Scaffold(
            modifier = Modifier.fillMaxSize().background(TermsColors.Background).safeDrawingPadding().imePadding(),
            containerColor = TermsColors.Background,
            topBar = { TermsHeader(onBack, "약관 동의로 돌아가기", compact = keyboardVisible) },
            bottomBar = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = 20.dp)
                        .padding(top = if (keyboardVisible) 8.dp else 16.dp, bottom = if (keyboardVisible) 8.dp else 40.dp)) {
                        Row(Modifier.fillMaxWidth().heightIn(min = if (keyboardVisible) 48.dp else 56.dp).clip(CircleShape)
                            .background(if (state.canContinue && !state.busy) TermsColors.Blue else TermsColors.Disabled)
                            .clickable(enabled = state.canContinue && !state.busy, role = Role.Button, onClick = check),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (state.busy) "처리 중" else "다음 단계로", fontSize = 16.sp,
                                color = if (state.canContinue && !state.busy) Color.White else TermsColors.Muted)
                            Image(painterResource(R.drawable.auth_terms_next), null, Modifier.size(14.dp),
                                colorFilter = ColorFilter.tint(if (state.canContinue && !state.busy) Color.White else TermsColors.Muted))
                        }
                        if (demo && !keyboardVisible) {
                            Text("UI 테스트 · 실제 발송 없음 · 샘플 번호 123456", Modifier.padding(top = 8.dp),
                                color = TermsColors.Muted, fontSize = 11.sp)
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(top = 48.dp, bottom = 24.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("2단계", Modifier.background(TermsColors.Link, CircleShape).padding(horizontal = 10.dp, vertical = 2.dp),
                            color = Color.White, fontSize = 12.sp, lineHeight = 16.sp)
                        Text("본인 인증", Modifier.padding(start = 6.dp), fontSize = 12.sp, color = TermsColors.Body)
                        Spacer(Modifier.weight(1f))
                        Row(Modifier.semantics { contentDescription = "회원가입 3단계 중 2단계" }, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(6.dp).background(TermsColors.Disabled, CircleShape))
                            Box(Modifier.size(24.dp, 6.dp).background(TermsColors.Link, CircleShape))
                            Box(Modifier.size(6.dp).background(TermsColors.Disabled, CircleShape))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.background(TermsColors.LightBlue, CircleShape).padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        TermsIcon(R.drawable.auth_terms_shield, 12, 15)
                        Text("안전한 일상 지킴이, 든든이", Modifier.padding(start = 6.dp), color = TermsColors.Link, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("가입할 이메일로\n본인 인증을 진행해 주세요", fontSize = 24.sp, lineHeight = 33.sp, color = TermsColors.Ink)
                    Spacer(Modifier.height(8.dp))
                    Text("실시간 탐지 서비스 제공을 위해 필수 단계입니다.", fontSize = 14.sp, lineHeight = 20.sp, color = TermsColors.Body)
                    Spacer(Modifier.height(50.dp))
                    Text("이메일 주소", fontSize = 14.sp, color = TermsColors.Ink)
                    Spacer(Modifier.height(10.dp))
                    val largeText = LocalDensity.current.fontScale > 1.3f
                    val sendEnabled = !state.active && !state.busy && !state.verified
                    val emailField: @Composable (Modifier) -> Unit = { modifier ->
                        VerificationField(state.email, "이메일 입력", "이메일 주소", onEmailChange,
                            modifier, !state.verified && !state.busy, state.emailError,
                            KeyboardType.Email, font, if (state.active) ImeAction.Next else ImeAction.Done,
                            { focus.clearFocus(); onSendCode() })
                    }
                    val sendButton: @Composable (Modifier) -> Unit = { modifier ->
                        Box(modifier.heightIn(min = 42.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (sendEnabled) TermsColors.Blue else TermsColors.Disabled)
                            .clickable(enabled = sendEnabled, role = Role.Button) { focus.clearFocus(); onSendCode() }
                            .padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text("인증번호 받기", fontSize = 14.sp, color = if (sendEnabled) Color.White else TermsColors.Muted)
                        }
                    }
                    if (largeText) {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            emailField(Modifier.fillMaxWidth())
                            sendButton(Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            emailField(Modifier.weight(1f))
                            sendButton(Modifier.width(122.dp))
                        }
                    }
                    state.emailError?.let { VerificationMessage(it, true) }
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("인증번호 6자리", fontSize = 14.sp, color = TermsColors.Ink)
                        Spacer(Modifier.weight(1f))
                        if (state.hasSentCode && !state.verified) {
                            TermsIcon(R.drawable.auth_timer, 14, 16)
                            Text(state.timerText, Modifier.padding(start = 4.dp).semantics { contentDescription = "인증 남은 시간 ${state.timerText}" },
                                fontSize = 14.sp, color = if (state.active) TermsColors.Ink else Danger)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    VerificationField(state.code, "", "인증번호 6자리", onCodeChange, Modifier.fillMaxWidth(),
                        state.active && !state.busy && !state.verified, state.codeError, KeyboardType.NumberPassword,
                        font, ImeAction.Done, check, code = true)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Box(Modifier.weight(1f)) { state.codeError?.let { VerificationMessage(it, true) } }
                        if (state.hasSentCode && !state.verified) {
                            Row(Modifier.heightIn(min = 48.dp).clickable(enabled = !state.busy, role = Role.Button, onClick = onSendCode),
                                horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("새 번호 발송", fontSize = 12.sp, color = TermsColors.Ink)
                                TermsIcon(R.drawable.auth_resend, 12, 12)
                            }
                        }
                    }
                    state.notice?.let { VerificationMessage(it, !state.verified) }
                }
            }
        }
    }
}

@Composable
private fun VerificationField(
    value: String, placeholder: String, label: String, onChange: (String) -> Unit, modifier: Modifier,
    enabled: Boolean, errorMessage: String?, keyboard: KeyboardType, font: FontFamily,
    imeAction: ImeAction, onDone: () -> Unit, code: Boolean = false,
) {
    val focus = LocalFocusManager.current
    val shape = RoundedCornerShape(12.dp)
    BasicTextField(value, onChange, modifier.heightIn(min = if (code) 56.dp else 42.dp).clip(shape)
        .background(if (errorMessage != null) Color(0xFFFAF0F3) else Color.White)
        .then(if (errorMessage != null) Modifier.border(1.dp, Danger, shape) else Modifier)
        .semantics { contentDescription = label; errorMessage?.let { error(it) } },
        enabled = enabled, singleLine = true,
        textStyle = TextStyle(fontFamily = font, color = TermsColors.Ink, fontSize = if (code) 22.sp else 14.sp, letterSpacing = if (code) 3.sp else 0.sp),
        cursorBrush = SolidColor(TermsColors.Link),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = imeAction, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }, onDone = { onDone() }),
        decorationBox = { input ->
            Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, fontSize = 14.sp, color = Color(0xFF999BA2))
                    input()
                }
                Box(Modifier.size(44.dp).clickable(enabled = enabled, role = Role.Button) { onChange("") }
                    .semantics { contentDescription = "$label 지우기" }, contentAlignment = Alignment.Center) {
                    TermsIcon(R.drawable.auth_clear, 14, 14)
                }
            }
        },
    )
}

@Composable
private fun VerificationMessage(message: String, error: Boolean) {
    Row(Modifier.padding(top = 10.dp, end = 6.dp).semantics { liveRegion = LiveRegionMode.Polite }, verticalAlignment = Alignment.CenterVertically) {
        TermsIcon(if (error) R.drawable.auth_error else R.drawable.auth_terms_badge, 11, 11)
        Text(message, Modifier.padding(start = 6.dp), fontSize = 12.sp, lineHeight = 18.sp, color = if (error) Danger else TermsColors.Link)
    }
}
