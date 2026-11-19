package com.dundueni.app.feature.mypage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dundueni.app.BuildConfig
import com.dundueni.app.data.auth.AuthSessionRuntime
import com.dundueni.app.data.auth.AuthSessionSource
import com.dundueni.app.feature.analysis.AnalysisFlowDependencies
import com.dundueni.app.feature.floatingbutton.FloatingButtonPreferences
import com.dundueni.app.feature.history.HistoryRecord
import com.dundueni.app.feature.history.HistoryStore
import com.dundueni.app.feature.history.toHistoryRecord
import com.dundueni.app.feature.home.HomeDestination
import com.dundueni.app.feature.payment.BasicInspectionQuota
import com.dundueni.app.feature.settings.readFamilyContacts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

private const val HISTORY_LIMIT = 100

@Composable
internal fun MyPageRoute(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onUpgrade: () -> Unit,
    onNavigationSelected: (HomeDestination) -> Unit,
    onLogoutConfirmed: () -> Unit = {},
    onWithdrawalConfirmed: () -> Unit = {},
) {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val sessionStore = remember(context) { AuthSessionRuntime.store(context) }
    val historyStore = remember(context) { HistoryStore(context) }
    val quota = remember(context) { BasicInspectionQuota(context) }
    val settings = remember(context) {
        context.getSharedPreferences(FloatingButtonPreferences.PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
    val unauthorizedRevision by AuthSessionRuntime.unauthorizedRevision.collectAsStateWithLifecycle()
    var session by remember(sessionStore) { mutableStateOf(sessionStore.read()?.takeIf { it.isValid() }) }
    var today by remember { mutableStateOf(startOfToday()) }
    var resumeRevision by remember { mutableIntStateOf(0) }
    var localRevision by remember { mutableIntStateOf(0) }
    var inspectionRevision by remember { mutableIntStateOf(0) }

    DisposableEffect(context, lifecycle) {
        val sources = listOf(
            settings,
            context.getSharedPreferences(HistoryStore.PREFERENCES_NAME, Context.MODE_PRIVATE),
            context.getSharedPreferences(BasicInspectionQuota.PREFERENCES_NAME, Context.MODE_PRIVATE),
        )
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { source, _ ->
            localRevision++
            if (source != settings) inspectionRevision++
        }
        sources.forEach { it.registerOnSharedPreferenceChangeListener(listener) }
        val clock = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                today = startOfToday()
            }
        }
        val clockChanges = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, clock, clockChanges, ContextCompat.RECEIVER_NOT_EXPORTED)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                today = startOfToday()
                resumeRevision++
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            context.unregisterReceiver(clock)
            sources.forEach { it.unregisterOnSharedPreferenceChangeListener(listener) }
        }
    }

    LaunchedEffect(resumeRevision, unauthorizedRevision) {
        session = sessionStore.read()?.takeIf { it.isValid() }
    }

    val date = today.toLocalDate()
    val firstDate = date.withDayOfMonth(1)
    val startMillis = firstDate.atStartOfDay(today.zone).toInstant().toEpochMilli()
    val endMillis = date.plusDays(1).atStartOfDay(today.zone).toInstant().toEpochMilli()
    val period = startMillis until endMillis
    var localStats by remember(period) { mutableStateOf<LocalStats?>(null) }
    LaunchedEffect(period, resumeRevision, localRevision) {
        localStats = withContext(Dispatchers.IO) {
            val records = runCatching { historyStore.read() }.getOrNull().orEmpty()
                .filterNot { it.id.startsWith("link-phone-scan-") }
            val contacts = runCatching { settings.readFamilyContacts() }.getOrNull()
            LocalStats(
                savedInspections = records.takeIf { it.isNotEmpty() }?.count { it.savedAt in period },
                familyCount = contacts?.count { (_, phone) -> phone.any(Char::isDigit) && '*' !in phone }
                    ?.takeIf { it > 0 },
                quotaUsage = quota.usageBetween(startMillis, endMillis),
            )
        }
    }

    val backendSession = session?.takeIf { it.source == AuthSessionSource.BACKEND }
    var serverRecords by remember(backendSession) { mutableStateOf<List<HistoryRecord>?>(null) }
    LaunchedEffect(backendSession, resumeRevision, inspectionRevision, today) {
        if (backendSession != null && BuildConfig.ANALYSIS_API_MODE.equals("REAL", ignoreCase = true)) {
            serverRecords = try {
                withContext(Dispatchers.IO) {
                    AnalysisFlowDependencies.createHistoryRepository(context).list(HISTORY_LIMIT)
                        .map { it.toHistoryRecord() }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
    }

    val records = serverRecords
    val inspections = when {
        records == null -> localStats?.savedInspections
        records.isEmpty() -> null
        records.size == HISTORY_LIMIT && records.none { it.savedAt < startMillis } -> null
        else -> records.count { it.status == "COMPLETED" && it.savedAt in period }
    }

    MyPageScreen(
        signedIn = session != null,
        nickname = backendSession?.nickname,
        email = backendSession?.email,
        firstDate = firstDate,
        today = date,
        inspections = inspections,
        familyCount = localStats?.familyCount,
        quotaUsage = localStats?.quotaUsage,
        onBack = onBack,
        onLogin = onLogin,
        onLogoutConfirmed = onLogoutConfirmed,
        onWithdrawalConfirmed = onWithdrawalConfirmed,
        onUpgrade = onUpgrade,
        onChangeMode = { easy ->
            settings.edit().putBoolean(FloatingButtonPreferences.KEY_EASY_MODE, easy).apply()
        },
        onNavigationSelected = onNavigationSelected,
    )
}

private fun startOfToday(): ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)

private data class LocalStats(
    val savedInspections: Int?,
    val familyCount: Int?,
    val quotaUsage: BasicInspectionQuota.Usage,
)
