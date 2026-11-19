package com.dundueni.app.feature.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dundueni.app.feature.payment.BasicInspectionQuota
import com.dundueni.app.ui.theme.DundueniFETheme
import java.time.LocalDate

private val PreviewDate = LocalDate.of(2026, 4, 18)

@Preview(name = "마이페이지 · 로그인", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun MyPageLoggedInPreview() = DundueniFETheme {
    MyPageScreen(
        signedIn = true,
        nickname = "든든한지킴이",
        email = "guardian@example.com",
        firstDate = PreviewDate.withDayOfMonth(1),
        today = PreviewDate,
        inspections = 12,
        familyCount = 1,
        quotaUsage = BasicInspectionQuota.Usage(remaining = 3, usedInPeriod = 2),
        onBack = {},
        onLogin = {},
        onLogoutConfirmed = {},
        onWithdrawalConfirmed = {},
        onUpgrade = {},
        onChangeMode = {},
        onNavigationSelected = {},
    )
}

@Preview(name = "마이페이지 · 비로그인", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun MyPageLoggedOutPreview() = DundueniFETheme {
    MyPageScreen(
        signedIn = false,
        nickname = null,
        email = null,
        firstDate = PreviewDate.withDayOfMonth(1),
        today = PreviewDate,
        inspections = null,
        familyCount = null,
        quotaUsage = BasicInspectionQuota.Usage(remaining = BasicInspectionQuota.LIMIT, usedInPeriod = 0),
        onBack = {},
        onLogin = {},
        onLogoutConfirmed = {},
        onWithdrawalConfirmed = {},
        onUpgrade = {},
        onChangeMode = {},
        onNavigationSelected = {},
    )
}

@Preview(name = "마이페이지 · 회원탈퇴 확인", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun MyPageWithdrawalConfirmationPreview() = AccountDialogPreview {
    WithdrawalConfirmationContent(onCancel = {}, onConfirm = {})
}

@Preview(name = "마이페이지 · 로그아웃 확인", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun MyPageLogoutConfirmationPreview() = AccountDialogPreview {
    LogoutConfirmationContent(onCancel = {}, onConfirm = {})
}

@Composable
private fun AccountDialogPreview(content: @Composable () -> Unit) = DundueniFETheme {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0x73000000)).padding(horizontal = 34.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
