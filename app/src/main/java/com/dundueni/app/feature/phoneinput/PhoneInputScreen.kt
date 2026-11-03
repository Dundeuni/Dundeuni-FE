package com.dundueni.app.feature.phoneinput

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dundueni.app.ui.component.DundueniButton
import com.dundueni.app.ui.component.DundueniButtonType

/**
 * 전화번호 입력, 포맷팅, 형식 안내, 붙여넣기, 삭제, 조회/검사 기능을 제공하는 Composable 화면.
 * 명세(I-03): 전화번호 입력, 형식 안내, 조회 버튼
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneInputScreen(
    onInspect: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rawInput by remember { mutableStateOf("") }
    val formattedNumber = formatPhoneNumber(rawInput)
    val isValid = isValidPhoneNumber(rawInput)
    val isError = rawInput.isNotBlank() && !isValid

    val clipboardManager = LocalClipboardManager.current

    BackHandler(onBack = onClose)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "전화번호 검사",
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
                text = "의심스러운 전화번호나 발신번호를 입력하여 신고 및 스팸 정보를 조회해 보세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 상단에 붙여넣기 / 지우기 버튼 영역
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        clipboardManager.getText()?.text?.let { pastedText ->
                            rawInput = pastedText
                        }
                    }
                ) {
                    Text("클립보드 붙여넣기")
                }

                if (rawInput.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { rawInput = "" }
                    ) {
                        Text("내용 지우기")
                    }
                }
            }

            // 전화번호 입력 필드 및 형식 안내 (명세 I-03 충족)
            OutlinedTextField(
                value = formattedNumber,
                onValueChange = { input ->
                    // 숫자와 '+'만 허용하여 rawInput 갱신
                    rawInput = input.filter { it.isDigit() || it == '+' }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("전화번호") },
                placeholder = { Text("예: 010-1234-5678") },
                singleLine = true,
                isError = isError,
                supportingText = {
                    when {
                        isError -> Text(
                            text = "올바른 전화번호 형식이 아닙니다 (예: 010-1234-5678 또는 8~11자리)",
                            color = MaterialTheme.colorScheme.error
                        )
                        isValid -> Text(
                            text = "올바른 전화번호 형식입니다",
                            color = MaterialTheme.colorScheme.primary
                        )
                        else -> Text(
                            text = "전화번호를 입력해 주세요 (예: 010-1234-5678)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                trailingIcon = {
                    if (rawInput.isNotEmpty()) {
                        IconButton(
                            onClick = { rawInput = "" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "전체 삭제"
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (isValid) {
                            onInspect(formattedNumber)
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // 하단 액션 버튼들 (조회 / 검사)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 지우기 버튼 (DANGER)
                if (rawInput.isNotEmpty()) {
                    DundueniButton(
                        text = "지우기",
                        onClick = { rawInput = "" },
                        type = DundueniButtonType.DANGER,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 조회 버튼 (PRIMARY - 명세 I-03)
                DundueniButton(
                    text = "조회하기",
                    onClick = { onInspect(formattedNumber) },
                    enabled = isValid,
                    modifier = Modifier.weight(if (rawInput.isNotEmpty()) 1f else 2f)
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

/**
 * 한국 전화번호 포맷팅 함수.
 */
internal fun formatPhoneNumber(input: String): String {
    val digits = input.filter { it.isDigit() }
    if (digits.isEmpty()) return input.filter { it == '+' }
    val prefix = if (input.startsWith("+")) "+" else ""

    return prefix + when {
        digits.startsWith("02") -> {
            when {
                digits.length <= 2 -> digits
                digits.length <= 5 -> "${digits.substring(0, 2)}-${digits.substring(2)}"
                digits.length <= 9 -> "${digits.substring(0, 2)}-${digits.substring(2, 5)}-${digits.substring(5)}"
                else -> "${digits.substring(0, 2)}-${digits.substring(2, 6)}-${digits.substring(6, minOf(digits.length, 10))}"
            }
        }
        digits.length >= 8 && (digits.startsWith("15") || digits.startsWith("16") || digits.startsWith("18")) && digits.length <= 8 -> {
            "${digits.substring(0, 4)}-${digits.substring(4)}"
        }
        else -> {
            when {
                digits.length <= 3 -> digits
                digits.length <= 7 -> "${digits.substring(0, 3)}-${digits.substring(3)}"
                digits.length <= 10 -> "${digits.substring(0, 3)}-${digits.substring(3, 6)}-${digits.substring(6)}"
                else -> "${digits.substring(0, 3)}-${digits.substring(3, 7)}-${digits.substring(7, minOf(digits.length, 11))}"
            }
        }
    }
}

/**
 * 전화번호 형식 유효성 검사 함수.
 */
internal fun isValidPhoneNumber(input: String): Boolean {
    val digits = input.filter { it.isDigit() }
    if (digits.length !in 8..11) return false
    if (digits.startsWith("02")) return digits.length in 9..10
    if (digits.startsWith("0")) return digits.length in 10..11
    if (digits.startsWith("15") || digits.startsWith("16") || digits.startsWith("18")) return digits.length == 8
    return true
}
