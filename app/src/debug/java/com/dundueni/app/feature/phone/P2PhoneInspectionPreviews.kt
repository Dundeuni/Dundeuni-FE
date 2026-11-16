package com.dundueni.app.feature.phone

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dundueni.app.ui.component.InspectionErrorDialog
import com.dundueni.app.ui.component.InspectionLoadingDialog
import com.dundueni.app.ui.theme.DundueniFETheme

@Composable
private fun PhoneInspectionPreview(loading: Boolean = false, errorMessage: String? = null) {
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

// POST inspection and the subsequent detail GET both use PhoneInspectionUiState.Loading.
@Preview(name = "P2 전화번호 검사 · 요청 및 상세 조회 중", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun PhoneInspectionLoadingPreview() = PhoneInspectionPreview(loading = true)

@Preview(name = "P2 전화번호 검사 · 일반 실패", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun PhoneInspectionErrorPreview() = PhoneInspectionPreview(
    errorMessage = "검사를 완료하지 못했어요. 다시 시도해 주세요.",
)

@Preview(name = "P2 전화번호 검사 · 네트워크 오류", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun PhoneInspectionNetworkErrorPreview() = PhoneInspectionPreview(
    errorMessage = "네트워크 연결을 확인한 뒤 다시 시도해 주세요.",
)

@Preview(name = "P2 전화번호 검사 · HTTP 오류", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun PhoneInspectionHttpErrorPreview() = PhoneInspectionPreview(
    errorMessage = "검사를 완료하지 못했어요 (HTTP 429).",
)

@Preview(name = "P2 전화번호 검사 · 상세 조회 실패", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun PhoneInspectionDetailErrorPreview() = PhoneInspectionPreview(
    errorMessage = "검사 결과를 조회하지 못했어요. 다시 시도해 주세요.",
)
