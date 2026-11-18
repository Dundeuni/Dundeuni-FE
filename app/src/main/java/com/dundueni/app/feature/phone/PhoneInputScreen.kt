package com.dundueni.app.feature.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.dundueni.app.ui.theme.BrandInk
import com.dundueni.app.ui.theme.DundueniDimens
import com.dundueni.app.ui.theme.DundueniPillShape
import com.dundueni.app.ui.theme.PageBackground
import com.dundueni.app.ui.theme.ProtectiveBlue
import com.dundueni.app.ui.theme.SecondaryInk
import com.dundueni.app.ui.theme.SoftBlue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun PhoneInputScreen(
    onInspect: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isInspecting: Boolean = false,
) {
    var phoneInput by rememberSaveable { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val phoneError = PhoneInputValidator.phoneError(phoneInput)
    val isValid = phoneInput.isNotBlank() && phoneError == null

    BoxWithConstraints(modifier.fillMaxSize().background(PageBackground).imePadding()) {
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .height(IntrinsicSize.Min),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).background(Color.White).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기", tint = BrandInk)
                }
                Text("전화번호 검사", color = BrandInk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = buildAnnotatedString {
                        append("모르는 번호, 안전한지\n")
                        withStyle(SpanStyle(color = ProtectiveBlue)) { append("든든이") }
                        append("가 먼저 확인해 드려요")
                    },
                    color = BrandInk, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "의심되는 발신자 전화번호를 입력하거나 클립보드에서 붙여넣어 스미싱 및 사기 여부를 검사해 보세요.",
                    style = MaterialTheme.typography.bodySmall, color = SecondaryInk,
                )
                Spacer(Modifier.height(18.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            "검사할 번호", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                            color = BrandInk, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        )
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            readOnly = isInspecting,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                errorContainerColor = Color.White,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                errorBorderColor = Color.Transparent,
                                cursorColor = ProtectiveBlue,
                            ),
                            textStyle = TextStyle(color = BrandInk, fontSize = 20.sp, textAlign = TextAlign.Center),
                            placeholder = {
                                Text(
                                    "번호를 입력해 주세요", modifier = Modifier.fillMaxWidth(),
                                    color = SecondaryInk.copy(alpha = 0.45f),
                                    fontSize = 18.sp, textAlign = TextAlign.Center,
                                )
                            },
                            singleLine = true,
                            isError = phoneError != null,
                            trailingIcon = {
                                if (phoneInput.isNotEmpty()) {
                                    IconButton(onClick = { phoneInput = "" }, enabled = !isInspecting) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "전화번호 지우기")
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { if (isValid && !isInspecting) onInspect(phoneInput) },
                            ),
                        )
                        HorizontalDivider(color = SoftBlue)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                enabled = !isInspecting,
                                onClick = {
                                    clipboardManager.getText()?.text?.let { pasted ->
                                        phoneInput = pasted.trim()
                                    }
                                },
                                modifier = Modifier.height(32.dp).background(SoftBlue, DundueniPillShape),
                                colors = ButtonDefaults.textButtonColors(contentColor = ProtectiveBlue),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            ) {
                                Text("클립보드 붙여넣기", fontSize = 11.sp)
                            }
                            TextButton(
                                onClick = { phoneInput = "" },
                                enabled = phoneInput.isNotEmpty() && !isInspecting,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            ) {
                                Text("전체 지우기", fontSize = 11.sp)
                            }
                        }
                    }
                }
                if (phoneError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = phoneError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "ⓘ  전화번호를 입력하거나 복사한 번호를 붙여넣을 수 있어요.",
                    color = SecondaryInk, style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("ⓘ", color = ProtectiveBlue, fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("든든이 검사 안내", color = BrandInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "입력한 번호로 검사를 요청하고\n결과를 화면에서 확인할 수 있어요.",
                                color = SecondaryInk, style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { onInspect(phoneInput) },
                    enabled = isValid && !isInspecting,
                    modifier = Modifier.fillMaxWidth().height(DundueniDimens.ButtonHeight),
                    shape = DundueniPillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ProtectiveBlue,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF95B6ED),
                        disabledContentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("검사하기", style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                    Text("닫기", color = SecondaryInk)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
