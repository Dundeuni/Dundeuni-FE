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
 * 든든이 앱에서 공통으로 사용하는 버튼 컴포넌트.
 *
 * 화면마다 Button을 새로 만들지 않고,
 * 이 컴포넌트를 재사용해서 버튼 디자인을 통일한다.
 *
 * 사용 예시:
 *
 * DundueniButton(
 *     text = "검사하기",
 *     onClick = { /* 클릭 시 실행할 코드 */ }
 * )
 *
 * 버튼 종류 변경:
 *
 * DundueniButton(
 *     text = "삭제",
 *     type = DundueniButtonType.DANGER,
 *     onClick = { }
 * )
 *
 * 아이콘도 함께 넣을 수 있다.
 */

// 버튼 용도에 따라 디자인을 구분
enum class DundueniButtonType {
    PRIMARY,   // 기본 주요 버튼
    SECONDARY, // 보조 버튼
    DANGER     // 삭제, 경고 등 위험 동작 버튼
}

@Composable
fun DundueniButton(

    // 버튼에 표시할 글자
    text: String,

    // 버튼을 눌렀을 때 실행할 코드
    onClick: () -> Unit,

    // 버튼 크기, 위치 등을 화면에서 추가로 설정할 때 사용
    modifier: Modifier = Modifier,

    // 버튼 종류. 기본값은 PRIMARY
    type: DundueniButtonType = DundueniButtonType.PRIMARY,

    // false로 설정하면 버튼을 누를 수 없음
    enabled: Boolean = true,

    // 필요할 경우 버튼 왼쪽에 아이콘 추가
    icon: (@Composable () -> Unit)? = null
) {

    // 버튼 종류에 따라 배경색 선택
    val containerColor = when (type) {
        DundueniButtonType.PRIMARY -> MaterialTheme.colorScheme.primary
        DundueniButtonType.SECONDARY -> MaterialTheme.colorScheme.secondary
        DundueniButtonType.DANGER -> MaterialTheme.colorScheme.error
    }

    // 버튼 종류에 따라 글자와 아이콘 색상 선택
    val contentColor = when (type) {
        DundueniButtonType.PRIMARY -> MaterialTheme.colorScheme.onPrimary
        DundueniButtonType.SECONDARY -> MaterialTheme.colorScheme.onSecondary
        DundueniButtonType.DANGER -> MaterialTheme.colorScheme.onError
    }

    // 실제 Material3 버튼 생성
    Button(
        onClick = onClick,

        // 모든 든든이 버튼 높이를 공통 규격으로 통일
        modifier = modifier.height(DundueniDimens.ButtonHeight),

        enabled = enabled,

        // 공통 둥근 모서리 적용
        shape = DundueniDefaultShape,

        // 위에서 결정한 배경색과 글자색 적용
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {

        // 아이콘과 텍스트를 가로로 배치
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 아이콘이 전달된 경우에만 표시
            if (icon != null) {
                icon()

                // 아이콘과 글자 사이 간격
                Spacer(
                    modifier = Modifier.width(8.dp)
                )
            }

            // 버튼 글자 표시
            Text(
                text = text
            )
        }
    }
}