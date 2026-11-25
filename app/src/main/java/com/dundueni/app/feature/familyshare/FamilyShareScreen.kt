package com.dundueni.app.feature.familyshare

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dundueni.app.R
import com.dundueni.app.ui.component.DundueniButton
import com.dundueni.app.ui.component.DundueniButtonType
import com.dundueni.app.ui.component.dundueniOutlinedTextFieldColors
import com.dundueni.app.ui.theme.BrandInk
import com.dundueni.app.ui.theme.CanvasSubLayer
import com.dundueni.app.ui.theme.CautionInk
import com.dundueni.app.ui.theme.Danger
import com.dundueni.app.ui.theme.DundueniDefaultShape
import com.dundueni.app.ui.theme.DundueniPillShape
import com.dundueni.app.ui.theme.ProtectiveBlue
import com.dundueni.app.ui.theme.SafeInk
import com.dundueni.app.ui.theme.SecondaryInk
import com.dundueni.app.ui.theme.SoftBlue
import com.dundueni.app.ui.theme.SoftCaution
import com.dundueni.app.ui.theme.SoftDanger
import com.dundueni.app.ui.theme.SoftSafe
import com.dundueni.app.ui.theme.SurfaceLight
import kotlin.math.roundToInt

private const val MaxFamilyMembers = 4

@Composable
internal fun FamilySharePlan(onViewPlans: () -> Unit) {
    FamilyCard(highlighted = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            FamilyBadge("패밀리 플랜", CanvasSubLayer, ProtectiveBlue)
            Image(painterResource(R.drawable.dundeuni_mascot_love), null, Modifier.size(56.dp))
        }
        Text(
            "온 가족의 스마트폰을\n든든하게",
            style = MaterialTheme.typography.headlineSmall.copy(lineHeight = 30.sp),
        )
        FamilyDescription("가족 공유는 든든이 패밀리 플랜에서 이용할 수 있어요.")
        DundueniButton(
            text = "패밀리 요금제 보기",
            onClick = onViewPlans,
            modifier = Modifier.fillMaxWidth(),
            type = DundueniButtonType.SECONDARY,
        )
    }
    FamilyCard {
        FamilyTitle("가족과 함께 이용할 수 있어요")
        FamilyBenefit("최대 4명 가족 연결", "초대 코드로 가족을 연결하고 검사 현황을 확인해요.")
        FamilyBenefit("위험 알림", "위험한 검사 결과를 가족에게 알릴 수 있어요.")
        FamilyBenefit("가족 암구호", "가족끼리 정한 질문과 답으로 사칭 전화에 대비해요.")
        FamilyBenefit("검사 기록 동기화", "가족의 검사 기록을 원하는 주기로 받아봐요.")
    }
}

internal enum class InviteTab(val label: String) {
    SHARE("초대 코드 공유"),
    JOIN("초대 코드 입력"),
}

@Composable
internal fun FamilyShareInvitations(
    group: FamilyGroup?,
    submitting: Boolean,
    onAction: (FamilyShareAction) -> Unit,
    onPaste: () -> String?,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    initialTab: InviteTab = InviteTab.SHARE,
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var input by remember { mutableStateOf("") }
    FamilyCard {
        FamilySectionHeading(
            icon = Icons.Filled.Person,
            title = group?.name ?: "가족 연결하기",
            description = if (group == null) "새 그룹을 만들거나 받은 초대 코드로 가족 그룹에 참여하세요."
            else "가족에게 초대 코드를 보내 그룹에 초대하세요.",
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(CanvasSubLayer, DundueniDefaultShape)
            .selectableGroup()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        InviteTab.entries.forEach { choice ->
            val selected = tab == choice
            FamilyChoice(
                label = choice.label,
                selected = selected,
                enabled = true,
                role = Role.Tab,
                background = if (selected) SurfaceLight else Color.Transparent,
                foreground = if (selected) BrandInk else SecondaryInk,
                modifier = Modifier.weight(1f),
                onClick = { tab = choice },
            )
        }
    }
    FamilyCard {
        when (tab) {
            InviteTab.SHARE -> {
                val code = group?.inviteCode?.takeIf { it.isNotBlank() }
                FamilyTitle("우리 가족 초대 코드")
                if (code == null) {
                    FamilyDescription(
                        if (group == null) "가족 그룹을 만들면 가족을 초대할 수 있어요."
                        else "발급된 초대 코드가 없어요.",
                    )
                    DundueniButton(
                        text = if (group == null) "가족 그룹 만들기" else "초대 코드 발급",
                        onClick = {
                            onAction(
                                if (group == null) FamilyShareAction.CreateGroup else FamilyShareAction.IssueInviteCode,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !submitting,
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SoftBlue,
                        shape = DundueniDefaultShape,
                    ) {
                        Text(
                            code,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 20.dp),
                            style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 2.sp),
                            textAlign = TextAlign.Center,
                        )
                    }
                    DundueniButton(
                        text = "초대 코드 공유하기",
                        onClick = { onShare(code) },
                        modifier = Modifier.fillMaxWidth(),
                        type = DundueniButtonType.SECONDARY,
                    )
                    FilledTonalButton(
                        onClick = { onCopy(code) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = DundueniPillShape,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CanvasSubLayer,
                            contentColor = BrandInk,
                        ),
                    ) {
                        Text("초대 코드 복사", style = MaterialTheme.typography.labelLarge)
                    }
                }
                FamilyDescription("초대받은 가족은 든든이 앱의 가족 공유에서 코드를 입력할 수 있어요.")
            }
            InviteTab.JOIN -> {
                FamilyTitle("받은 초대 코드")
                if (group != null) {
                    FamilyDescription("이미 가족 그룹에 연결되어 있어요.")
                } else {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("초대 코드") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        enabled = !submitting,
                        shape = DundueniDefaultShape,
                        colors = dundueniOutlinedTextFieldColors(),
                    )
                    TextButton(
                        onClick = { onPaste()?.let { input = it } },
                        modifier = Modifier.align(Alignment.End),
                        enabled = !submitting,
                    ) {
                        Text("클립보드에서 붙여넣기")
                    }
                    DundueniButton(
                        text = "가족 그룹 참여하기",
                        onClick = { onAction(FamilyShareAction.JoinGroup(input.trim())) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = input.isNotBlank() && !submitting,
                    )
                }
            }
        }
    }
}

@Composable
internal fun FamilyShareDashboard(
    group: FamilyGroup,
    submitting: Boolean,
    onInvite: () -> Unit,
    onEditPassphrase: () -> Unit,
    onUpdateSettings: (FamilyShareSettings) -> Unit,
    monthlyInspectionLimit: Int? = null,
    hasCautionAlert: (FamilyMember) -> Boolean = { false },
) {
    FamilyInspectionCard(group, monthlyInspectionLimit)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val memberCount = group.members.size
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("연결된 구성원", style = MaterialTheme.typography.titleMedium, color = BrandInk)
            FamilyBadge("$memberCount/$MaxFamilyMembers", CanvasSubLayer, ProtectiveBlue)
        }
        if (group.members.isEmpty()) {
            FamilyCard {
                FamilyDescription("연결된 구성원이 없어요. 초대 코드를 보내 가족을 연결해 보세요.")
            }
        }
        group.members.forEach { member ->
            FamilyMemberCard(member, hasCautionAlert(member))
        }
        DundueniButton(
            text = "구성원 초대",
            onClick = onInvite,
            modifier = Modifier.fillMaxWidth(),
            type = DundueniButtonType.SECONDARY,
            enabled = memberCount < MaxFamilyMembers,
            icon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
    }
    val settings = group.settings
    FamilyCard {
        FamilySectionHeading(
            icon = Icons.Filled.Refresh,
            title = "검사 기록 동기화",
            description = if (settings == null) "동기화 설정을 확인할 수 없어요."
            else "가족과 검사 기록을 공유할 주기를 선택하세요.",
        )
        if (settings != null) {
            Row(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FamilySyncInterval.entries.forEach { interval ->
                    val selected = settings.syncInterval == interval
                    FamilyChoice(
                        label = interval.label,
                        selected = selected,
                        enabled = !submitting,
                        role = Role.RadioButton,
                        background = if (selected) ProtectiveBlue else SoftBlue,
                        foreground = if (selected) SurfaceLight else SecondaryInk,
                        modifier = Modifier.weight(1f),
                        onClick = { if (!selected) onUpdateSettings(settings.copy(syncInterval = interval)) },
                    )
                }
            }
            HorizontalDivider(color = CanvasSubLayer)
            FamilySwitch("개인정보를 가리고 공유", settings.maskSensitiveInfo, !submitting) {
                onUpdateSettings(settings.copy(maskSensitiveInfo = it))
            }
        }
    }
    FamilyCard {
        val question = group.passphraseQuestion
        FamilySectionHeading(
            icon = Icons.Filled.Lock,
            title = "우리 가족 세이프 암구호",
            description = "사칭이 의심되는 통화에서 가족만 아는\n질문과 답으로 서로를 확인해요.",
        )
        if (question != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = DundueniDefaultShape,
                color = SoftBlue,
                contentColor = BrandInk,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("확인 질문", color = ProtectiveBlue, style = MaterialTheme.typography.labelLarge)
                    Text(question, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        FilledTonalButton(
            onClick = onEditPassphrase,
            modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp),
            shape = DundueniPillShape,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = SoftBlue,
                contentColor = ProtectiveBlue,
            ),
        ) {
            Text(if (question == null) "암구호 설정" else "암구호 수정")
        }
    }
    FamilyCard {
        FamilySectionHeading(
            icon = Icons.Filled.Notifications,
            title = "가족 위험 알림",
            iconBackground = SoftDanger,
            iconTint = Danger,
        )
        if (settings == null) {
            FamilyDescription("위험 알림 설정을 확인할 수 없어요.")
        } else {
            FamilySwitch(
                label = "위험 결과 즉시 알림",
                description = "가족 구성원의 검사에서 위험 결과가 확인되면\n알려드려요.",
                checked = settings.dangerAlerts,
                enabled = !submitting,
            ) {
                onUpdateSettings(settings.copy(dangerAlerts = it))
            }
            HorizontalDivider(color = CanvasSubLayer)
            FamilySwitch(
                label = "주의 결과 즉시 알림",
                description = "가족 구성원의 검사에서 주의가 필요한 결과가\n나오면 알려드려요.",
                checked = settings.cautionAlerts,
                enabled = !submitting,
            ) {
                onUpdateSettings(settings.copy(cautionAlerts = it))
            }
            HorizontalDivider(color = CanvasSubLayer)
            FamilySwitch(
                label = "가족 도움 요청 알림",
                description = "가족 구성원이 도움을 요청하면 알려드려요.",
                checked = settings.sosAlerts,
                enabled = !submitting,
            ) {
                onUpdateSettings(settings.copy(sosAlerts = it))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FamilyInspectionCard(group: FamilyGroup, monthlyInspectionLimit: Int?) {
    val used = group.monthlyInspectionCount?.takeIf { it >= 0 }
    val total = monthlyInspectionLimit?.takeIf { it > 0 }

    FamilyCard {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FamilyBadge("패밀리 플랜 이용 중", SoftBlue, ProtectiveBlue, Icons.Filled.Check)
            FamilyBadge("매월 1일 갱신", CanvasSubLayer, SecondaryInk)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(group.name, color = SecondaryInk, style = MaterialTheme.typography.labelMedium)
            FamilyTitle("이달의 가족 검사")
        }
        if (used != null && total != null) {
            val remaining = (total - used).coerceAtLeast(0)
            val progress = remaining.toFloat() / total
            val remainingStyle = MaterialTheme.typography.headlineSmall.toSpanStyle().copy(color = BrandInk)
            Text(
                text = buildAnnotatedString {
                    append("남은 검사 ")
                    withStyle(remainingStyle) { append("${remaining}회") }
                    append(" / 총 ${total}회 제공")
                },
                color = SecondaryInk,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 32.sp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f).height(8.dp),
                    color = ProtectiveBlue,
                    trackColor = CanvasSubLayer,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                Text(
                    "${(progress * 100).roundToInt()}% 잔여",
                    color = ProtectiveBlue,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        } else {
            FamilyDescription(
                when {
                    used != null -> "이달 사용 ${used}회 · 총 제공 횟수 미확인"
                    total != null -> "총 ${total}회 제공 · 남은 횟수 미확인"
                    else -> "가족 검사 횟수를 확인할 수 없어요."
                },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(SoftBlue, DundueniDefaultShape)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.dundeuni_mascot_default), null, Modifier.size(24.dp))
            Text(
                "가족 구성원 누구나 화면 속 든든이 플로팅 버튼을\n탭하여 실시간 검사를 자유롭게 공유할 수 있습니다",
                color = SecondaryInk,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
            )
        }
    }
}

@Composable
private fun FamilyMemberCard(member: FamilyMember, hasCautionAlert: Boolean) {
    val statusTint = if (hasCautionAlert) Danger else SafeInk
    FamilyCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FamilyIcon(
                image = Icons.Filled.Person,
                background = if (hasCautionAlert) SoftDanger else CanvasSubLayer,
                foreground = if (hasCautionAlert) Danger else BrandInk,
                modifier = Modifier.size(40.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(member.name, style = MaterialTheme.typography.labelLarge)
                Text(
                    member.monthlyInspectionCount?.let { "이달 검사 ${it}회" } ?: "검사 횟수 미확인",
                    color = SecondaryInk,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            FamilyBadge(
                text = if (hasCautionAlert) "주의 알림" else "보호 중",
                background = if (hasCautionAlert) SoftDanger else SoftSafe,
                foreground = statusTint,
                icon = if (hasCautionAlert) Icons.Filled.Warning else Icons.Filled.Check,
            )
        }
    }
}

@Composable
internal fun FamilySharePassphrase(
    question: String?,
    submitting: Boolean,
    onSave: (question: String, answer: String, notifyFamily: Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    var questionInput by remember(question) { mutableStateOf(question.orEmpty()) }
    var answer by remember { mutableStateOf("") }
    var showAnswer by remember { mutableStateOf(false) }
    var notifyFamily by rememberSaveable { mutableStateOf(false) }
    FamilyCard {
        FamilySectionHeading(
            icon = Icons.Filled.Lock,
            title = if (question == null) "세이프 암구호 설정" else "세이프 암구호 수정",
            description = "가족만 답을 알 수 있는 질문을 정해 주세요.",
        )
        OutlinedTextField(
            value = questionInput,
            onValueChange = { questionInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("확인 질문") },
            maxLines = 2,
            enabled = !submitting,
            shape = DundueniDefaultShape,
            colors = dundueniOutlinedTextFieldColors(),
        )
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("암구호 정답") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !submitting,
            visualTransformation = if (showAnswer) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { showAnswer = !showAnswer }) {
                    Text(if (showAnswer) "숨기기" else "보기")
                }
            },
            shape = DundueniDefaultShape,
            colors = dundueniOutlinedTextFieldColors(),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = DundueniDefaultShape,
            color = SoftCaution,
            contentColor = CautionInk,
        ) {
            Text(
                "긴 문장보다 가족이 바로 떠올릴 수 있는 짧은 답이 좋아요.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    FamilyCard {
        FamilySwitch(
            label = "가족에게 암구호 변경 알림",
            description = "암구호를 저장할 때 변경 사실을 가족에게 알려요.",
            checked = notifyFamily,
            enabled = !submitting,
        ) {
            notifyFamily = it
        }
    }
    DundueniButton(
        text = "암구호 저장",
        onClick = { onSave(questionInput.trim(), answer.trim(), notifyFamily) },
        modifier = Modifier.fillMaxWidth(),
        type = DundueniButtonType.SECONDARY,
        enabled = questionInput.isNotBlank() && answer.isNotBlank() && !submitting,
    )
    TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text("취소하고 돌아가기")
    }
}

@Composable
private fun FamilyCard(highlighted: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (highlighted) SoftBlue else SurfaceLight,
        contentColor = BrandInk,
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun FamilyTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun FamilyDescription(text: String) {
    Text(text, color = SecondaryInk, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun FamilyBenefit(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        FamilyDescription(description)
    }
}

@Composable
private fun FamilySectionHeading(
    icon: ImageVector,
    title: String,
    description: String? = null,
    iconBackground: Color = CanvasSubLayer,
    iconTint: Color = ProtectiveBlue,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = if (description == null) Alignment.CenterVertically else Alignment.Top,
    ) {
        FamilyIcon(icon, iconBackground, iconTint)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FamilyTitle(title)
            if (description != null) FamilyDescription(description)
        }
    }
}

@Composable
private fun FamilyIcon(
    image: ImageVector,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier.size(36.dp),
) {
    Box(
        modifier = modifier.background(background, DundueniDefaultShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(image, contentDescription = null, modifier = Modifier.size(20.dp), tint = foreground)
    }
}

@Composable
private fun FamilyBadge(text: String, background: Color, foreground: Color, icon: ImageVector? = null) {
    Row(
        modifier = Modifier.background(background, DundueniPillShape).padding(horizontal = 9.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = foreground)
        Text(text, color = foreground, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun FamilyChoice(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    role: Role,
    background: Color,
    foreground: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier.heightIn(min = 48.dp)
            .clip(DundueniDefaultShape)
            .background(background)
            .selectable(selected = selected, enabled = enabled, role = role, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = foreground,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FamilySwitch(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    description: String? = null,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            if (description != null) {
                Text(description, color = SecondaryInk, style = MaterialTheme.typography.bodySmall)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.semantics { contentDescription = label },
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = ProtectiveBlue),
        )
    }
}
