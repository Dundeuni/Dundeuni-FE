package com.dundueni.app.feature.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dundueni.app.feature.analysisresult.ResultGlyph
import com.dundueni.app.feature.analysisresult.ResultIcon
import com.dundueni.app.feature.home.HomeBottomNavigation
import com.dundueni.app.feature.home.HomeDestination
import com.dundueni.app.feature.payment.BasicInspectionQuota
import com.dundueni.app.ui.component.DundueniButton
import com.dundueni.app.ui.component.DundueniButtonType
import com.dundueni.app.ui.theme.BrandInk
import com.dundueni.app.ui.theme.DundueniDefaultShape
import com.dundueni.app.ui.theme.DundueniPillShape
import com.dundueni.app.ui.theme.LocalEasyMode
import com.dundueni.app.ui.theme.PageBackground
import com.dundueni.app.ui.theme.PrimaryMidnight
import com.dundueni.app.ui.theme.ProtectiveBlue
import com.dundueni.app.ui.theme.SecondaryInk
import com.dundueni.app.ui.theme.SoftBlue
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val BASIC_PLAN = "든든이 베이직"
private val DateFormat = DateTimeFormatter.ofPattern("MM.dd")

@Composable
internal fun MyPageScreen(
    signedIn: Boolean,
    nickname: String?,
    email: String?,
    firstDate: LocalDate,
    today: LocalDate,
    inspections: Int?,
    familyCount: Int?,
    quotaUsage: BasicInspectionQuota.Usage?,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onLogoutConfirmed: () -> Unit,
    onWithdrawalConfirmed: () -> Unit,
    onUpgrade: () -> Unit,
    onChangeMode: (Boolean) -> Unit,
    onNavigationSelected: (HomeDestination) -> Unit,
) {
    val easy = LocalEasyMode.current
    Scaffold(
        containerColor = PageBackground,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "홈으로 돌아가기", tint = BrandInk)
                }
                Text("마이페이지", Modifier.weight(1f), color = BrandInk, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { onNavigationSelected(HomeDestination.SETTINGS) }) {
                    Icon(Icons.Filled.Settings, contentDescription = "설정", tint = BrandInk)
                }
            }
        },
        bottomBar = {
            HomeBottomNavigation(if (easy) null else HomeDestination.MY_SHELTER, onNavigationSelected)
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileCard(signedIn, nickname, email, onLogin)
            MonthlyInspectionsCard("${firstDate.format(DateFormat)} ~ ${today.format(DateFormat)}", inspections, familyCount)
            PlanCard(quotaUsage, onUpgrade)
            ModeCard(easy, onChangeMode)
            if (signedIn) {
                MyPageAccountActions(
                    onLogoutConfirmed = onLogoutConfirmed,
                    onWithdrawalConfirmed = onWithdrawalConfirmed,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(signedIn: Boolean, nickname: String?, email: String?, onLogin: () -> Unit) {
    MyPageCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(56.dp).background(SoftBlue, CircleShape), contentAlignment = Alignment.Center) {
                ResultGlyph(ResultIcon.SHIELD, ProtectiveBlue, 28.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (signedIn) nickname ?: email ?: "내 계정" else "로그인이 필요해요",
                    color = BrandInk,
                    style = MaterialTheme.typography.titleMedium,
                )
                val description = when {
                    !signedIn -> "로그인하면 프로필을 확인할 수 있어요."
                    nickname != null -> email
                    email == null -> "저장된 프로필 정보가 없어요."
                    else -> null
                }
                description?.let { Text(it, color = SecondaryInk, style = MaterialTheme.typography.bodySmall) }
                if (signedIn) {
                    Text(
                        "Basic",
                        Modifier.background(SoftBlue, DundueniPillShape).padding(horizontal = 8.dp, vertical = 2.dp),
                        color = ProtectiveBlue,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        if (!signedIn) {
            DundueniButton("로그인", onLogin, Modifier.fillMaxWidth(), DundueniButtonType.SECONDARY)
        }
    }
}

@Composable
private fun MonthlyInspectionsCard(period: String, inspections: Int?, familyCount: Int?) {
    MyPageCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "이번 달 든든이 검사 현황", Modifier.weight(1f), color = BrandInk,
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
            )
            Text(period, color = SecondaryInk, style = MaterialTheme.typography.labelSmall)
        }
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val tile = Modifier.weight(1f).fillMaxHeight()
            StatTile("검사 횟수", inspections?.let { "${it}회" } ?: "—", tile)
            StatTile("사기 차단/예방", "—", tile)
            StatTile("가족 연결", familyCount?.let { "${it}명" } ?: "—", tile)
        }
        Text("검사는 저장 기록, 가족은 기기 등록 기준", color = SecondaryInk, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    Column(
        modifier.background(SoftBlue, DundueniDefaultShape).padding(horizontal = 6.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Text(label, color = SecondaryInk, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
        Text(value, color = BrandInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PlanCard(usage: BasicInspectionQuota.Usage?, onUpgrade: () -> Unit) {
    MyPageCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("현재 이용 중인 요금제", color = SecondaryInk, style = MaterialTheme.typography.labelSmall)
                Text(BASIC_PLAN, color = BrandInk, style = MaterialTheme.typography.titleMedium)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val used = usage?.usedInPeriod?.let { "${it}회" } ?: "—"
                val remaining = usage?.let { "${it.remaining}/${BasicInspectionQuota.LIMIT}회" } ?: "—"
                Text("이번 달 AI 검사 $used", color = SecondaryInk, style = MaterialTheme.typography.labelSmall)
                Text("남은 검사 $remaining", color = ProtectiveBlue, style = MaterialTheme.typography.labelLarge)
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(DundueniDefaultShape)
                .background(Brush.horizontalGradient(listOf(PrimaryMidnight, Color(0xFF0050CC))))
                .clickable(role = Role.Button, onClick = onUpgrade).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("업그레이드", color = Color.White, style = MaterialTheme.typography.labelLarge)
                Text("든든이 요금제 보기", color = SoftBlue, style = MaterialTheme.typography.bodySmall)
            }
            Box(Modifier.size(36.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = BrandInk)
            }
        }
    }
}

@Composable
private fun ModeCard(easy: Boolean, onChangeMode: (Boolean) -> Unit) {
    MyPageCard(color = SoftBlue) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).background(Color.White, DundueniDefaultShape), contentAlignment = Alignment.Center) {
                Text("가", color = ProtectiveBlue, style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f)) {
                Text(if (easy) "쉬운 모드 사용 중" else "쉬운 모드로 전환하기", color = BrandInk,
                    style = MaterialTheme.typography.labelLarge)
                Text("글씨를 더 크게, 조작을 더 단순하게 바꿔요.", color = SecondaryInk,
                    style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = easy,
                onCheckedChange = onChangeMode,
                modifier = Modifier.semantics { contentDescription = "쉬운 모드" },
                colors = SwitchDefaults.colors(checkedTrackColor = ProtectiveBlue),
            )
        }
    }
}

@Composable
private fun MyPageCard(color: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = color,
        shadowElevation = 1.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}
