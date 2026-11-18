package com.dundueni.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dundueni.app.ui.theme.DundueniFETheme

@Preview(name = "공통 검사 실패 팝업", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun InspectionErrorDialogPreview() {
    DundueniFETheme {
        InspectionErrorDialog(
            message = "네트워크 연결을 확인한 뒤 다시 시도해 주세요.",
            onRetry = {},
            onCancelInspection = {},
        )
    }
}
