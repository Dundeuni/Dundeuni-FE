package com.dundueni.app.feature.payment

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dundueni.app.ui.component.InspectionCompletedContent
import com.dundueni.app.ui.component.InspectionLimitDialog
import com.dundueni.app.ui.theme.DundueniFETheme

// Image, screen, and text inspections use this same completion content.
// This previews the Compose card only; WindowManager and the service are not started.
@Preview(name = "P3 검사 완료 · 결과 확인 팝업", widthDp = 390, heightDp = 440, showBackground = true)
@Composable
private fun InspectionCompletedPreview() = DundueniFETheme {
    InspectionCompletedContent(onViewResult = {}, onClose = {})
}

@Preview(name = "P3 검사 제한 · 횟수 소진 팝업", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun InspectionLimitPreview() = DundueniFETheme {
    InspectionLimitDialog(onUpgrade = {}, onLater = {})
}

@Preview(name = "P3 멤버십 · 기본 검사 0회", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun PaymentNoRemainingPreview() = DundueniFETheme {
    PaymentScreen(remaining = 0, onClose = {})
}
