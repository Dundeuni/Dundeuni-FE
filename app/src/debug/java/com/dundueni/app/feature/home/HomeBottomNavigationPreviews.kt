package com.dundueni.app.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.dundueni.app.ui.theme.DundueniFETheme
import com.dundueni.app.ui.theme.LocalEasyMode

@Preview(name = "쉬운 모드 하단 바 · 홈 선택", widthDp = 390, showBackground = true)
@Composable
private fun EasyHomeNavigationPreview() = EasyNavigationPreview(HomeDestination.HOME)

@Preview(name = "쉬운 모드 하단 바 · 마이페이지 선택", widthDp = 390, showBackground = true)
@Composable
private fun EasyMyPageNavigationPreview() = EasyNavigationPreview(HomeDestination.MY_SHELTER)

@Composable
private fun EasyNavigationPreview(selectedDestination: HomeDestination) = DundueniFETheme {
    CompositionLocalProvider(LocalEasyMode provides true) {
        HomeBottomNavigation(selectedDestination, onNavigationSelected = {})
    }
}
