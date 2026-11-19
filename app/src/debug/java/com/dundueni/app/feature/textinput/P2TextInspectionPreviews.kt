package com.dundueni.app.feature.textinput

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dundueni.app.ui.theme.DundueniFETheme

@Preview(name = "문자 검사 · 기본 입력", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun TextInspectionInputPreview() {
    DundueniFETheme {
        TextInputScreen(textInput = "", onTextChange = {}, onInspect = {}, onClose = {})
    }
}
