package com.dundueni.app.feature.familyshare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dundueni.app.feature.home.HomeBottomNavigation
import com.dundueni.app.feature.home.HomeDestination
import com.dundueni.app.ui.theme.BrandInk
import com.dundueni.app.ui.theme.DundueniFETheme
import com.dundueni.app.ui.theme.PageBackground

private const val FAMILY_SHARE_PREVIEW_GROUP = "가족 공유"
private const val PREVIEW_MONTHLY_INSPECTION_LIMIT = 200
private val PreviewMemberWithCaution = FamilyMember(name = "엄마", monthlyInspectionCount = 48)
private val PreviewSettings = FamilyShareSettings(
    syncInterval = FamilySyncInterval.REAL_TIME,
    maskSensitiveInfo = true,
    dangerAlerts = true,
    cautionAlerts = false,
    sosAlerts = true,
)

private val PreviewGroup = FamilyGroup(
    id = "preview-family",
    name = "우리 가족",
    members = listOf(
        FamilyMember(name = "나", monthlyInspectionCount = 62),
        PreviewMemberWithCaution,
        FamilyMember(name = "아빠", monthlyInspectionCount = 32),
    ),
    monthlyInspectionCount = 142,
    inviteCode = "DN-8842-FAM",
    passphraseQuestion = "우리 가족이 처음 함께 여행한 곳은?",
    settings = PreviewSettings,
)

@Preview(name = "미가입", group = FAMILY_SHARE_PREVIEW_GROUP, widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun FamilyShareNotJoinedPreview() = FamilySharePreview {
    FamilySharePlan(onViewPlans = {})
}

@Preview(name = "초대 코드 공유", group = FAMILY_SHARE_PREVIEW_GROUP, widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun FamilyShareCodeSharePreview() = FamilySharePreview {
    FamilyShareInvitations(
        group = PreviewGroup,
        submitting = false,
        onAction = {},
        onPaste = { null },
        onCopy = {},
        onShare = {},
        initialTab = InviteTab.SHARE,
    )
}

@Preview(name = "초대 코드 입력", group = FAMILY_SHARE_PREVIEW_GROUP, widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun FamilyShareCodeInputPreview() = FamilySharePreview {
    FamilyShareInvitations(
        group = null,
        submitting = false,
        onAction = {},
        onPaste = { PreviewGroup.inviteCode },
        onCopy = {},
        onShare = {},
        initialTab = InviteTab.JOIN,
    )
}

@Preview(name = "암구호 설정", group = FAMILY_SHARE_PREVIEW_GROUP, widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun FamilySharePassphraseSetupPreview() = FamilySharePreview(title = "가족 암구호") {
    FamilySharePassphrase(
        question = null,
        submitting = false,
        onSave = { _, _, _ -> },
        onCancel = {},
    )
}

@Preview(name = "암구호 수정", group = FAMILY_SHARE_PREVIEW_GROUP, widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun FamilySharePassphraseEditPreview() = FamilySharePreview(title = "가족 암구호") {
    FamilySharePassphrase(
        question = PreviewGroup.passphraseQuestion,
        submitting = false,
        onSave = { _, _, _ -> },
        onCancel = {},
    )
}

@Preview(
    name = "홈 · 3/4명 · 주의 알림 · 실시간 동기화",
    group = FAMILY_SHARE_PREVIEW_GROUP,
    widthDp = 390,
    heightDp = 1600,
    showBackground = true,
)
@Composable
private fun FamilyShareHomePreview() = FamilyShareDashboardPreview(
    initialGroup = PreviewGroup,
    memberWithCaution = PreviewMemberWithCaution,
)

@Preview(
    name = "홈 · 4/4명 · 초대 비활성화 · 하루 1회 동기화",
    group = FAMILY_SHARE_PREVIEW_GROUP,
    widthDp = 390,
    heightDp = 1600,
    showBackground = true,
)
@Composable
private fun FamilyShareFullHomePreview() = FamilyShareDashboardPreview(
    initialGroup = PreviewGroup.copy(
        members = PreviewGroup.members + FamilyMember(name = "동생", monthlyInspectionCount = 58),
        monthlyInspectionCount = PREVIEW_MONTHLY_INSPECTION_LIMIT,
        passphraseQuestion = null,
        settings = PreviewSettings.copy(
            syncInterval = FamilySyncInterval.DAILY,
            dangerAlerts = false,
            cautionAlerts = true,
            sosAlerts = false,
        ),
    ),
)

@Composable
private fun FamilyShareDashboardPreview(
    initialGroup: FamilyGroup,
    memberWithCaution: FamilyMember? = null,
) = FamilySharePreview {
    var settings by remember(initialGroup) { mutableStateOf(initialGroup.settings) }
    FamilyShareDashboard(
        group = initialGroup.copy(settings = settings),
        submitting = false,
        onInvite = {},
        onEditPassphrase = {},
        onUpdateSettings = { settings = it },
        monthlyInspectionLimit = PREVIEW_MONTHLY_INSPECTION_LIMIT,
        hasCautionAlert = { member -> member == memberWithCaution },
    )
}

@Composable
private fun FamilySharePreview(
    title: String = "가족 공유",
    content: @Composable () -> Unit,
) = DundueniFETheme {
    Scaffold(
        containerColor = PageBackground,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기", tint = BrandInk)
                }
                Text(title, color = BrandInk, style = MaterialTheme.typography.titleMedium)
            }
        },
        bottomBar = { HomeBottomNavigation(HomeDestination.FAMILY_SHARE, onNavigationSelected = {}) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
        }
    }
}
