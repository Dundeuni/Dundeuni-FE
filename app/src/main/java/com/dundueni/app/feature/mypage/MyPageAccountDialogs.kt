package com.dundueni.app.feature.mypage

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dundueni.app.R
import com.dundueni.app.feature.analysisresult.ResultGlyph
import com.dundueni.app.feature.analysisresult.ResultIcon
import com.dundueni.app.ui.component.DundueniButton
import com.dundueni.app.ui.component.DundueniButtonType
import com.dundueni.app.ui.theme.BrandInk
import com.dundueni.app.ui.theme.CanvasSubLayer
import com.dundueni.app.ui.theme.Danger
import com.dundueni.app.ui.theme.DisabledSurface
import com.dundueni.app.ui.theme.DundueniDefaultShape
import com.dundueni.app.ui.theme.ProtectiveBlue
import com.dundueni.app.ui.theme.SecondaryInk
import com.dundueni.app.ui.theme.SoftBlue

private enum class AccountAction { LOGOUT, WITHDRAWAL }

@Composable
internal fun MyPageAccountActions(
    onLogoutConfirmed: () -> Unit,
    onWithdrawalConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingAction by rememberSaveable { mutableStateOf<AccountAction?>(null) }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { pendingAction = AccountAction.LOGOUT }) {
            Text("로그아웃", color = SecondaryInk)
        }
        VerticalDivider(Modifier.height(12.dp), color = DisabledSurface)
        TextButton(onClick = { pendingAction = AccountAction.WITHDRAWAL }) {
            Text("회원탈퇴", color = SecondaryInk)
        }
    }

    val action = pendingAction ?: return
    val dismiss = { pendingAction = null }
    val confirm: () -> Unit = {
        if (pendingAction == action) {
            dismiss()
            when (action) {
                AccountAction.LOGOUT -> onLogoutConfirmed()
                AccountAction.WITHDRAWAL -> onWithdrawalConfirmed()
            }
        }
    }
    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        when (action) {
            AccountAction.LOGOUT -> LogoutConfirmationContent(onCancel = dismiss, onConfirm = confirm)
            AccountAction.WITHDRAWAL -> WithdrawalConfirmationContent(onCancel = dismiss, onConfirm = confirm)
        }
    }
}

@Composable
internal fun LogoutConfirmationContent(onCancel: () -> Unit, onConfirm: () -> Unit) {
    AccountConfirmationCard("정말 로그아웃 하시겠어요?") {
        ConfirmationActions(
            cancelLabel = "취소",
            confirmLabel = "로그아웃",
            onCancel = onCancel,
            onConfirm = onConfirm,
        )
    }
}

@Composable
internal fun WithdrawalConfirmationContent(onCancel: () -> Unit, onConfirm: () -> Unit) {
    var acknowledged by rememberSaveable { mutableStateOf(false) }
    AccountConfirmationCard("정말 든든이를 탈퇴하시겠어요?") {
        Text(
            "탈퇴하시면 그동안 보이스피싱과 스미싱으로부터 고객님을 지켜주던 안심 보호 혜택과 검사 기록이 모두 소멸됩니다.",
            color = SecondaryInk,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
        )
        Column(
            Modifier.fillMaxWidth().background(SoftBlue, MaterialTheme.shapes.large).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ResultGlyph(ResultIcon.WARNING, Danger, 14.dp)
                Text(
                    "탈퇴 시 즉시 중단되는 혜택",
                    color = Danger,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            WithdrawalWarning("실시간 의심 통화 및 악성 URL 차단 알림 중단")
            WithdrawalWarning("안심 금고에 백업된 위험 검사 기록 영구 파기")
            WithdrawalWarning("위급 상황 시 연동된 보호자 SOS 연락망 해제")
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .clip(DundueniDefaultShape).background(CanvasSubLayer)
                .toggleable(value = acknowledged, role = Role.Checkbox, onValueChange = { acknowledged = it })
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(20.dp)
                    .background(if (acknowledged) ProtectiveBlue else Color.White, RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (acknowledged) ResultGlyph(ResultIcon.CHECK, Color.White, 14.dp)
            }
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = ProtectiveBlue, fontWeight = FontWeight.Bold)) {
                        append("[필수] ")
                    }
                    append("안내 사항을 모두 확인하였으며, 소멸된 ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("데이터 복구가 불가능함에 동의합니다.")
                    }
                },
                modifier = Modifier.weight(1f),
                color = BrandInk,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        ConfirmationActions(
            cancelLabel = "든든이 계속 쓰고 보호받기",
            confirmLabel = "탈퇴 진행하기",
            onCancel = onCancel,
            onConfirm = onConfirm,
            confirmEnabled = acknowledged,
            cancelIcon = { ResultGlyph(ResultIcon.SHIELD_OUTLINE, Color.White, 14.dp) },
        )
    }
}

@Composable
private fun AccountConfirmationCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState())
                .padding(start = 22.dp, top = 20.dp, end = 22.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.dundeuni_mascot_nervous),
                contentDescription = null,
                modifier = Modifier.size(72.dp).background(SoftBlue, CircleShape),
            )
            Text(
                title,
                color = BrandInk,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            content()
        }
    }
}

@Composable
private fun WithdrawalWarning(message: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(11.dp).border(1.dp, Danger, CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(width = 5.dp, height = 1.dp).background(Danger))
        }
        Text(message, color = BrandInk, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ConfirmationActions(
    cancelLabel: String,
    confirmLabel: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    cancelIcon: (@Composable () -> Unit)? = null,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DundueniButton(
            text = cancelLabel,
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            type = DundueniButtonType.SECONDARY,
            icon = cancelIcon,
        )
        TextButton(
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier.heightIn(min = 48.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = Danger,
                disabledContentColor = Color(0xFFB0B5BF),
            ),
        ) {
            Text(confirmLabel, style = MaterialTheme.typography.bodySmall)
        }
    }
}
