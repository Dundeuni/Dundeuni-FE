package com.dundueni.app.feature.auth

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dundueni.app.R
import com.dundueni.app.ui.theme.Danger

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onLogin: () -> Unit,
    onSignUp: () -> Unit,
    onResetPassword: () -> Unit,
    onKakaoLogin: () -> Unit,
    onGoogleLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    val keyboardVisible = WindowInsets.isImeVisible
    val submit = { focus.clearFocus(); onLogin() }
    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(
        fontFamily = LoginFont, letterSpacing = 0.sp,
    )) {
    Box(modifier.fillMaxSize().background(LoginColors.Background).safeDrawingPadding().imePadding()) {
        Column(
            Modifier.fillMaxSize().padding(bottom = if (keyboardVisible) 64.dp else 0.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 390px 원본의 로고 위치를 유지하고 큰 화면에서는 폼 너비만 제한한다.
            if (keyboardVisible) Spacer(Modifier.height(16.dp))
            else {
                Spacer(Modifier.height(111.dp))
                LoginBrand()
                Spacer(Modifier.height(50.dp))
            }
            Column(Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = 20.dp)) {
                Column(
                    Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp))
                        .background(Color.White, RoundedCornerShape(12.dp)).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LoginField(
                        label = "이메일 계정", value = state.email, placeholder = "이메일 입력",
                        onValueChange = onEmailChange, icon = R.drawable.auth_email,
                        enabled = !state.submitting, invalid = state.credentialsRejected || state.emailError != null,
                        errorMessage = state.emailError, keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    )
                    Column {
                        LoginField(
                            label = "비밀번호", value = state.password, placeholder = "비밀번호 입력",
                            onValueChange = onPasswordChange, icon = R.drawable.auth_lock,
                            enabled = !state.submitting, invalid = state.credentialsRejected || state.passwordError != null,
                            errorMessage = state.passwordError, keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done, onDone = submit,
                            passwordVisible = state.passwordVisible, onTogglePassword = onTogglePassword,
                        )
                        state.message?.let { message ->
                            Spacer(Modifier.height(6.dp))
                            LoginMessage(message)
                        }
                    }
                    if (!keyboardVisible) LoginSubmitButton(state, submit)
                    Row(
                        Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        UtilityLink("비밀번호 재설정", LoginColors.Text, !state.submitting, onResetPassword)
                        Spacer(Modifier.width(16.dp))
                        Box(Modifier.size(4.dp).background(LoginColors.Divider, RoundedCornerShape(2.dp)))
                        Spacer(Modifier.width(16.dp))
                        UtilityLink("회원가입", LoginColors.Link, !state.submitting, onSignUp)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f).height(1.dp).background(LoginColors.Divider))
                    Text("간편 소셜 로그인", Modifier.padding(horizontal = 12.dp), color = LoginColors.Muted, fontSize = 12.sp)
                    Box(Modifier.weight(1f).height(1.dp).background(LoginColors.Divider))
                }
                SocialButton("카카오로 시작하기", R.drawable.auth_kakao, LoginColors.Kakao, !state.submitting, onKakaoLogin)
                Spacer(Modifier.height(12.dp))
                SocialButton("Google로 시작하기", R.drawable.auth_google, Color.White, !state.submitting, onGoogleLogin)
                Spacer(Modifier.height(60.dp))
            }
        }
        if (keyboardVisible) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(LoginColors.Background), contentAlignment = Alignment.Center) {
                Box(Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = 40.dp, vertical = 8.dp)) {
                    LoginSubmitButton(state, submit, compact = true)
                }
            }
        }
    }
    }
}

@Composable
private fun LoginSubmitButton(state: LoginUiState, onSubmit: () -> Unit, compact: Boolean = false) {
    Box(Modifier.fillMaxWidth().height(if (compact) 48.dp else 27.dp).clip(RoundedCornerShape(24.dp))
        .clickable(enabled = !state.submitting, role = Role.Button, onClick = onSubmit), contentAlignment = Alignment.Center) {
        Row(Modifier.fillMaxWidth().heightIn(min = 27.dp).background(LoginColors.Blue, RoundedCornerShape(24.dp)),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            if (state.submitting) CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
            Text(if (state.submitting) "로그인 중" else "로그인", color = Color.White, fontSize = 11.sp)
            if (!state.submitting) AssetIcon(R.drawable.auth_arrow, 7, 8)
        }
    }
}

@Composable
private fun LoginBrand() {
    val image = ImageBitmap.imageResource(R.drawable.auth_login_brand)
    Box(Modifier.size(128.dp, 191.dp)) {
        // Figma는 같은 원본을 두 프레임으로 잘라 캐릭터와 워드마크를 세로 배치한다.
        Canvas(Modifier.offset(x = 5.dp).size(128.dp).clip(RoundedCornerShape(0.dp)).semantics {
            contentDescription = "든든이 캐릭터"
        }) {
            drawImage(image, dstOffset = IntOffset((-21.85.dp.toPx()).toInt(), 0),
                dstSize = IntSize(350.44.dp.roundToPx(), 128.dp.roundToPx()))
        }
        Canvas(Modifier.align(Alignment.TopCenter).offset(x = 5.dp, y = 119.dp).size(120.dp, 72.dp).clip(RoundedCornerShape(0.dp)).semantics {
            contentDescription = "든든이, AI로부터 더 안전한 디지털 생활"
        }) {
            drawImage(image, dstOffset = IntOffset((-101.71.dp.toPx()).toInt(), (-15.34.dp.toPx()).toInt()),
                dstSize = IntSize(239.62.dp.roundToPx(), 87.34.dp.roundToPx()))
        }
    }
}

@Composable
private fun LoginField(
    label: String, value: String, placeholder: String, onValueChange: (String) -> Unit,
    @DrawableRes icon: Int, enabled: Boolean, invalid: Boolean, errorMessage: String?,
    keyboardType: KeyboardType, imeAction: ImeAction, onDone: () -> Unit = {},
    passwordVisible: Boolean? = null, onTogglePassword: () -> Unit = {},
) {
    val focus = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, Modifier.padding(start = 4.dp), color = LoginColors.Text, fontSize = 12.sp, lineHeight = 16.sp)
        val shape = RoundedCornerShape(8.dp)
        BasicTextField(
            value = value, onValueChange = onValueChange, enabled = enabled, singleLine = true,
            modifier = Modifier.fillMaxWidth().heightIn(min = if (passwordVisible == null) 40.dp else 43.dp).clip(shape)
                .background(if (invalid) LoginColors.ErrorBackground else LoginColors.Background)
                .then(if (invalid) Modifier.border(1.dp, Danger, shape) else Modifier)
                .semantics { contentDescription = label; if (invalid) error(errorMessage ?: "이메일 또는 비밀번호가 일치하지 않습니다.") },
            textStyle = TextStyle(color = LoginColors.Text, fontFamily = LoginFont, fontSize = 14.sp, letterSpacing = 0.sp),
            cursorBrush = SolidColor(LoginColors.Link),
            visualTransformation = if (passwordVisible == false) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }, onDone = { onDone() }),
            decorationBox = { input ->
                Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AssetIcon(icon, if (passwordVisible == null) 15 else 12, if (passwordVisible == null) 12 else 16)
                    Spacer(Modifier.width(16.dp))
                    Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
                        if (value.isEmpty()) Text(placeholder, color = LoginColors.Placeholder, fontSize = 14.sp)
                        input()
                    }
                    if (passwordVisible != null) {
                        Box(Modifier.size(48.dp).clickable(enabled = enabled, role = Role.Button, onClick = onTogglePassword)
                            .semantics { contentDescription = if (passwordVisible) "비밀번호 숨기기" else "비밀번호 보기" },
                            contentAlignment = Alignment.Center) {
                            AssetIcon(if (passwordVisible) R.drawable.auth_eye_visible else R.drawable.auth_eye_hidden, 18, if (passwordVisible) 13 else 17)
                        }
                    } else Spacer(Modifier.width(12.dp))
                }
            },
        )
        errorMessage?.let { LoginMessage(it) }
    }
}

@Composable
private fun LoginMessage(message: String) {
    Row(Modifier.padding(start = 4.dp).semantics { liveRegion = LiveRegionMode.Polite }, verticalAlignment = Alignment.CenterVertically) {
        AssetIcon(R.drawable.auth_error, 11, 11)
        Spacer(Modifier.width(4.dp))
        Text(message, color = Danger, fontSize = 10.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun UtilityLink(label: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.height(24.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, color = color, fontSize = 12.sp)
    }
}

@Composable
private fun SocialButton(label: String, @DrawableRes icon: Int, background: Color, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(56.dp).shadow(1.dp, RoundedCornerShape(28.dp))
        .background(background, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp))
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Image(painterResource(icon), null, Modifier.align(Alignment.CenterStart).padding(start = 20.dp).size(20.dp))
        Text(label, color = LoginColors.Ink, fontSize = 14.sp)
    }
}

@Composable
private fun AssetIcon(@DrawableRes resource: Int, width: Int, height: Int) {
    Image(painterResource(resource), null, Modifier.size(width.dp, height.dp))
}

private val LoginFont = FontFamily(Font(R.font.pretendard_medium))

private object LoginColors {
    val Background = Color(0xFFF8F9FF)
    val Blue = Color(0xFF0266FF)
    val Link = Color(0xFF0050CC)
    val Text = Color(0xFF45474C)
    val Muted = Color(0xFF75777D)
    val Placeholder = Color(0xFF999BA2)
    val Divider = Color(0xFFDDE3EF)
    val ErrorBackground = Color(0xFFFAF0F3)
    val Kakao = Color(0xFFFEE500)
    val Ink = Color(0xFF161C25)
}
