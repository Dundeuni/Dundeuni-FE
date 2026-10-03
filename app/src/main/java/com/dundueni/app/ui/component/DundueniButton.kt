package com.dundueni.app.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dundueni.app.ui.theme.DundueniDefaultShape
import com.dundueni.app.ui.theme.DundueniDimens

/**
 * 든든이 앱에서 일반적인 주요 버튼을 통일하기 위해 사용하는 버튼 컴포넌트.
 *
 * 화면마다 Button을 새로 만들지 않고
 * 이 컴포넌트를 재사용해서 버튼 디자인을 통일한다.
 *
 * 공통 적용 요소
 * - Color.kt   → MaterialTheme.colorScheme
 * - Type.kt    → MaterialTheme.typography.bodyLarge
 * - Shape.kt   → DundueniDefaultShape
 * - Dimens.kt  → DundueniDimens.ButtonHeight
 *
 * 사용 예시:
 *
 * DundueniButton(
 *     text = "검사하기",
 *     onClick = { }
 * )
 */

// 버튼 용도에 따라 디자인 구분
enum class DundueniButtonType {
    PRIMARY,   // 기본 주요 버튼
    SECONDARY, // 보조 버튼
    DANGER     // 삭제, 경고 등 위험 동작 버튼
}

@Composable
fun DundueniButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: DundueniButtonType = DundueniButtonType.PRIMARY,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {

    // 버튼 종류에 따라 배경색 선택
    val containerColor = when (type) {
        DundueniButtonType.PRIMARY ->
            MaterialTheme.colorScheme.primary

        DundueniButtonType.SECONDARY ->
            MaterialTheme.colorScheme.secondary

        DundueniButtonType.DANGER ->
            MaterialTheme.colorScheme.error
    }

    // 버튼 종류에 따라 글자 / 아이콘 색상 선택
    val contentColor = when (type) {
        DundueniButtonType.PRIMARY ->
            MaterialTheme.colorScheme.onPrimary

        DundueniButtonType.SECONDARY ->
            MaterialTheme.colorScheme.onSecondary

        DundueniButtonType.DANGER ->
            MaterialTheme.colorScheme.onError
    }

    Button(
        onClick = onClick,

        // Dimens.kt의 공통 버튼 높이 사용
        modifier = modifier.height(
            DundueniDimens.ButtonHeight
        ),

        enabled = enabled,

        // Shape.kt의 공통 버튼 모서리 사용
        shape = DundueniDefaultShape,

        // Color.kt → Theme.kt에 등록된 공통 색상 사용
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 아이콘이 전달된 경우에만 표시
            if (icon != null) {
                icon()

                Spacer(
                    modifier = Modifier.width(8.dp)
                )
            }

            Text(
                text = text,

                // Type.kt → Theme.kt에 등록된 공통 Typography 사용
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}