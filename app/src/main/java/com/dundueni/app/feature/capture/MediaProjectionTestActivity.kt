package com.dundueni.app.feature.capture

import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * 이름에 Test가 있지만 실제 화면 검사에서 권한 승인을 중계하는 Activity다.
 * 다른 앱의 화면을 읽으려면 사용자가 시스템 화면 공유 창에서 승인해야 한다.
 * 플로팅 서비스의 요청을 받아 승인 창을 열고, 승인 결과를 캡처 서비스에 넘긴다.
 * ResultReceiver는 Activity와 플로팅 서비스가 상태와 응답을 주고받는 통로다.
 * 요청한 쪽이 여전히 유효한지 확인해 불필요한 캡처 서비스 실행을 막는다.
 * 이 Activity가 닫힌 뒤 플로팅 서비스에 알리고, 실제 캡처는 서비스에 맡긴다.
 */
class MediaProjectionTestActivity : ComponentActivity() {
    companion object {
        const val EXTRA_RECEIVER = "capture_result_receiver"
        const val EXTRA_REPLY = "capture_owner_reply"
        const val PERMISSION_OPENED = 1
        const val PERMISSION_APPROVED = 2
        const val PERMISSION_CANCELLED = 3
        const val CHECK_REQUEST = 4
        private const val STATE_COMPLETED = "permission_completed"
        private const val STATE_STARTED = "service_started"
        private const val STATE_REQUESTED = "permission_requested"
    }

    private var receiver: ResultReceiver? = null
    private var completed = false
    private var serviceStarted = false
    private var permissionRequested = false
    private val handler = Handler(Looper.getMainLooper())
    private val ownerTimeout = Runnable { finish() }
    private val captureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (completed) return@registerForActivityResult
            completed = true
            if (result.resultCode == RESULT_OK && result.data != null) {
                // Do not start an orphan FGS if the floating flow was cancelled/destroyed.
                confirmOwner(CHECK_REQUEST) {
                    try {
                        // 승인 결과를 캡처 서비스에 넘겨 화면 공유 세션을 시작하게 한다.
                        ContextCompat.startForegroundService(this,
                            Intent(this, MediaProjectionService::class.java).apply {
                                putExtra(MediaProjectionService.EXTRA_RESULT_CODE, result.resultCode)
                                putExtra(MediaProjectionService.EXTRA_DATA, result.data)
                            })
                        serviceStarted = true
                    } catch (error: Exception) {
                        Log.e("ProjectionPermission", "캡처 서비스 시작 실패", error)
                    }
                    finish()
                }
            } else {
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        val resultReceiver = intent.getParcelableExtra<ResultReceiver>(EXTRA_RECEIVER)
        receiver = resultReceiver
        completed = savedInstanceState?.getBoolean(STATE_COMPLETED) ?: false
        serviceStarted = savedInstanceState?.getBoolean(STATE_STARTED) ?: false
        permissionRequested = savedInstanceState?.getBoolean(STATE_REQUESTED) ?: false
        if (receiver == null || completed) {
            finish()
            return
        }
        confirmOwner(PERMISSION_OPENED) {
            // Activity Result registry restores the in-flight request on recreation.
            if (permissionRequested) return@confirmOwner
            permissionRequested = true
            try {
                // 화면을 직접 읽기 전에 Android가 제공하는 사용자 승인 창을 준비한다.
                val manager = getSystemService(MediaProjectionManager::class.java)
                val captureIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
                } else {
                    manager.createScreenCaptureIntent()
                }
                captureLauncher.launch(captureIntent)
            } catch (error: Exception) {
                Log.e("ProjectionPermission", "화면 공유 승인 화면 실행 실패", error)
                completed = true
                finish()
            }
        }
    }

    // 요청한 플로팅 서비스가 응답할 때만 진행하며, 응답이 없으면 중계 화면을 닫는다.
    private fun confirmOwner(event: Int, onConfirmed: () -> Unit) {
        handler.postDelayed(ownerTimeout, 5_000L)
        val reply = object : ResultReceiver(handler) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                handler.removeCallbacks(ownerTimeout)
                if (isFinishing || isDestroyed) return
                if (resultCode == RESULT_OK) onConfirmed() else finish()
            }
        }
        receiver?.send(event, Bundle().apply { putParcelable(EXTRA_REPLY, reply) })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_COMPLETED, completed)
        outState.putBoolean(STATE_STARTED, serviceStarted)
        outState.putBoolean(STATE_REQUESTED, permissionRequested)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        handler.removeCallbacks(ownerTimeout)
        super.onDestroy()
        // Do not arm capture while this Activity is still alive, or on configuration change.
        if (!isChangingConfigurations) {
            receiver?.send(if (serviceStarted) PERMISSION_APPROVED else PERMISSION_CANCELLED, null)
        }
    }
}
