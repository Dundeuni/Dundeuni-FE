package com.dundueni.app.ui.component

import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.dundueni.app.R
import com.dundueni.app.feature.payment.BasicInspectionQuota
import com.dundueni.app.ui.theme.DundueniDefaultShape
import com.dundueni.app.ui.theme.DundueniDimens
import com.dundueni.app.ui.theme.DundueniFETheme
import com.dundueni.app.ui.theme.PrimaryMidnight
import com.dundueni.app.ui.theme.ProtectiveBlue

/** Dismissing this dialog only hides the popup. The caller keeps the analysis job running. */
@Composable
fun InspectionLoadingDialog(onDismissPopup: () -> Unit) {
    InspectionDialogFrame(onDismissRequest = onDismissPopup) {
        InspectionLoadingContent(onDismissPopup)
    }
}

@Composable
private fun InspectionLoadingContent(onDismissPopup: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(
            max = (LocalConfiguration.current.screenHeightDp - 48).coerceAtLeast(1).dp,
        ),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
                .padding(start = 24.dp, top = 40.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.dundeuni_mascot_phone),
                contentDescription = "검사 중인 든든이",
                modifier = Modifier.size(96.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "잠시만 기다려 주세요",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 27.sp,
                ),
                color = Color(0xFF171D29),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "든든이가 꼼꼼하게 검사하고 있어요!",
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                color = Color(0xFF454A53),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = Color(0xFF348BFF),
                strokeWidth = 4.dp,
                trackColor = Color(0xFFC8DFFF),
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.height(26.dp))
            Button(
                onClick = onDismissPopup,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryMidnight),
            ) {
                Text(
                    text = "닫고 검사 계속하기 →",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    ),
                    color = Color.White,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ⓘ  팝업을 닫아도 검사는 안전하게 계속 진행돼요",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    ),
                    color = Color(0xFF8B9099),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(name = "검사 진행 로딩 팝업", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun InspectionLoadingDialogPreview() {
    DundueniFETheme {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0x73000000))
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            InspectionLoadingContent(onDismissPopup = {})
        }
    }
}

/** Retry and cancel are actions supplied by the caller; hiding is never treated as cancel. */
@Composable
fun InspectionErrorDialog(
    message: String,
    onRetry: () -> Unit,
    onCancelInspection: () -> Unit,
    showRetry: Boolean = true,
) {
    InspectionDialogFrame(onDismissRequest = {}, dismissible = false) {
        InspectionErrorContent(message, onRetry, onCancelInspection, showRetry)
    }
}

@Composable
private fun InspectionErrorContent(
    message: String,
    onRetry: () -> Unit,
    onCancelInspection: () -> Unit,
    showRetry: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(
            max = (LocalConfiguration.current.screenHeightDp - 48).coerceAtLeast(1).dp,
        ),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
                .padding(start = 24.dp, top = 40.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.dundeuni_mascot_nervous),
                contentDescription = "걱정하는 든든이",
                modifier = Modifier.size(96.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "검사를 완료하지 못했어요",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 27.sp,
                ),
                color = Color(0xFF171D29),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                color = Color(0xFF454A53),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            if (showRetry) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryMidnight),
                ) {
                    Text(
                        text = "다시 시도",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                        ),
                        color = Color.White,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            Button(
                onClick = onCancelInspection,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E5EC)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF0F3F7),
                    contentColor = Color(0xFF535D6C),
                ),
            ) {
                Text(
                    text = "검사 취소",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    ),
                )
            }
        }
    }
}

@Composable
fun InspectionLimitDialog(onUpgrade: () -> Unit, onLater: () -> Unit) {
    InspectionDialogFrame(onDismissRequest = onLater) {
        InspectionLimitContent(onUpgrade, onLater)
    }
}

@Composable
private fun InspectionLimitContent(onUpgrade: () -> Unit, onLater: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(
            max = (LocalConfiguration.current.screenHeightDp - 48).coerceAtLeast(1).dp,
        ),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
                .padding(start = 24.dp, top = 40.dp, end = 24.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.dundeuni_mascot_nervous),
                contentDescription = "든든이 캐릭터",
                modifier = Modifier.size(96.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "검사 횟수를 다 썼어요",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                ),
                color = PrimaryMidnight,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = buildAnnotatedString {
                    append("이번 달 무료 검사 ")
                    withStyle(SpanStyle(
                        color = PrimaryMidnight,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                    )) {
                        append("${BasicInspectionQuota.LIMIT}회")
                    }
                    append("를 모두 사용하셨습니다.")
                },
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                color = Color(0xFF535D6C),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "무제한 검사로 업그레이드할까요?",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp,
                ),
                color = ProtectiveBlue,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onUpgrade,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ProtectiveBlue),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "요금제 업그레이드",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Spacer(Modifier.size(8.dp))
                    Image(
                        painter = painterResource(R.drawable.auth_arrow),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onLater,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E5EC)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF0F3F7),
                    contentColor = Color(0xFF8D9BAE),
                ),
            ) {
                Text(
                    text = "다음에 하기",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Preview(name = "검사 횟수 소진 팝업", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun InspectionLimitDialogPreview() {
    DundueniFETheme {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0x73000000))
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            InspectionLimitContent(onUpgrade = {}, onLater = {})
        }
    }
}

@Composable
private fun InspectionDialogFrame(
    onDismissRequest: () -> Unit,
    dismissible: Boolean = true,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
        ),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(window) {
            if (window == null) {
                onDispose { }
            } else {
                val originalDimAmount = window.attributes.dimAmount
                val hadDimBehind = window.attributes.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setDimAmount(0.45f)
                onDispose {
                    window.setDimAmount(originalDimAmount)
                    if (!hadDimBehind) window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                }
            }
        }
        content()
    }
}

/** Shared card used by Activity dialogs and the service overlay window. */
@Composable
private fun InspectionStatusContent(
    @DrawableRes mascotResId: Int,
    mascotDescription: String,
    title: String,
    description: String,
    actions: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(mascotResId),
                contentDescription = mascotDescription,
                modifier = Modifier.size(112.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            actions()
        }
    }
}

@Composable
fun InspectionCompletedContent(onViewResult: () -> Unit, onClose: () -> Unit) {
    InspectionStatusContent(
        mascotResId = R.drawable.dundeuni_mascot_thumbs_up,
        mascotDescription = "검사를 완료한 든든이",
        title = "검사가 완료됐어요 !",
        description = "검사 결과를 확인해 주세요.",
    ) {
        DundueniButton("검사 결과 조회", onViewResult, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().height(DundueniDimens.ButtonHeight),
            shape = DundueniDefaultShape,
        ) {
            Text("닫기", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
