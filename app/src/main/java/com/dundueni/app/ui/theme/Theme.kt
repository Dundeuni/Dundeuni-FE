package com.dundueni.app.ui.theme

import android.content.SharedPreferences
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.dundueni.app.feature.floatingbutton.FloatingButtonPreferences

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
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(FloatingButtonPreferences.PREFERENCES_NAME, android.content.Context.MODE_PRIVATE)
    }
    var appTextScale by remember(context) {
        mutableFloatStateOf(FloatingButtonPreferences.appTextScale(context))
    }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == FloatingButtonPreferences.KEY_FONT_SIZE || key == FloatingButtonPreferences.KEY_FONT_SCALE) {
                appTextScale = FloatingButtonPreferences.appTextScale(context)
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * appTextScale),
    ) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = Typography,
            shapes = DundueniShapes,
            content = content,
        )
    }
}



