package com.dundueni.app.feature.textinput

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dundueni.app.ui.component.DundueniButton
import com.dundueni.app.ui.component.DundueniButtonType

/**
 * 텍스트 입력, 붙여넣기, 글자 수 표시, 삭제(지우기), 검사 기능을 제공하는 Composable 화면이다.
 * 명세(I-02): 텍스트 입력창, 붙여넣기, 글자 수, 검사 버튼
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInputScreen(
    onInspect: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    BackHandler(onBack = onClose)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "텍스트 검사",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "의심되는 문자메시지, 피싱 내용, 또는 텍스트를 입력하거나 붙여넣어 검사해 보세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 상단에 빠른 붙여넣기 / 지우기 버튼 영역
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        clipboardManager.getText()?.text?.let { pastedText ->
                            textInput = pastedText
                        }
                    }
                ) {
                    Text("클립보드 붙여넣기")
                }

                if (textInput.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { textInput = "" }
                    ) {
                        Text("내용 지우기")
                    }
                }
            }

            // 텍스트 입력 필드 및 실시간 글자 수 표시 (명세 I-02 충족)
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("여기에 텍스트를 입력하세요...") },
                singleLine = false,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "${textInput.length}자",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                trailingIcon = {
                    if (textInput.isNotEmpty()) {
                        IconButton(
                            onClick = { textInput = "" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "전체 삭제"
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (textInput.isNotBlank()) {
                            onInspect(textInput)
                        }
                    }
                )
            )

            // 하단 액션 버튼들
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 지우기 버튼 (DANGER)
                if (textInput.isNotEmpty()) {
                    DundueniButton(
                        text = "지우기",
                        onClick = { textInput = "" },
                        type = DundueniButtonType.DANGER,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 검사하기 버튼 (PRIMARY)
                DundueniButton(
                    text = "검사하기",
                    onClick = { onInspect(textInput) },
                    enabled = textInput.isNotBlank(),
                    modifier = Modifier.weight(if (textInput.isNotEmpty()) 1f else 2f)
                )
            }

            // 닫기 버튼
            DundueniButton(
                text = "닫기",
                onClick = onClose,
                type = DundueniButtonType.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
