package com.dundueni.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// 든든이 앱의 기본 라이트 컬러 테마 설정
private val LightColorScheme = lightColorScheme(
    primary = PrimaryMidnight,
    primaryContainer = PrimaryContainer,
    secondary = ProtectiveBlue,

    background = CanvasSubLayer,
    surface = SurfaceLight,

    error = Danger
)

// 든든이 앱 전체에 색상, 글꼴, 모양 규칙을 공통 적용하는 테마
@Composable
fun DundueniFETheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = DundueniShapes,
        content = content
    )
}



