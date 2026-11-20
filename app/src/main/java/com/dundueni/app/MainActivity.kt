package com.dundueni.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.dundueni.app.feature.floatingbutton.FloatingButtonManager
import com.dundueni.app.feature.auth.LoginActivity
import com.dundueni.app.data.auth.AuthSessionRuntime
import com.dundueni.app.feature.capture.MediaProjectionService
import com.dundueni.app.feature.familyshare.FamilyShareResult
import com.dundueni.app.feature.familyshare.FamilyShareRoute
import com.dundueni.app.feature.familyshare.FamilyShareState
import com.dundueni.app.feature.home.HomeDestination
import com.dundueni.app.feature.home.HomeScreen
import com.dundueni.app.feature.mypage.MyPageRoute
import com.dundueni.app.feature.history.HistoryActivity
import com.dundueni.app.feature.photopicker.PhotoPickerActivity
import com.dundueni.app.feature.textinput.TextInputActivity
import com.dundueni.app.feature.phone.PhoneInputActivity
import com.dundueni.app.feature.payment.PaymentActivity
import com.dundueni.app.feature.settings.SettingsScreen
import com.dundueni.app.feature.onboarding.OnboardingScreen
import com.dundueni.app.feature.payment.showLimitIfExhausted
import com.dundueni.app.ui.theme.LocalEasyMode
import com.dundueni.app.ui.theme.DundueniFETheme

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_OPEN_SETTINGS = "com.dundueni.app.extra.OPEN_SETTINGS"
        const val EXTRA_OPEN_MY_PAGE = "com.dundueni.app.extra.OPEN_MY_PAGE"
        const val EXTRA_OPEN_FAMILY_SHARE = "com.dundueni.app.extra.OPEN_FAMILY_SHARE"
        private const val STATE_DESTINATION = "main_destination"
        private const val ONBOARDING_PREFERENCES = "dundueni_onboarding"
        private const val ONBOARDING_COMPLETED = "completed"
        private const val STATE_ONBOARDING_PAGE = "onboarding_page"
    }

    private var floatingButtonEnabled by mutableStateOf(false)
    private var currentDestination by mutableStateOf(HomeDestination.HOME)
    private var showingOnboarding by mutableStateOf(false)
    private var onboardingPage by mutableIntStateOf(1)
    private var pendingOverlayAction: OverlayAction? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastUnauthorizedRevision = 0L
    private var redirectingToLogin = false

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        val action = pendingOverlayAction
        pendingOverlayAction = null
        if (FloatingButtonManager.hasOverlayPermission(this)) {
            when (action) {
                OverlayAction.ENABLE -> startFloatingButton()
                OverlayAction.SCREEN_SCAN -> startScreenScan()
                null -> syncFloatingButtonState()
            }
        } else {
            syncFloatingButtonState()
            if (action == OverlayAction.SCREEN_SCAN) {
                showMessage("화면 검사를 사용하려면 오버레이 권한이 필요합니다.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        currentDestination = savedInstanceState?.getString(STATE_DESTINATION)?.let { HomeDestination.valueOf(it) }
            ?: requestedDestination(intent) ?: HomeDestination.HOME
        lastUnauthorizedRevision = AuthSessionRuntime.unauthorizedRevision.value
        val session = AuthSessionRuntime.store(applicationContext).read()
        if (session != null && !session.isValid()) {
            AuthSessionRuntime.onExpired(session)
            redirectToLogin()
            return
        }
        val onboardingCompleted = session?.onboardingCompleted
            ?: getSharedPreferences(ONBOARDING_PREFERENCES, MODE_PRIVATE)
                .getBoolean(ONBOARDING_COMPLETED, false)
        showingOnboarding = !onboardingCompleted
        onboardingPage = savedInstanceState?.getInt(STATE_ONBOARDING_PAGE, 1) ?: 1
        syncFloatingButtonState()

        setContent {
            DundueniFETheme {
                val easy = LocalEasyMode.current
                val navigate: (HomeDestination) -> Unit = { destination ->
                    when (destination) {
                        HomeDestination.HOME, HomeDestination.SETTINGS, HomeDestination.MY_SHELTER,
                        HomeDestination.FAMILY_SHARE ->
                            currentDestination = destination
                        HomeDestination.REPORT -> {
                            val fromFamilyShare = currentDestination == HomeDestination.FAMILY_SHARE
                            if (easy || currentDestination == HomeDestination.MY_SHELTER) {
                                currentDestination = HomeDestination.HOME
                            }
                            startActivity(Intent(this@MainActivity, HistoryActivity::class.java).apply {
                                if (fromFamilyShare && !easy) {
                                    putExtra(HistoryActivity.EXTRA_FROM_FAMILY_SHARE, true)
                                }
                            })
                        }
                        HomeDestination.CONSULTATION -> showMessage("준비 중인 기능입니다")
                    }
                }
                if (showingOnboarding) {
                    OnboardingScreen(
                        page = onboardingPage,
                        onNext = { onboardingPage = (onboardingPage + 1).coerceAtMost(4) },
                        onSkip = { showingOnboarding = false },
                        onOverlayPermission = {
                            onboardingOverlayPermissionLauncher.launch(
                                FloatingButtonManager.createOverlayPermissionIntent(this@MainActivity),
                            )
                        },
                        onStart = ::completeOnboarding,
                    )
                } else if (currentDestination == HomeDestination.SETTINGS) {
                    SettingsScreen(navigate)
                } else if (currentDestination == HomeDestination.FAMILY_SHARE) {
                    FamilyShareRoute(
                        // The current payment flow provides no paid entitlement or family group.
                        state = FamilyShareState(hasFamilyPlan = false),
                        onAction = { FamilyShareResult.Unavailable },
                        onBack = { navigate(HomeDestination.HOME) },
                        onViewPlans = { startActivity(PaymentActivity.createIntent(this@MainActivity)) },
                        onNavigationSelected = navigate,
                    )
                } else if (currentDestination == HomeDestination.MY_SHELTER) {
                    val goHome = { navigate(HomeDestination.HOME) }
                    BackHandler(onBack = goHome)
                    MyPageRoute(
                        onBack = goHome,
                        onLogin = { startActivity(Intent(this@MainActivity, LoginActivity::class.java)) },
                        onUpgrade = { startActivity(PaymentActivity.createIntent(this@MainActivity)) },
                        onNavigationSelected = navigate,
                    )
                } else HomeScreen(
                    floatingButtonEnabled = floatingButtonEnabled,
                    onFloatingButtonChange = ::onFloatingButtonChange,
                    onScreenScan = ::requestScreenScan,
                    onImageScan = ::openPhotoPicker,
                    onTextScan = { startActivity(Intent(this@MainActivity, TextInputActivity::class.java)) },
                    onPhoneScan = { startActivity(Intent(this@MainActivity, PhoneInputActivity::class.java)) },
                    onNotifications = { showMessage("준비 중인 기능입니다") },
                    onMembership = { startActivity(PaymentActivity.createIntent(this@MainActivity)) },
                    onRecentHistory = { id -> startActivity(HistoryActivity.createIntent(this@MainActivity, id)) },
                    onViewAllHistory = { startActivity(Intent(this@MainActivity, HistoryActivity::class.java)) },
                    onNavigationSelected = navigate,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val unauthorizedRevision = AuthSessionRuntime.unauthorizedRevision.value
        if (unauthorizedRevision > lastUnauthorizedRevision) {
            lastUnauthorizedRevision = unauthorizedRevision
            redirectToLogin()
            return
        }
        AuthSessionRuntime.store(applicationContext).read()?.takeIf { !it.isValid() }?.let {
            AuthSessionRuntime.onExpired(it)
            lastUnauthorizedRevision = AuthSessionRuntime.unauthorizedRevision.value
            redirectToLogin()
            return
        }
        if (pendingOverlayAction == null) {
            syncFloatingButtonState()
            if (floatingButtonEnabled) FloatingButtonManager.restoreIfEnabled(this)
            android.util.Log.d("FloatingButtonService", "MainActivity.onResume toggle=$floatingButtonEnabled ${com.dundueni.app.feature.floatingbutton.FloatingButtonService.debugState(this)}")
        }
    }

    private val onboardingOverlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (showingOnboarding && onboardingPage == 3) onboardingPage = 4
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val requested = requestedDestination(intent)
        if (requested != null || currentDestination == HomeDestination.MY_SHELTER ||
            currentDestination == HomeDestination.FAMILY_SHARE
        ) {
            currentDestination = requested ?: HomeDestination.HOME
        }
    }

    private fun requestedDestination(intent: Intent): HomeDestination? = when {
        intent.getBooleanExtra(EXTRA_OPEN_SETTINGS, false) -> HomeDestination.SETTINGS
        intent.getBooleanExtra(EXTRA_OPEN_MY_PAGE, false) -> HomeDestination.MY_SHELTER
        intent.getBooleanExtra(EXTRA_OPEN_FAMILY_SHARE, false) -> HomeDestination.FAMILY_SHARE
        else -> null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_DESTINATION, currentDestination.name)
        outState.putInt(STATE_ONBOARDING_PAGE, onboardingPage)
        super.onSaveInstanceState(outState)
    }

    private fun completeOnboarding() {
        // TODO(BE_CONTRACT): onboarding completion API is outside PB-09; retain locally for this session.
        val authStore = AuthSessionRuntime.store(applicationContext)
        authStore.read()?.takeIf { it.onboardingCompleted == false && it.isValid() }?.let {
            runCatching { authStore.save(it.copy(onboardingCompleted = true)) }.onFailure {
                showMessage("설정을 저장하지 못했습니다. 다시 시도해 주세요.")
                return
            }
        }
        getSharedPreferences(ONBOARDING_PREFERENCES, MODE_PRIVATE)
            .edit()
            .putBoolean(ONBOARDING_COMPLETED, true)
            .apply()
        showingOnboarding = false
    }

    private fun redirectToLogin() {
        if (redirectingToLogin || isFinishing) return
        redirectingToLogin = true
        FloatingButtonManager.stop(this)
        stopService(Intent(this, MediaProjectionService::class.java))
        floatingButtonEnabled = false
        startActivity(Intent(this, LoginActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
        ))
    }

    private fun onFloatingButtonChange(enabled: Boolean) {
        if (!enabled) {
            FloatingButtonManager.stop(this)
            floatingButtonEnabled = false
            return
        }

        if (FloatingButtonManager.hasOverlayPermission(this)) {
            startFloatingButton()
        } else {
            requestOverlayPermission(OverlayAction.ENABLE)
        }
    }

    private fun requestScreenScan() {
        if (showLimitIfExhausted(this)) return
        if (FloatingButtonManager.hasOverlayPermission(this)) {
            startScreenScan()
        } else {
            requestOverlayPermission(OverlayAction.SCREEN_SCAN)
        }
    }

    private fun requestOverlayPermission(action: OverlayAction) {
        pendingOverlayAction = action
        try {
            overlayPermissionLauncher.launch(FloatingButtonManager.createOverlayPermissionIntent(this))
        } catch (_: Exception) {
            pendingOverlayAction = null
            syncFloatingButtonState()
            showMessage("오버레이 권한 설정을 열지 못했습니다.")
        }
    }

    private fun startFloatingButton() {
        try {
            FloatingButtonManager.start(this)
            verifyFloatingButtonStarted()
        } catch (_: Exception) {
            syncFloatingButtonState()
            showMessage("안심 버튼을 시작하지 못했습니다.")
        }
    }

    private fun startScreenScan() {
        if (showLimitIfExhausted(this)) return
        try {
            FloatingButtonManager.startScreenScan(this)
        } catch (_: Exception) {
            syncFloatingButtonState()
            showMessage("화면 검사를 시작하지 못했습니다.")
        }
    }

    private fun verifyFloatingButtonStarted() {
        floatingButtonEnabled = true
        mainHandler.postDelayed({
            if (!FloatingButtonManager.isUserEnabled(this)) return@postDelayed
            syncFloatingButtonState()
            if (!floatingButtonEnabled) showMessage("안심 버튼을 표시하지 못했습니다.")
        }, 700L)
    }

    private fun syncFloatingButtonState() {
        floatingButtonEnabled = FloatingButtonManager.isUserEnabled(this)
    }

    private fun openPhotoPicker() {
        if (showLimitIfExhausted(this)) return
        try {
            startActivity(Intent(this, PhotoPickerActivity::class.java).apply {
                putExtra(PhotoPickerActivity.EXTRA_OPEN_PICKER, true)
            })
        } catch (_: Exception) {
            showMessage("이미지 선택 화면을 열지 못했습니다.")
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private enum class OverlayAction { ENABLE, SCREEN_SCAN }
}
