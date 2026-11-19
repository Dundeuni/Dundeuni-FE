package com.dundueni.app.feature.phone

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dundueni.app.ui.component.InspectionErrorDialog
import com.dundueni.app.ui.component.InspectionLoadingDialog
import com.dundueni.app.ui.theme.DundueniFETheme

@Composable
internal fun PhoneInspectionPreview(loading: Boolean = false, errorMessage: String? = null) {
    DundueniFETheme {
        PhoneInputScreen(onInspect = {}, onClose = {}, isInspecting = loading)
        if (loading) InspectionLoadingDialog(onDismissPopup = {})
        errorMessage?.let { message ->
            InspectionErrorDialog(
                message = message, onRetry = {}, onCancelInspection = {}, showRetry = true,
            )
        }
    }
}

@Preview(name = "전화번호 검사 · 기본 입력", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun PhoneInspectionInputPreview() = PhoneInspectionPreview()
