package com.dundueni.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// 든든이 앱의 기본 라이트 컬러 테마 설정

/**
 * 든든이 앱의 공통 UI 디자인 규칙을 하나로 묶어 적용하는 파일.
 *
 * Color.kt → 앱 공통 색상
 * Type.kt  → 앱 공통 글자 스타일
 * Shape.kt → 앱 공통 모서리 형태
 *
 * 위 규칙들을 MaterialTheme에 등록한다.
 *
 * 화면을 DundueniFETheme으로 감싸면
 * 내부의 모든 Compose UI에서 공통 색상, 글꼴, 모양을 사용할 수 있다.
 */



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



