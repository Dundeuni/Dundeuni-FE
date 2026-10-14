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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextOverflow
import com.dundueni.app.R
import com.dundueni.app.ui.theme.Danger

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun SignUpCredentialsScreen(
    state: SignUpCredentialsUiState,
    onPasswordChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onProfile: () -> Unit,
    demo: Boolean = false,
) {
    val font = FontFamily(Font(R.font.pretendard_medium))
    val focus = LocalFocusManager.current
    val keyboardVisible = WindowInsets.isImeVisible
    val submit = { focus.clearFocus(); onSubmit() }
    val enabled = !state.submitting && !state.completed
    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(fontFamily = font, letterSpacing = 0.sp)) {
        Scaffold(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), containerColor = TermsColors.Background,
            topBar = { TermsHeader(onBack, "이메일 인증으로 돌아가기", compact = keyboardVisible) },
            bottomBar = {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .padding(top = if (keyboardVisible) 8.dp else 16.dp, bottom = if (keyboardVisible) 8.dp else 40.dp)) {
                    CredentialsButton(if (state.submitting) "가입 처리 중" else "회원가입 완료하기", state.canSubmit, submit, compact = keyboardVisible)
                    if (demo && !keyboardVisible) Text("UI 테스트 · 실제 계정 생성 없음", Modifier.padding(top = 8.dp), color = TermsColors.Muted, fontSize = 11.sp)
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(top = 48.dp, bottom = 24.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("3단계", Modifier.background(TermsColors.Link, CircleShape).padding(horizontal = 10.dp, vertical = 2.dp), color = Color.White, fontSize = 12.sp)
                        Text("닉네임 / 비밀번호 설정", Modifier.padding(start = 6.dp), fontSize = 12.sp, color = TermsColors.Body)
                        Spacer(Modifier.weight(1f))
                        Row(Modifier.semantics { contentDescription = "회원가입 3단계 중 3단계" }, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(2) { Box(Modifier.size(6.dp).background(TermsColors.Disabled, CircleShape)) }
                            Box(Modifier.size(24.dp, 6.dp).background(TermsColors.Link, CircleShape))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.background(TermsColors.LightBlue, CircleShape).padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        TermsIcon(R.drawable.auth_terms_shield, 12, 15)
                        Text("안전한 일상 지킴이, 든든이", Modifier.padding(start = 6.dp), fontSize = 12.sp, color = TermsColors.Link)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("안전한 활동을 위해\n비밀번호와 닉네임을 설정해주세요", color = TermsColors.Ink, fontSize = 24.sp, lineHeight = 33.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("에브리데이 쉴드가 안전하게 암호화하여 보호합니다.", fontSize = 14.sp, lineHeight = 23.sp, color = TermsColors.Body)
                    Spacer(Modifier.height(30.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RequiredLabel("비밀번호")
                        Spacer(Modifier.weight(1f))
                        val strength = when {
                            !state.lengthSatisfied || !state.combinationSatisfied -> "미충족"
                            state.specialSatisfied -> "높음"
                            else -> "보통"
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(R.drawable.auth_terms_protection), null, Modifier.size(11.dp, 14.dp),
                                colorFilter = ColorFilter.tint(if (strength == "높음") TermsColors.Link else Color(0xFFCC8B00)))
                            Text("보안 강도: $strength", Modifier.padding(start = 6.dp), fontSize = 11.sp,
                                color = if (strength == "높음") TermsColors.Link else Color(0xFFCC8B00))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    CredentialsField(state.password, "비밀번호 입력", "비밀번호", onPasswordChange, state.passwordError,
                        enabled, font, state.passwordVisible, onTogglePassword)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ValidationChip("8~20자 이내", state.lengthSatisfied)
                        ValidationChip("특수문자 포함", state.specialSatisfied)
                        ValidationChip("영문/숫자 조합", state.combinationSatisfied)
                    }
                    state.passwordError?.let { CredentialsMessage(it, true) }
                    Spacer(Modifier.height(12.dp))
                    CredentialsField(state.confirmation, "비밀번호 확인", "비밀번호 확인", onConfirmationChange, state.confirmationError,
                        enabled, font, state.passwordVisible, valid = state.passwordsMatch)
                    if (state.confirmationError != null) CredentialsMessage(state.confirmationError!!, true)
                    else if (state.passwordsMatch) CredentialsMessage("비밀번호가 안전하게 일치합니다", false)
                    Spacer(Modifier.height(36.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RequiredLabel("닉네임 설정")
                        Spacer(Modifier.weight(1f))
                        Text("${state.nicknameLength} / 10자", fontSize = 12.sp, color = if (state.nicknameLength > 10) Danger else TermsColors.Link)
                    }
                    Spacer(Modifier.height(12.dp))
                    CredentialsField(state.nickname, "닉네임 입력", "닉네임 설정", onNicknameChange, state.nicknameError,
                        enabled, font, null, valid = state.nickname.isNotBlank() && state.nicknameLength <= 10 && state.nicknameError == null,
                        onDone = submit)
                    state.nicknameError?.let { CredentialsMessage(it, true) }
                    state.message?.let { CredentialsMessage(it, true) }
                }
            }
        }
        if (state.completed) SignUpCompletedDialog(state.email, state.nickname, onHome, onProfile, demo)
    }
}

@Composable
private fun RequiredLabel(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TermsColors.Ink, fontSize = 14.sp)
        Box(Modifier.padding(start = 6.dp).size(4.dp).background(TermsColors.Link, CircleShape))
    }
}

@Composable
private fun ValidationChip(label: String, satisfied: Boolean) {
    Row(Modifier.background(TermsColors.LightBlue, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp)
        .semantics { contentDescription = "$label ${if (satisfied) "충족" else "미충족"}" }, verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.auth_terms_badge), null, Modifier.size(12.dp), colorFilter = ColorFilter.tint(if (satisfied) TermsColors.Link else TermsColors.Muted))
        Text(label, Modifier.padding(start = 4.dp), fontSize = 12.sp, color = if (satisfied) TermsColors.Link else TermsColors.Muted)
    }
}

@Composable
private fun CredentialsField(
    value: String, placeholder: String, label: String, onChange: (String) -> Unit, errorMessage: String?,
    enabled: Boolean, font: FontFamily, visible: Boolean?, onToggle: (() -> Unit)? = null,
    valid: Boolean = false, onDone: () -> Unit = {},
) {
    val focus = LocalFocusManager.current
    val shape = RoundedCornerShape(12.dp)
    BasicTextField(value, onChange, Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(shape)
        .background(if (errorMessage == null) Color.White else Color(0xFFFAF0F3))
        .then(if (errorMessage == null) Modifier else Modifier.border(1.dp, Danger, shape))
        .semantics { contentDescription = label; errorMessage?.let { error(it) } }, enabled = enabled, singleLine = true,
        textStyle = TextStyle(fontFamily = font, fontSize = 16.sp, color = TermsColors.Ink),
        cursorBrush = SolidColor(TermsColors.Link),
        visualTransformation = if (visible == false) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (visible == null) KeyboardType.Text else KeyboardType.Password,
            autoCorrectEnabled = false, imeAction = if (visible == null) ImeAction.Done else ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }, onDone = { onDone() }),
        decorationBox = { input ->
            Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, fontSize = 16.sp, color = TermsColors.Muted)
                    input()
                }
                if (onToggle != null) {
                    Box(Modifier.size(48.dp).clickable(enabled = enabled, role = Role.Button, onClick = onToggle)
                        .semantics { contentDescription = if (visible == true) "비밀번호 숨기기" else "비밀번호 보기" }, contentAlignment = Alignment.Center) {
                        TermsIcon(if (visible == true) R.drawable.auth_eye_visible else R.drawable.auth_eye_hidden, 18, if (visible == true) 13 else 17)
                    }
                } else {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        if (valid) Box(Modifier.size(28.dp).background(TermsColors.LightBlue, CircleShape), contentAlignment = Alignment.Center) {
                            Image(painterResource(R.drawable.auth_terms_check), null, Modifier.size(12.dp, 9.dp), colorFilter = ColorFilter.tint(TermsColors.Link))
                        }
                    }
                }
            }
        })
}

@Composable
private fun CredentialsMessage(message: String, isError: Boolean) {
    Row(Modifier.padding(top = 6.dp).semantics { liveRegion = LiveRegionMode.Polite }, verticalAlignment = Alignment.CenterVertically) {
        TermsIcon(if (isError) R.drawable.auth_error else R.drawable.auth_terms_badge, 14, 14)
        Text(message, Modifier.padding(start = 6.dp), fontSize = 12.sp, lineHeight = 20.sp, color = if (isError) Danger else TermsColors.Link)
    }
}

@Composable
internal fun CredentialsButton(label: String, enabled: Boolean, onClick: () -> Unit, compact: Boolean = false) {
    Box(Modifier.fillMaxWidth().heightIn(min = if (compact) 48.dp else 56.dp).clip(CircleShape)
        .background(if (enabled) TermsColors.Blue else TermsColors.Disabled)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(vertical = 12.dp, horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 16.sp, color = if (enabled) Color.White else TermsColors.Muted)
    }
}

@Composable
private fun SignUpCompletedDialog(email: String, nickname: String, onHome: () -> Unit, onProfile: () -> Unit, demo: Boolean) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.padding(16.dp).widthIn(max = 480.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
            .verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.padding(top = 8.dp).size(64.dp).background(TermsColors.Blue, CircleShape), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.auth_success_shield), null, Modifier.size(26.dp, 32.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("보호 시스템 활성화", Modifier.background(TermsColors.LightBlue, CircleShape).padding(horizontal = 12.dp, vertical = 4.dp), color = TermsColors.Link, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            Text("회원가입이 완료되었습니다!", fontSize = 20.sp, color = TermsColors.Ink)
            Spacer(Modifier.height(12.dp))
            Text("$nickname 님, 환영합니다!", fontSize = 18.sp, color = TermsColors.Link)
            Spacer(Modifier.height(12.dp))
            Text("이제 든든이가 가족과 금융을 실시간 사기 위협으로부터\n24시간 안전하게 지켜드릴게요.", fontSize = 12.sp, lineHeight = 17.sp, color = TermsColors.Body)
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth().background(TermsColors.LightBlue, RoundedCornerShape(12.dp)).padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TermsIcon(R.drawable.auth_profile, 14, 14)
                    Text("가입 계정", Modifier.padding(start = 8.dp), color = TermsColors.Body, fontSize = 12.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(email, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, color = TermsColors.Ink, fontSize = 14.sp)
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = TermsColors.Disabled)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TermsIcon(R.drawable.auth_terms_badge, 14, 14)
                    Text("기본 안심 플랜", Modifier.padding(start = 8.dp), color = TermsColors.Body, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text("든든이 베이직 무료 시작", Modifier.background(Color(0xFFDCE3FF), CircleShape).padding(horizontal = 8.dp, vertical = 2.dp),
                        color = TermsColors.Ink, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(36.dp))
            CredentialsButton("든든이 시작하기 (홈으로) →", true, onHome)
            Text("내 프로필 및 안심 보호자 설정하기", Modifier.padding(top = 12.dp).heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onProfile).padding(vertical = 14.dp), color = TermsColors.Body, fontSize = 12.sp)
            if (demo) Text("UI 테스트 · 실제 가입·보호 활성화 없음", color = TermsColors.Muted, fontSize = 11.sp)
        }
    }
}
