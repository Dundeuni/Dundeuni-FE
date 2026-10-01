package com.dundueni.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 든든이 앱에서 공통으로 사용하는 모서리 모양을 정의하는 파일.
 *
 * 버튼, 카드, 배지처럼 여러 UI에서 반복되는
 * 둥근 모서리 규칙을 한 곳에서 관리한다.
 */

// 일반 카드 / 버튼 등에 사용하는 기본 모서리
val DundueniDefaultShape = RoundedCornerShape(12.dp)

// 배지, 필터 칩처럼 양 끝이 완전히 둥근 형태
val DundueniPillShape = RoundedCornerShape(50)

// MaterialTheme에서 사용할 기본 Shape 규칙
val DundueniShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)



