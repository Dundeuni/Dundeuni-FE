package com.dundueni.app.feature.familyshare

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.dundueni.app.feature.home.HomeBottomNavigation
import com.dundueni.app.feature.home.HomeDestination
import com.dundueni.app.ui.theme.BrandInk
import com.dundueni.app.ui.theme.LocalEasyMode
import com.dundueni.app.ui.theme.PageBackground
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal data class FamilyShareState(
    val hasFamilyPlan: Boolean,
    val group: FamilyGroup? = null,
)

internal data class FamilyGroup(
    val id: String,
    val name: String,
    val members: List<FamilyMember>,
    val monthlyInspectionCount: Int? = null,
    val inviteCode: String? = null,
    val passphraseQuestion: String? = null,
    val settings: FamilyShareSettings? = null,
)

internal data class FamilyMember(
    val name: String,
    val monthlyInspectionCount: Int?,
)

internal data class FamilyShareSettings(
    val syncInterval: FamilySyncInterval,
    val maskSensitiveInfo: Boolean,
    val dangerAlerts: Boolean,
    val cautionAlerts: Boolean,
    val sosAlerts: Boolean,
)

internal enum class FamilySyncInterval(val label: String) {
    REAL_TIME("실시간"),
    HOURLY("1시간마다"),
    DAILY("하루 1회"),
}

internal sealed interface FamilyShareAction {
    data object CreateGroup : FamilyShareAction
    data object IssueInviteCode : FamilyShareAction
    data class JoinGroup(val code: String) : FamilyShareAction
    data class SavePassphrase(
        val question: String,
        val answer: String,
        val notifyFamily: Boolean,
    ) : FamilyShareAction {
        override fun toString() = "SavePassphrase(notifyFamily=$notifyFamily)"
    }
    data class UpdateSettings(val settings: FamilyShareSettings) : FamilyShareAction
}

internal sealed interface FamilyShareResult {
    data object Completed : FamilyShareResult
    data object Unavailable : FamilyShareResult
    data class Failed(val message: String) : FamilyShareResult
}

private enum class FamilySharePage(val title: String) {
    PLAN("가족 공유"),
    INVITATIONS("가족 공유"),
    DASHBOARD("가족 공유"),
    PASSPHRASE("가족 암구호"),
}

// Supply account state from the data source and return Completed after it confirms the change.
@Suppress("DEPRECATION")
@Composable
internal fun FamilyShareRoute(
    state: FamilyShareState,
    onAction: suspend (FamilyShareAction) -> FamilyShareResult,
    onBack: () -> Unit,
    onViewPlans: () -> Unit,
    onNavigationSelected: (HomeDestination) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val easy = LocalEasyMode.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var subpage by rememberSaveable { mutableStateOf<FamilySharePage?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val group = state.group
    val root = when {
        !state.hasFamilyPlan -> FamilySharePage.PLAN
        group == null -> FamilySharePage.INVITATIONS
        else -> FamilySharePage.DASHBOARD
    }
    val page = if (root == FamilySharePage.DASHBOARD) subpage ?: root else root
    val goBack: () -> Unit = { if (page == root) onBack() else subpage = null }

    fun showMessage(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    fun submit(action: FamilyShareAction) {
        if (submitting) return
        submitting = true
        scope.launch {
            val result = try {
                onAction(action)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                FamilyShareResult.Failed("가족 공유 요청을 처리하지 못했어요. 다시 시도해 주세요.")
            } finally {
                submitting = false
            }
            when (result) {
                FamilyShareResult.Completed -> when (action) {
                    FamilyShareAction.CreateGroup -> if (page == FamilySharePage.INVITATIONS) {
                        subpage = FamilySharePage.INVITATIONS
                    }
                    is FamilyShareAction.JoinGroup, is FamilyShareAction.SavePassphrase -> subpage = null
                    else -> Unit
                }
                FamilyShareResult.Unavailable -> showMessage("현재 이 기능을 이용할 수 없어요.")
                is FamilyShareResult.Failed -> showMessage(result.message)
            }
        }
    }

    BackHandler(onBack = goBack)
    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = PageBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = goBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기", tint = BrandInk)
                }
                Text(page.title, color = BrandInk, style = MaterialTheme.typography.titleMedium)
            }
        },
        bottomBar = {
            HomeBottomNavigation(HomeDestination.FAMILY_SHARE.takeUnless { easy }) { destination ->
                if (destination == HomeDestination.FAMILY_SHARE) subpage = null else onNavigationSelected(destination)
            }
        },
    ) { padding ->
        key(page, group?.id) {
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (page) {
                    FamilySharePage.PLAN -> FamilySharePlan(onViewPlans)
                    FamilySharePage.INVITATIONS -> FamilyShareInvitations(
                        group = group,
                        submitting = submitting,
                        onAction = ::submit,
                        onPaste = {
                            clipboard.getText()?.text?.trim()?.takeIf { it.isNotEmpty() }
                                .also { if (it == null) showMessage("붙여넣을 초대 코드가 없어요.") }
                        },
                        onCopy = { code ->
                            clipboard.setText(AnnotatedString(code))
                            showMessage("초대 코드를 복사했어요.")
                        },
                        onShare = { code ->
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "든든이 앱의 가족 공유에서 초대 코드를 입력해 주세요.\n$code")
                            }
                            try {
                                context.startActivity(Intent.createChooser(share, "가족 초대 코드 공유"))
                            } catch (_: ActivityNotFoundException) {
                                showMessage("초대 코드를 공유할 앱을 열 수 없어요.")
                            }
                        },
                    )
                    FamilySharePage.DASHBOARD -> FamilyShareDashboard(
                        group = checkNotNull(group),
                        submitting = submitting,
                        onInvite = { subpage = FamilySharePage.INVITATIONS },
                        onEditPassphrase = { subpage = FamilySharePage.PASSPHRASE },
                        onUpdateSettings = { submit(FamilyShareAction.UpdateSettings(it)) },
                    )
                    FamilySharePage.PASSPHRASE -> FamilySharePassphrase(
                        question = checkNotNull(group).passphraseQuestion,
                        submitting = submitting,
                        onSave = { question, answer, notifyFamily ->
                            submit(FamilyShareAction.SavePassphrase(question, answer, notifyFamily))
                        },
                        onCancel = goBack,
                    )
                }
            }
        }
    }
}
