package com.dundueni.app.feature.url

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
fun UrlInputScreen(
    onInspect: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var urlInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val urlError = UrlInputValidator.urlError(urlInput)
    val isValid = urlInput.isNotBlank() && urlError == null

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "링크(URL) 검사",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "의심되는 웹 링크나 단축 URL을 입력하거나 클립보드에서 붙여넣어 피싱 여부를 검사해 보세요.",
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
                        urlInput = pasted.trim()
                    }
                }
            ) {
                Text("클립보드 붙여넣기")
            }
        }

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("웹 링크 (URL)") },
            placeholder = { Text("https://example.com") },
            singleLine = true,
            isError = urlError != null,
            trailingIcon = {
                if (urlInput.isNotEmpty()) {
                    IconButton(onClick = { urlInput = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "링크 지우기")
                    }
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (isValid) onInspect(urlInput)
                }
            )
        )
        if (urlError != null) {
            Text(
                text = urlError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (urlInput.isNotEmpty()) {
                DundueniButton(
                    text = "지우기",
                    onClick = { urlInput = "" },
                    type = DundueniButtonType.DANGER,
                    modifier = Modifier.weight(1f)
                )
            }
            DundueniButton(
                text = "검사하기",
                onClick = { onInspect(urlInput) },
                enabled = isValid,
                modifier = Modifier.weight(if (urlInput.isNotEmpty()) 1f else 2f)
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
