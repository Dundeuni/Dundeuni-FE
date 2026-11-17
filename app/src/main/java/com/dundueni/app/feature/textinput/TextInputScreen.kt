package com.dundueni.app.feature.textinput

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dundueni.app.ui.theme.BrandInk
import com.dundueni.app.ui.theme.CanvasSubLayer
import com.dundueni.app.ui.theme.DisabledSurface
import com.dundueni.app.ui.theme.DundueniDimens
import com.dundueni.app.ui.theme.DundueniPillShape
import com.dundueni.app.ui.theme.PageBackground
import com.dundueni.app.ui.theme.ProtectiveBlue
import com.dundueni.app.ui.theme.SecondaryInk
import com.dundueni.app.ui.theme.SoftBlue
import com.dundueni.app.ui.theme.SurfaceLight

/** 텍스트 입력, 붙여넣기, 삭제(지우기), 검사 기능을 제공하는 Composable 화면이다. */
@Composable
fun TextInputScreen(
    textInput: String,
    onTextChange: (String) -> Unit,
    onInspect: (String) -> Unit,
    onClose: () -> Unit,
    isInspecting: Boolean = false,
    showLengthError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    Column(modifier.fillMaxSize().background(PageBackground).imePadding()) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).background(SurfaceLight)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기", tint = BrandInk)
                }
                Text("텍스트 검사", color = BrandInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                Spacer(Modifier.height(24.dp))
                Text(
                    buildAnnotatedString {
                        append("의심스러운 문자, 안전한지\n")
                        withStyle(SpanStyle(color = ProtectiveBlue)) { append("든든이") }
                        append("가 먼저 확인해 드려요")
                    },
                    color = BrandInk, fontSize = 20.sp, lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "의심되는 문자메시지, 피싱 내용, 또는 텍스트를 입력하거나 붙여넣어 검사해 보세요.",
                    style = MaterialTheme.typography.bodySmall, color = SecondaryInk,
                )
                if (isInspecting) {
                    Spacer(Modifier.height(8.dp))
                    Text("검사가 진행 중이에요. 완료되면 결과가 표시됩니다.")
                }
                Spacer(Modifier.height(18.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                    color = SurfaceLight, shadowElevation = 1.dp,
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("▤", color = ProtectiveBlue, fontSize = 17.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("메시지 원문 입력", color = BrandInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            TextButton(
                                enabled = !isInspecting,
                                onClick = {
                                    clipboardManager.getText()?.text?.let { pastedText ->
                                        onTextChange(pastedText)
                                    }
                                },
                                modifier = Modifier.background(SoftBlue, DundueniPillShape),
                                colors = ButtonDefaults.textButtonColors(contentColor = ProtectiveBlue),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            ) { Text("클립보드 붙여넣기", fontSize = 11.sp) }
                        }
                        Spacer(Modifier.height(12.dp))
                        Surface(color = SoftBlue, shape = RoundedCornerShape(DundueniDimens.DefaultRadius)) {
                            Column {
                                OutlinedTextField(
                                    value = textInput,
                                    onValueChange = onTextChange,
                                    enabled = !isInspecting,
                                    shape = RoundedCornerShape(DundueniDimens.DefaultRadius),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SoftBlue,
                                        unfocusedContainerColor = SoftBlue,
                                        disabledContainerColor = SoftBlue,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        disabledBorderColor = Color.Transparent,
                                        cursorColor = ProtectiveBlue,
                                    ),
                                    textStyle = TextStyle(color = BrandInk, fontSize = 14.sp, lineHeight = 21.sp),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 170.dp, max = 220.dp),
                                    placeholder = {
                                        Text(
                                            "의심되는 문자 메시지나 메신저 대화 내용을 입력하거나 붙여넣어 주세요.\n(예: [우체국 택배] 배송지 확인이 필요합니다.)",
                                            color = SecondaryInk.copy(alpha = 0.65f),
                                            fontSize = 13.sp, lineHeight = 20.sp,
                                        )
                                    },
                                    singleLine = false,
                                    trailingIcon = {
                                        if (textInput.isNotEmpty()) {
                                            IconButton(
                                                enabled = !isInspecting,
                                                onClick = { onTextChange("") },
                                            ) {
                                                Icon(Icons.Default.Clear, contentDescription = "전체 삭제")
                                            }
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        if (!isInspecting) onInspect(textInput)
                                    }),
                                )
                                Text(
                                    textInput.codePointCount(0, textInput.length).toString() + " / 3,000자",
                                    modifier = Modifier.fillMaxWidth().padding(end = 14.dp, bottom = 10.dp),
                                    textAlign = TextAlign.End, color = SecondaryInk, fontSize = 11.sp,
                                )
                            }
                        }
                        if (textInput.isNotEmpty()) {
                            TextButton(
                                enabled = !isInspecting,
                                onClick = { onTextChange("") },
                                modifier = Modifier.align(Alignment.End),
                            ) { Text("내용 지우기", color = SecondaryInk, fontSize = 12.sp) }
                        }
                    }
                }
                if (showLengthError) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "텍스트는 앞뒤 공백을 제외하고 5~3,000자로 입력해 주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    color = SurfaceLight, shadowElevation = 1.dp,
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(32.dp).background(SoftBlue, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Text("ⓘ", color = ProtectiveBlue, fontSize = 18.sp) }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("문자 검사 안내", color = BrandInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "입력한 내용을 검사해 결과 화면에서 확인할 수 있어요.",
                                color = SecondaryInk, style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (textInput.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { onTextChange("") }, enabled = !isInspecting,
                            modifier = Modifier.weight(1f).height(DundueniDimens.ButtonHeight),
                            shape = DundueniPillShape,
                            border = BorderStroke(1.dp, CanvasSubLayer),
                        ) { Text("지우기", color = SecondaryInk) }
                    }
                    Button(
                        onClick = { onInspect(textInput) }, enabled = !isInspecting,
                        modifier = Modifier.weight(if (textInput.isNotEmpty()) 1f else 2f)
                            .height(DundueniDimens.ButtonHeight),
                        shape = DundueniPillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProtectiveBlue, contentColor = SurfaceLight,
                            disabledContainerColor = DisabledSurface, disabledContentColor = SecondaryInk,
                        ),
                    ) {
                        Text(
                            if (isInspecting) "검사 중..." else "검사하기",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                    Text("닫기", color = SecondaryInk)
                }
                Spacer(Modifier.height(12.dp))
            }
    }
}
