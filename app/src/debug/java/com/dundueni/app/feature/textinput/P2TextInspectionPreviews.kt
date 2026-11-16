package com.dundueni.app.feature.textinput

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dundueni.app.ui.component.InspectionErrorDialog
import com.dundueni.app.ui.component.InspectionLoadingDialog
import com.dundueni.app.ui.theme.DundueniFETheme

@Composable
private fun TextInspectionPreview(loading: Boolean = false, errorMessage: String? = null) {
    DundueniFETheme {
        TextInputScreen(
            textInput = "급하게 송금해 주세요.",
            onTextChange = {}, onInspect = {}, onClose = {},
            isInspecting = loading,
        )
        if (loading) InspectionLoadingDialog(onDismissPopup = {})
        errorMessage?.let { message ->
            InspectionErrorDialog(
                message = message, onRetry = {}, onCancelInspection = {},
            )
        }
    }
}

@Preview(name = "P2 텍스트 검사 · 요청 중", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun TextInspectionLoadingPreview() = TextInspectionPreview(loading = true)

@Preview(name = "P2 텍스트 검사 · 실패 후 재시도", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun TextInspectionErrorPreview() = TextInspectionPreview(
    errorMessage = "텍스트 검사를 완료하지 못했어요. 다시 시도하면 같은 텍스트로 검사합니다.",
)

@Preview(name = "P2 텍스트 검사 · 결과 재조회", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun TextInspectionLookupErrorPreview() = TextInspectionPreview(
    errorMessage = "결과를 조회하지 못했어요. 다시 시도하면 같은 분석 ID로 확인합니다.",
)
