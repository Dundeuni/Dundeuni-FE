package com.dundueni.app.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.dundueni.app.ui.theme.DundueniFETheme
import com.dundueni.app.ui.theme.LocalEasyMode

@Preview(name = "설정 · 일반 모드", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun NormalSettingsPreview() = SettingsModePreview(easy = false)

@Preview(name = "설정 · 쉬운 모드", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun EasySettingsPreview() = SettingsModePreview(easy = true)

@Composable
private fun SettingsModePreview(easy: Boolean) = DundueniFETheme {
    CompositionLocalProvider(LocalEasyMode provides easy) {
        SettingsScreen(onNavigationSelected = {})
    }
}
