package com.dundueni.app.feature.floatingbutton

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ResultReceiver
import android.provider.Settings
import android.util.Log
import android.view.Choreographer
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import com.dundueni.app.MainActivity
import com.dundueni.app.R
import com.dundueni.app.data.model.ImageInput
import com.dundueni.app.feature.analysis.AnalysisActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.dundueni.app.feature.capture.MediaProjectionService
import com.dundueni.app.feature.capture.MediaProjectionTestActivity
import com.dundueni.app.feature.floatingbutton.view.FloatingButtonView
import com.dundueni.app.feature.floatingbutton.view.FloatingMenuView
import com.dundueni.app.feature.photopicker.PhotoPickerActivity
import java.io.File
import java.io.IOException

/**
 * Activity 화면과 별개로 플로팅 버튼과 검사 메뉴의 표시 수명을 관리한다.
 * Foreground Service는 실행 상태를 알림으로 알리며 동작하는 서비스다.
 * 시작 시 알림과 Overlay 권한을 준비한 뒤 WindowManager로 버튼을 표시한다.
 * WindowManager는 View를 화면의 창으로 등록하고 제거하는 Android 기능이다.
 * FloatingButtonView와 FloatingMenuView는 모양과 클릭 전달을 담당한다.
 * 이 서비스는 메뉴 선택을 화면 검사 또는 사진 선택 흐름에 연결한다.
 * 화면 캡처 제어도 함께 들어 있지만, 버튼·메뉴 표시와는 별도 책임이다.
 */
class FloatingButtonService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingButton: ImageButton? = null
    private var floatingMenu: View? = null
    private var isMenuOpen = false
    private val captureHandler = Handler(Looper.getMainLooper())
    private var scanInProgress = false
    private var scanGeneration = 0
    private var captureService: MediaProjectionService? = null
    private var captureConnection: ServiceConnection? = null
    private var captureFrameCallback: Choreographer.FrameCallback? = null
    private var destroyed = false
    private val analysisHandoffScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val scanTimeout = Runnable { finishScreenScan("화면 검사를 시작하지 못했습니다.") }

    companion object {
        private const val CHANNEL_ID = "floating_button_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()

        // 1. Foreground Service용 알림 채널 생성
        createNotificationChannel()

        // 2. Service를 Foreground Service로 전환
        startAsForegroundService()

        // 3. Overlay 권한이 없으면 버튼을 띄울 수 없으므로 Service 종료
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        // 4. WindowManager 가져오기
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // 5. Floating Button 화면에 표시
        showFloatingButton()
    }

    // 실제 Floating Button 생성 + 화면에 추가
    private fun showFloatingButton() {

        if (floatingButton != null) return
        // View는 클릭만 전달하고, 메뉴 전환과 검사 중 입력 제한은 서비스가 결정한다.
        floatingButton = FloatingButtonView.create(this) {
            if (!scanInProgress) {
                if (isMenuOpen) hideFloatingMenu() else showFloatingMenu()
            }
        }

        // 72dp → px 변환
        val buttonSize = (72 * resources.displayMetrics.density).toInt()

        // 앱 밖에 표시할 창으로 설정하고, 키보드 입력 포커스는 가져오지 않는다.
        val params = WindowManager.LayoutParams(
            buttonSize,
            buttonSize,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {

            // 화면 오른쪽 중앙
            gravity = Gravity.END or Gravity.CENTER_VERTICAL

            // 오른쪽에서 약간 떨어뜨림
            x = 30
            y = 0
        }

        // 생성한 버튼을 창에 등록해야 다른 앱 위에서도 실제로 보인다.
        windowManager.addView(floatingButton, params)
    }

    private fun showFloatingMenu() {
        // 메뉴 중복 표시와 검사 중 재진입을 막고, 이어서 권한 취소 여부도 확인한다.
        if (isMenuOpen || scanInProgress) return
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        // 닫아 둔 메뉴 View는 재사용하고, 처음 열 때만 선택 동작을 연결한다.
        val menu = floatingMenu ?: FloatingMenuView.create(
            context = this,
            // 화면 검사 선택은 별도의 캡처 흐름으로 넘긴다.
            onScreenScanClick = { startScreenScan() },
            // 이미지 검사 선택은 메뉴를 닫고 사진 선택 화면으로 넘긴다.
            onImageScanClick = {
                if (isMenuOpen) {
                    hideFloatingMenu()
                    startActivity(
                        Intent(this, PhotoPickerActivity::class.java).apply {
                            // Service에서 실행하고, 기존 Picker 화면이 있으면 새 요청으로 교체한다.
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            // 다음 화면이 열리자마자 시스템 사진 선택기를 띄우도록 요청한다.
                            putExtra(PhotoPickerActivity.EXTRA_OPEN_PICKER, true)
                        }
                    )
                }
            },
            onEmptyClick = { /* TODO: 빈 메뉴 기능 정의 */ }
        ).also { floatingMenu = it }

        val density = resources.displayMetrics.density
        val offset = 30 + (80 * density).toInt()
        val params = WindowManager.LayoutParams(
            minOf((300 * density).toInt(), resources.displayMetrics.widthPixels - offset)
                .coerceAtLeast(1),
            (280 * density).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            // 버튼 창과 겹치지 않게 왼쪽에 배치한다.
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = offset
            y = 0
        }

        try {
            // 메뉴는 버튼과 별도 창으로 표시하고, 버튼 모양도 닫기 상태로 맞춘다.
            windowManager.addView(menu, params)
            isMenuOpen = true
            floatingButton?.let { FloatingButtonView.setMenuOpen(it, true) }
        } catch (error: WindowManager.BadTokenException) {
            Log.w("FloatingButtonService", "메뉴 오버레이를 표시할 수 없습니다.", error)
            stopSelf()
        } catch (error: SecurityException) {
            Log.w("FloatingButtonService", "오버레이 권한이 취소되었습니다.", error)
            stopSelf()
        }
    }

    // 서비스와 버튼은 유지하고 메뉴 창만 떼어 낸 뒤 버튼 모양을 되돌린다.
    private fun hideFloatingMenu() {
        if (!isMenuOpen) return
        removeOverlay(floatingMenu)
        isMenuOpen = false
        floatingButton?.let { FloatingButtonView.setMenuOpen(it, false) }
    }

    // 메뉴의 화면 검사 요청부터 승인·캡처·정리까지 이 서비스가 진행 순서를 관리한다.
    private fun startScreenScan() {
        if (scanInProgress || destroyed) return
        scanInProgress = true
        // 이전 요청의 늦은 응답을 새 검사에 섞지 않도록 요청 번호를 구분한다.
        val generation = ++scanGeneration
        hideFloatingMenu()
        floatingButton?.isEnabled = false
        // 승인 Activity의 상태를 받아 요청 유효성을 답하고 캡처 서비스 연결 시점을 정한다.
        val receiver = object : ResultReceiver(captureHandler) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                @Suppress("DEPRECATION")
                val reply = resultData?.getParcelable<ResultReceiver>(MediaProjectionTestActivity.EXTRA_REPLY)
                if (!scanInProgress || generation != scanGeneration || destroyed) {
                    reply?.send(Activity.RESULT_CANCELED, null)
                    return
                }
                when (resultCode) {
                    MediaProjectionTestActivity.PERMISSION_OPENED -> {
                        // User may take as long as needed to decide. No permission idle timeout.
                        // 승인 창이 열린 뒤에는 사용자의 결정을 기다리므로 시작 대기 제한을 해제한다.
                        captureHandler.removeCallbacks(scanTimeout)
                        // 검사할 화면에 우리 버튼이 함께 찍히지 않도록 Overlay를 잠시 제거한다.
                        removeOverlay(floatingButton)
                        floatingButton = null
                        reply?.send(Activity.RESULT_OK, null)
                    }
                    MediaProjectionTestActivity.CHECK_REQUEST -> reply?.send(Activity.RESULT_OK, null)
                    MediaProjectionTestActivity.PERMISSION_APPROVED -> {
                        captureHandler.postDelayed(scanTimeout, 10_000L)
                        connectCaptureService(generation)
                    }
                    MediaProjectionTestActivity.PERMISSION_CANCELLED -> finishScreenScan()
                }
            }
        }
        captureHandler.postDelayed(scanTimeout, 10_000L)
        try {
            startActivity(Intent(this, MediaProjectionTestActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                putExtra(MediaProjectionTestActivity.EXTRA_RECEIVER, receiver)
            })
        } catch (error: Exception) {
            Log.e("FloatingButtonService", "화면 공유 승인 Activity 실행 실패", error)
            finishScreenScan("화면 공유 승인을 시작하지 못했습니다.")
        }
    }

    private fun connectCaptureService(generation: Int) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                if (!scanInProgress || generation != scanGeneration) return
                // Binder로 캡처 서비스에 접근하고, 세션 준비가 끝났는지 확인한다.
                val service = (binder as? MediaProjectionService.LocalBinder)?.service
                captureService = service
                if (service?.sessionState != MediaProjectionService.SessionState.ACTIVE) {
                    finishScreenScan("화면 캡처 세션을 시작하지 못했습니다.")
                    return
                }
                // Permission Activity has been destroyed and overlays are detached.
                // Allow window removal to pass display frame boundaries, without sleeping.
                waitForCaptureFrame(generation, 2) {
                    service.requestCapture { result ->
                        if (!scanInProgress || generation != scanGeneration) {
                            result.getOrNull()?.bitmap?.recycle()
                            return@requestCapture
                        }
                        // MVP policy belongs here, NOT inside requestCapture().
                        // 현재 흐름은 한 장만 사용하므로 파일 저장에 앞서 화면 공유를 끝낸다.
                        service.stopSession()
                        result.fold(
                            onSuccess = { input ->
                                try {
                                    onScreenCaptureReady(input)
                                } finally {
                                    finishScreenScan()
                                }
                            },
                            onFailure = { error ->
                                Log.w("FloatingButtonService", "화면 캡처 실패", error)
                                finishScreenScan("화면 캡처에 실패했습니다. 다시 시도해 주세요.")
                            }
                        )
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                captureService = null
                if (generation == scanGeneration) finishScreenScan("화면 공유가 종료되었습니다.")
            }

            override fun onNullBinding(name: ComponentName?) {
                if (generation == scanGeneration) finishScreenScan("캡처 서비스에 연결하지 못했습니다.")
            }

            override fun onBindingDied(name: ComponentName?) {
                if (generation == scanGeneration) finishScreenScan("캡처 서비스 연결이 종료되었습니다.")
            }
        }
        try {
            // 승인 Activity가 시작한 서비스에 연결해 캡처 요청과 결과를 주고받는다.
            if (bindService(Intent(this, MediaProjectionService::class.java), connection, BIND_AUTO_CREATE)) {
                captureConnection = connection
            } else {
                finishScreenScan("캡처 서비스에 연결하지 못했습니다.")
            }
        } catch (error: Exception) {
            Log.e("FloatingButtonService", "캡처 서비스 연결 실패", error)
            finishScreenScan("캡처 서비스에 연결하지 못했습니다.")
        }
    }

    // 화면 갱신 시점을 기다려 승인 창과 Overlay가 사라진 뒤 캡처를 요청한다.
    private fun waitForCaptureFrame(generation: Int, remaining: Int, ready: () -> Unit) {
        val callback = Choreographer.FrameCallback {
            captureFrameCallback = null
            if (scanInProgress && generation == scanGeneration && !destroyed) {
                if (remaining > 1) waitForCaptureFrame(generation, remaining - 1, ready)
                else ready()
            }
        }
        captureFrameCallback = callback
        Choreographer.getInstance().postFrameCallback(callback)
    }

    private fun onScreenCaptureReady(input: ImageInput.ScreenCapture) {
        // Bitmap을 Binder에 싣지 않고 앱 내부 PNG 캐시를 전달합니다. 기존 PNG 인코딩은 한 번만 수행합니다.
        // 큰 Bitmap의 Intent 전달 용량 제한을 피하고, 파일 쓰기는 화면을 막지 않게 처리한다.
        analysisHandoffScope.launch(start = CoroutineStart.UNDISPATCHED) {
            var file: File? = null
            var handedOff = false
            try {
                withContext(Dispatchers.IO) {
                    val target = File.createTempFile("capture_", ".png", AnalysisActivity.captureDirectory(this@FloatingButtonService))
                    file = target
                    target.outputStream().use { output ->
                        if (!input.bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                            throw IOException("Failed to encode screen capture as PNG")
                        }
                    }
                }
                // PNG 저장이 끝나면 파일 정보를 넘기고, 이후 파일 정리는 분석 화면에 맡긴다.
                startActivity(AnalysisActivity.forCapture(this@FloatingButtonService, checkNotNull(file)))
                handedOff = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w("FloatingButtonService", "분석 화면으로 캡처 전달 실패", error)
                Toast.makeText(this@FloatingButtonService, "검사를 시작하지 못했습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            } finally {
                // Bitmap은 여기서 해제하고, 전달하지 못한 임시 파일도 삭제한다.
                input.bitmap.recycle()
                if (!handedOff) file?.delete()
            }
        }
    }

    // 성공·취소·실패가 같은 정리 경로를 거쳐 다음 검사를 받을 수 있게 한다.
    private fun finishScreenScan(message: String? = null) {
        if (!scanInProgress) return
        scanInProgress = false
        captureHandler.removeCallbacks(scanTimeout)
        // 예약된 프레임 대기를 취소한 뒤 캡처 세션을 끝내고 서비스 연결을 해제한다.
        captureFrameCallback?.let { Choreographer.getInstance().removeFrameCallback(it) }
        captureFrameCallback = null
        captureService?.stopSession()
        captureService = null
        captureConnection?.let {
            try {
                unbindService(it)
            } catch (error: IllegalArgumentException) {
                Log.w("FloatingButtonService", "이미 해제된 캡처 서비스 연결", error)
            }
        }
        captureConnection = null
        // Also covers initialization/binding failure before a binder became available.
        stopService(Intent(this, MediaProjectionService::class.java))
        // 플로팅 서비스와 표시 권한이 남아 있을 때만 버튼을 복구한다.
        if (!destroyed && Settings.canDrawOverlays(this)) {
            try {
                showFloatingButton()
                floatingButton?.isEnabled = true
            } catch (error: Exception) {
                Log.w("FloatingButtonService", "플로팅 버튼 복구 실패", error)
                stopSelf()
            }
        }
        if (message != null && !destroyed) Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun removeOverlay(view: View?) {
        if (view == null || !::windowManager.isInitialized) return
        try {
            // 즉시 분리하여 빠른 재클릭 시에도 같은 View를 안전하게 재사용한다.
            windowManager.removeViewImmediate(view)
        } catch (error: IllegalArgumentException) {
            // 이미 분리된 View여도 다른 오버레이의 정리는 계속한다.
            Log.w("FloatingButtonService", "이미 제거된 오버레이입니다.", error)
        }
    }

    // Foreground Service 시작
    private fun startAsForegroundService() {

        val intent = Intent(this, MainActivity::class.java)

        // 알림을 누르면 Android가 앱 진입 화면을 열 수 있도록 실행 정보를 전달한다.
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 앱 화면 밖에서도 기능이 실행 중임을 사용자가 알 수 있게 알림을 만든다.
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("든든이 실행 중")
            .setContentText("안심 버튼을 사용할 수 있습니다.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        // 알림을 서비스에 연결하며, Android 14부터는 등록된 specialUse 유형도 전달한다.
        // Android 14 이상
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {

            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )

        } else {

            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }
    }

    // 알림 채널 생성
    private fun createNotificationChannel() {

        // 실행 상태 알림을 묶는 채널로, 사용자가 시스템 설정에서 알림을 관리할 수 있다.
        val channel = NotificationChannel(
            CHANNEL_ID,
            "든든이 Floating Button",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "든든이 안심 버튼 실행 상태 알림"
        }

        val notificationManager =
            getSystemService(NotificationManager::class.java)

        notificationManager.createNotificationChannel(channel)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        // 시스템이 Service를 종료했을 경우 가능한 범위에서 재생성 요청
        return START_STICKY
    }

    override fun onDestroy() {

        destroyed = true
        analysisHandoffScope.cancel()
        finishScreenScan()

        // 서비스 종료 후 화면에 버튼이나 메뉴가 남지 않도록 창과 참조를 정리한다.
        if (isMenuOpen) removeOverlay(floatingMenu)
        removeOverlay(floatingButton)
        isMenuOpen = false
        floatingMenu = null
        floatingButton?.setOnClickListener(null)
        floatingButton = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
