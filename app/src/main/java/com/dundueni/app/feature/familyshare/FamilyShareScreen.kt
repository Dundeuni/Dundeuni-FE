package com.dundueni.app.feature.familyshare

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.dundueni.app.R
import com.dundueni.app.ui.component.DundueniButton
import com.dundueni.app.ui.component.DundueniButtonType
import com.dundueni.app.ui.component.dundueniOutlinedTextFieldColors
import com.dundueni.app.ui.theme.BrandInk
import com.dundueni.app.ui.theme.DisabledSurface
import com.dundueni.app.ui.theme.DundueniDefaultShape
import com.dundueni.app.ui.theme.ProtectiveBlue
import com.dundueni.app.ui.theme.SecondaryInk
import com.dundueni.app.ui.theme.SoftBlue
import com.dundueni.app.ui.theme.SurfaceLight

@Composable
internal fun FamilySharePlan(onViewPlans: () -> Unit) {
    FamilyCard(highlighted = true) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("패밀리 플랜", color = ProtectiveBlue, style = MaterialTheme.typography.labelLarge)
                Text("온 가족의 스마트폰을 든든하게", style = MaterialTheme.typography.headlineSmall)
            }
            Image(painterResource(R.drawable.dundeuni_mascot_love), null, Modifier.size(72.dp))
        }
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
    FamilyCard(highlighted = true) {
        FamilyTitle(group?.name ?: "가족 연결하기")
        FamilyDescription(
            if (group == null) "새 그룹을 만들거나 받은 초대 코드로 가족 그룹에 참여하세요."
            else "가족에게 초대 코드를 보내 그룹에 초대하세요.",
        )
    }
    FamilyChoices(InviteTab.entries, tab, InviteTab::label, onSelect = { tab = it })
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
                        Text(code, Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                    }
                    DundueniButton("초대 코드 공유하기", { onShare(code) }, Modifier.fillMaxWidth())
                    TextButton(onClick = { onCopy(code) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("초대 코드 복사")
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
) {
    FamilyCard(highlighted = true) {
        Text(group.name, color = ProtectiveBlue, style = MaterialTheme.typography.labelLarge)
        FamilyTitle("이달의 가족 검사")
        val count = group.monthlyInspectionCount
        if (count == null) {
            FamilyDescription("가족 검사 횟수를 확인할 수 없어요.")
        } else {
            Text("${count}회", style = MaterialTheme.typography.headlineSmall)
        }
    }
    FamilyCard {
        FamilyTitle("연결된 구성원 ${group.members.size}명")
        if (group.members.isEmpty()) {
            FamilyDescription("연결된 구성원이 없어요. 초대 코드를 보내 가족을 연결해 보세요.")
        }
        group.members.forEach { member ->
            HorizontalDivider(color = DisabledSurface)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(member.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                FamilyDescription(member.monthlyInspectionCount?.let { "이달 검사 ${it}회" } ?: "검사 횟수 미확인")
            }
        }
        DundueniButton(
            text = "구성원 초대",
            onClick = onInvite,
            modifier = Modifier.fillMaxWidth(),
            type = DundueniButtonType.SECONDARY,
        )
    }
    val settings = group.settings
    FamilyCard {
        FamilyTitle("검사 기록 동기화")
        if (settings == null) {
            FamilyDescription("동기화 설정을 확인할 수 없어요.")
        } else {
            FamilyDescription("가족과 검사 기록을 공유할 주기를 선택하세요.")
            FamilyChoices(
                choices = FamilySyncInterval.entries,
                selected = settings.syncInterval,
                label = FamilySyncInterval::label,
                enabled = !submitting,
                onSelect = { onUpdateSettings(settings.copy(syncInterval = it)) },
            )
            FamilySwitch("개인정보를 가리고 공유", settings.maskSensitiveInfo, !submitting) {
                onUpdateSettings(settings.copy(maskSensitiveInfo = it))
            }
        }
    }
    FamilyCard {
        FamilyTitle("우리 가족 암구호")
        val question = group.passphraseQuestion
        FamilyDescription(question ?: "가족끼리 확인할 질문과 답을 정해 보세요.")
        TextButton(onClick = onEditPassphrase, modifier = Modifier.align(Alignment.End)) {
            Text(if (question == null) "암구호 설정" else "암구호 수정")
        }
    }
    FamilyCard {
        FamilyTitle("가족 위험 알림")
        if (settings == null) {
            FamilyDescription("위험 알림 설정을 확인할 수 없어요.")
        } else {
            FamilySwitch("위험 결과 즉시 알림", settings.dangerAlerts, !submitting) {
                onUpdateSettings(settings.copy(dangerAlerts = it))
            }
            FamilySwitch("주의 결과 알림", settings.cautionAlerts, !submitting) {
                onUpdateSettings(settings.copy(cautionAlerts = it))
            }
            FamilySwitch("가족 도움 요청 알림", settings.sosAlerts, !submitting) {
                onUpdateSettings(settings.copy(sosAlerts = it))
            }
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
        FamilyTitle(if (question == null) "가족 암구호 설정" else "가족 암구호 수정")
        FamilyDescription("가족만 답을 알 수 있는 질문을 정해 주세요.")
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
        FamilyDescription("긴 문장보다 가족이 바로 떠올릴 수 있는 짧은 답이 좋아요.")
    }
    FamilyCard(highlighted = true) {
        FamilySwitch("가족에게 암구호 변경 알림", notifyFamily, !submitting) { notifyFamily = it }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> FamilyChoices(
    choices: List<T>,
    selected: T,
    label: (T) -> String,
    enabled: Boolean = true,
    onSelect: (T) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { choice ->
            FilterChip(
                selected = selected == choice,
                onClick = { if (selected != choice) onSelect(choice) },
                enabled = enabled,
                label = { Text(label(choice)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = SurfaceLight,
                    selectedContainerColor = SoftBlue,
                    selectedLabelColor = ProtectiveBlue,
                    disabledSelectedContainerColor = SoftBlue,
                    disabledLabelColor = SecondaryInk,
                ),
            )
        }
    }
}

@Composable
private fun FamilySwitch(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.semantics { contentDescription = label },
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = ProtectiveBlue),
        )
    }
}
