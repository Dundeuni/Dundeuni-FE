package com.dundueni.app.feature.phone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

@Composable
fun PhoneInputScreen(
    onInspect: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var phoneInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val phoneError = PhoneInputValidator.phoneError(phoneInput)
    val isValid = phoneInput.isNotBlank() && phoneError == null

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "전화번호 검사",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "의심되는 발신자 전화번호를 입력하거나 클립보드에서 붙여넣어 스미싱 및 사기 여부를 검사해 보세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    clipboardManager.getText()?.text?.let { pasted ->
                        phoneInput = pasted.trim()
                    }
                }
            ) {
                Text("클립보드 붙여넣기")
            }
        }

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
                    if (isValid) onInspect(phoneInput)
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (phoneInput.isNotEmpty()) {
                DundueniButton(
                    text = "지우기",
                    onClick = { phoneInput = "" },
                    type = DundueniButtonType.DANGER,
                    modifier = Modifier.weight(1f)
                )
            }
            DundueniButton(
                text = "검사하기",
                onClick = { onInspect(phoneInput) },
                enabled = isValid,
                modifier = Modifier.weight(if (phoneInput.isNotEmpty()) 1f else 2f)
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
