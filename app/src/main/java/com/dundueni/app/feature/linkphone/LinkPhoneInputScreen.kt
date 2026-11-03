package com.dundueni.app.feature.linkphone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dundueni.app.ui.component.DundueniButton
import com.dundueni.app.ui.component.DundueniButtonType

/**
 * 링크(URL) 및 전화번호 입력, 유효성 검사, 붙여넣기, 삭제 및 검사 기능을 제공하는 Composable 화면이다.
 */
@Composable
fun LinkPhoneInputScreen(
    onInspect: (link: String, phone: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var linkInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    val linkError = LinkPhoneInputValidator.linkError(linkInput)
    val phoneError = LinkPhoneInputValidator.phoneError(phoneInput)
    val hasContent = linkInput.isNotBlank() || phoneInput.isNotBlank()
    val isValid = hasContent && linkError == null && phoneError == null

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "링크·전화번호 검사",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "의심되는 웹 링크(URL)나 발신자 전화번호를 입력하여 피싱 여부를 검사해 보세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 빠른 클립보드 붙여넣기
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    clipboardManager.getText()?.text?.let { pastedText ->
                        if (pastedText.contains("http") || pastedText.contains("www") || pastedText.contains(".")) {
                            linkInput = pastedText.trim()
                        } else {
                            phoneInput = pastedText.trim()
                        }
                    }
                }
            ) {
                Text("클립보드 내용 붙여넣기")
            }
        }

        // 링크 입력 필드
        OutlinedTextField(
            value = linkInput,
            onValueChange = { linkInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("웹 링크 (URL)") },
            placeholder = { Text("https://example.com 또는 단축 URL") },
            singleLine = true,
            isError = linkError != null,
            trailingIcon = {
                if (linkInput.isNotEmpty()) {
                    IconButton(onClick = { linkInput = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "링크 지우기")
                    }
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next
            )
        )
        if (linkError != null) {
            Text(
                text = linkError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // 전화번호 입력 필드
        OutlinedTextField(
            value = phoneInput,
            onValueChange = { phoneInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("전화번호") },
            placeholder = { Text("010-0000-0000") },
            singleLine = true,
            isError = phoneError != null,
            trailingIcon = {
                if (phoneInput.isNotEmpty()) {
                    IconButton(onClick = { phoneInput = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "전화번호 지우기")
                    }
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (isValid) {
                        onInspect(linkInput, phoneInput)
                    }
                }
            )
        )
        if (phoneError != null) {
            Text(
                text = phoneError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 하단 액션 버튼들
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (hasContent) {
                DundueniButton(
                    text = "지우기",
                    onClick = {
                        linkInput = ""
                        phoneInput = ""
                    },
                    type = DundueniButtonType.DANGER,
                    modifier = Modifier.weight(1f)
                )
            }

            DundueniButton(
                text = "검사하기",
                onClick = { onInspect(linkInput, phoneInput) },
                enabled = isValid,
                modifier = Modifier.weight(if (hasContent) 1f else 2f)
            )
        }

        DundueniButton(
            text = "닫기",
            onClick = onClose,
            type = DundueniButtonType.SECONDARY,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
