package com.dundueni.app

import android.content.Intent
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dundueni.app.feature.floatingbutton.FloatingButtonManager
import com.dundueni.app.feature.auth.LoginActivity
import com.dundueni.app.data.auth.AuthSessionRuntime
import com.dundueni.app.feature.capture.MediaProjectionService
import com.dundueni.app.feature.home.HomeDestination
import com.dundueni.app.feature.home.HomeScreen
import com.dundueni.app.feature.photopicker.PhotoPickerActivity
import com.dundueni.app.feature.textinput.TextInputActivity
import com.dundueni.app.feature.url.UrlInputActivity
import com.dundueni.app.feature.phone.PhoneInputActivity
import com.dundueni.app.ui.theme.DundueniFETheme

class MainActivity : ComponentActivity() {
    private var floatingButtonEnabled by mutableStateOf(false)
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
        val authStore = AuthSessionRuntime.store(applicationContext)
        lastUnauthorizedRevision = AuthSessionRuntime.unauthorizedRevision.value
        authStore.read()?.takeIf { !it.isValid() }?.let {
            AuthSessionRuntime.onExpired(it)
            redirectToLogin()
            return
        }
        syncFloatingButtonState()

        setContent {
            DundueniFETheme {
                HomeScreen(
                    floatingButtonEnabled = floatingButtonEnabled,
                    onFloatingButtonChange = ::onFloatingButtonChange,
                    onScreenScan = ::requestScreenScan,
                    onImageScan = ::openPhotoPicker,
                    onTextScan = { startActivity(Intent(this@MainActivity, TextInputActivity::class.java)) },
                    onUrlScan = { startActivity(Intent(this@MainActivity, UrlInputActivity::class.java)) },
                    onPhoneScan = { startActivity(Intent(this@MainActivity, PhoneInputActivity::class.java)) },
                    onNotifications = { showMessage("준비 중인 기능입니다") },
                    onSettings = { showMessage("준비 중인 기능입니다") },
                    onViewAllHistory = { showMessage("준비 중인 기능입니다") },
                    onNavigationSelected = { destination ->
                        when (destination) {
                            HomeDestination.MY_SHELTER -> openAccount()
                            HomeDestination.HOME -> Unit
                            else -> showMessage("준비 중인 기능입니다")
                        }
                    },
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
        if (pendingOverlayAction == null) syncFloatingButtonState()
    }

    private fun openAccount() {
        val session = AuthSessionRuntime.store(applicationContext).read()
        if (session == null) {
            startActivity(Intent(this, LoginActivity::class.java))
        } else if (!session.isValid()) {
            AuthSessionRuntime.onExpired(session)
            lastUnauthorizedRevision = AuthSessionRuntime.unauthorizedRevision.value
            redirectToLogin()
        } else {
            AlertDialog.Builder(this)
                .setTitle("계정")
                .setMessage("로그인된 상태입니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("로그아웃") { _, _ ->
                    AuthSessionRuntime.store(applicationContext).clear()
                    redirectToLogin()
                }
                .show()
        }
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
            syncFloatingButtonState()
            if (!floatingButtonEnabled) showMessage("안심 버튼을 표시하지 못했습니다.")
        }, 700L)
    }

    private fun syncFloatingButtonState() {
        floatingButtonEnabled = FloatingButtonManager.isRunning()
    }

    private fun openPhotoPicker() {
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
