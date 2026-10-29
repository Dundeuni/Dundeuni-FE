package com.dundueni.app.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dundueni.app.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PasswordResetScreen(
    state: PasswordResetUiState,
    onPasswordChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    demo: Boolean = false,
) {
    val fields = state.fields
    val font = FontFamily(Font(R.font.pretendard_medium))
    val keyboardVisible = WindowInsets.isImeVisible
    val focus = LocalFocusManager.current
    val submit = { focus.clearFocus(); onSubmit() }
    val enabled = !state.submitting && !state.completed && !state.needsVerification
    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(fontFamily = font, letterSpacing = 0.sp)) {
        Scaffold(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), containerColor = TermsColors.Background,
            topBar = { TermsHeader(onBack, "재설정 이메일 인증으로 돌아가기", keyboardVisible, title = "비밀번호 재설정") },
            bottomBar = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = if (keyboardVisible) 8.dp else 24.dp)) {
                        CredentialsButton(if (state.submitting) "변경 처리 중" else "비밀번호 변경 완료", state.canSubmit, submit, keyboardVisible, showArrow = true)
                        Text(if (demo) "UI 미리보기 · 샘플 인증 · 실제 비밀번호 변경 없음"
                            else "새 비밀번호와 확인 입력값이 일치해야 진행할 수 있습니다.",
                            Modifier.padding(top = 8.dp), color = TermsColors.Muted, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 24.dp)) {
                    Row(Modifier.fillMaxWidth().background(TermsColors.LightBlue, RoundedCornerShape(12.dp)).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(28.dp).background(Color(0xFFD6E4FA), CircleShape), contentAlignment = Alignment.Center) {
                            TermsIcon(R.drawable.auth_reset_verified, 12, 15)
                        }
                        Column(Modifier.padding(start = 10.dp)) {
                            Text("이메일 인증 완료", color = TermsColors.Link, fontSize = 11.sp)
                            Text(maskResetEmail(fields.email), color = TermsColors.Ink, fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("새로운 비밀번호를\n설정해 주세요", fontSize = 24.sp, lineHeight = 33.sp, color = TermsColors.Ink)
                    Spacer(Modifier.height(8.dp))
                    Text("안전한 계정 보호를 위해 새로운 비밀번호를\n입력해 주세요.", fontSize = 14.sp, lineHeight = 23.sp, color = TermsColors.Body)
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("비밀번호", fontSize = 14.sp, color = TermsColors.Ink)
                        Box(Modifier.padding(start = 6.dp).size(4.dp).background(TermsColors.Link, CircleShape))
                        Spacer(Modifier.weight(1f))
                        val strength = if (!fields.lengthSatisfied || !fields.combinationSatisfied) "미충족" else if (fields.specialSatisfied) "높음" else "보통"
                        Image(painterResource(R.drawable.auth_reset_strength), null, Modifier.size(10.5.dp, 11.667.dp))
                        Text("보안 강도: $strength", Modifier.padding(start = 6.dp), fontSize = 11.sp,
                            color = if (strength == "높음") TermsColors.Link else Color(0xFFCC8B00))
                    }
                    Spacer(Modifier.height(12.dp))
                    CredentialsField(fields.password, "비밀번호 입력", "새 비밀번호", onPasswordChange, fields.passwordError,
                        enabled, font, fields.passwordVisible, onTogglePassword)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ValidationChip("8~20자 이내", fields.lengthSatisfied, filled = true)
                        ValidationChip("특수문자 포함", fields.specialSatisfied, filled = true)
                        ValidationChip("영문/숫자 조합", fields.combinationSatisfied, filled = true)
                    }
                    fields.passwordError?.let { CredentialsMessage(it, true) }
                    Spacer(Modifier.height(12.dp))
                    CredentialsField(fields.confirmation, "비밀번호 확인", "새 비밀번호 확인", onConfirmationChange, fields.confirmationError,
                        enabled, font, fields.passwordVisible, valid = fields.passwordsMatch, onDone = submit, imeAction = ImeAction.Done)
                    if (fields.confirmationError != null) CredentialsMessage(fields.confirmationError!!, true)
                    else if (fields.passwordsMatch) Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.auth_reset_match), null, Modifier.size(13.333.dp))
                        Text("비밀번호가 안전하게 일치합니다", Modifier.padding(start = 6.dp), color = TermsColors.Link, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth().background(Color(0xFFE8ECF4), RoundedCornerShape(12.dp)).padding(16.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(32.dp).background(Color.White, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Image(painterResource(R.drawable.auth_reset_notice), null, Modifier.size(16.667.dp))
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("보안 알림", fontSize = 12.sp, color = TermsColors.Ink)
                                Box(Modifier.padding(start = 6.dp).size(4.dp).background(TermsColors.Link, CircleShape))
                            }
                            Text(if (demo) "샘플 인증 화면입니다. 실제 비밀번호 변경이나 다른 기기의 로그아웃은 실행하지 않습니다."
                                else "비밀번호 변경 및 다른 기기의 로그인 유지 정책은 서비스 연동 후 안내됩니다.",
                                Modifier.padding(top = 4.dp), fontSize = 11.sp, lineHeight = 19.sp, color = TermsColors.Body)
                        }
                    }
                    state.message?.let { CredentialsMessage(it, true) }
                    if (state.needsVerification) TextButton(onClick = onBack) { Text("이메일 인증 다시 하기") }
                }
            }
        }
        if (state.completed) AlertDialog(
            onDismissRequest = onLogin,
            title = { Text(if (demo) "샘플 변경 확인 완료" else "비밀번호 변경 완료") },
            text = { Text(if (demo) "비밀번호 재설정 UI 흐름을 확인했습니다. 실제 비밀번호는 변경되지 않았습니다."
                else "새 비밀번호로 로그인해 주세요.") },
            confirmButton = { TextButton(onClick = onLogin) { Text("로그인으로 돌아가기") } },
        )
    }
}

internal fun maskResetEmail(email: String): String {
    val separator = email.lastIndexOf('@')
    if (separator <= 0) return ""
    return email.take(separator).take(2) + "***" + email.substring(separator)
}
