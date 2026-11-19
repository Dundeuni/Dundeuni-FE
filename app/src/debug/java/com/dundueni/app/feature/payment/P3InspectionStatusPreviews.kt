package com.dundueni.app.feature.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.dundueni.app.ui.component.InspectionCompletedContent
import com.dundueni.app.ui.component.InspectionCompletedDimAmount
import com.dundueni.app.ui.component.InspectionCompletedWindowMargin
import com.dundueni.app.ui.component.InspectionLimitDialog
import com.dundueni.app.ui.theme.DundueniFETheme
import com.dundueni.app.ui.theme.LocalEasyMode

// Image, screen, and text inspections use this same completion content.
// This previews the Compose card only; WindowManager and the service are not started.
@Preview(name = "P3 검사 완료 · 결과 확인 팝업", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun InspectionCompletedPreview() = DundueniFETheme {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = InspectionCompletedDimAmount))
            .padding(InspectionCompletedWindowMargin),
        contentAlignment = Alignment.Center,
    ) {
        InspectionCompletedContent(onViewResult = {}, onClose = {})
    }
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

@Preview(name = "요금제 · 기본 모드 · 연간", widthDp = 320, heightDp = 1500, showBackground = true)
@Composable
private fun PaymentDefaultModePreview() = PaymentModePreview(easyMode = false)

@Preview(name = "요금제 · 쉬운 모드 · 연간", widthDp = 320, heightDp = 1850, showBackground = true)
@Composable
private fun PaymentEasyModePreview() = PaymentModePreview(easyMode = true)

@Composable
private fun PaymentModePreview(easyMode: Boolean) = DundueniFETheme {
    CompositionLocalProvider(LocalEasyMode provides easyMode) {
        PaymentScreen(remaining = 3, onClose = {}, initialAnnual = true)
    }
}
