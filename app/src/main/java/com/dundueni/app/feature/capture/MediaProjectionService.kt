package com.dundueni.app.feature.capture

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.annotation.MainThread
import androidx.core.app.NotificationCompat
import com.dundueni.app.data.model.ImageInput

/** Session operations and callbacks are serialized on the main thread. */
/**
 * 사용자 승인 결과로 화면 공유 세션을 만들고 캡처 요청에 Bitmap을 돌려준다.
 * Activity가 닫혀도 캡처를 이어 가도록 알림을 표시하는 Foreground Service로 동작한다.
 * MediaProjection은 승인된 화면 공유를 관리하는 Android 객체다.
 * VirtualDisplay는 화면을 복제해 ImageReader로 보내는 가상 화면이다.
 * ImageReader가 받은 프레임을 Bitmap으로 바꿔 플로팅 서비스에 전달한다.
 * 한 장을 받은 뒤 세션을 끝낼지는 호출자가 결정하고, 이 서비스는 자원을 정리한다.
 */
class MediaProjectionService : Service() {
    enum class SessionState { INACTIVE, STARTING, ACTIVE, STOPPING }

    companion object {
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"
        private const val CHANNEL_ID = "media_projection_channel"
        private const val NOTIFICATION_ID = 1002
        private const val TAG = "MediaProjectionService"
        private const val CAPTURE_TIMEOUT_MS = 5_000L
        private const val CLIENT_CONNECT_TIMEOUT_MS = 15_000L
    }

    var sessionState = SessionState.INACTIVE
        private set
    private val handler = Handler(Looper.getMainLooper())
    private var mediaProjection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var pendingCapture: ((Result<ImageInput.ScreenCapture>) -> Unit)? = null
    private var width = 0
    private var height = 0
    private var densityDpi = 0
    // Never consume consent twice, even if this bound instance outlives stopSelf().
    private var sessionStarted = false
    private val captureTimeout = Runnable {
        completeCapture(Result.failure(IllegalStateException("새 화면 프레임 수신 시간 초과")))
    }
    // Startup handoff watchdog only; not a session idle timeout.
    private val clientConnectTimeout = Runnable { stopSession() }

    // 같은 앱의 플로팅 서비스가 연결 후 캡처 요청을 직접 보낼 수 있게 한다.
    inner class LocalBinder : Binder() {
        val service: MediaProjectionService get() = this@MediaProjectionService
    }

    override fun onBind(intent: Intent?): IBinder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "화면 캡처", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (sessionStarted) return START_NOT_STICKY
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        @Suppress("DEPRECATION")
        val data = intent?.getParcelableExtra<Intent>(EXTRA_DATA)
        intent?.removeExtra(EXTRA_DATA)
        if (resultCode != Activity.RESULT_OK || data == null) {
            stopSession()
            return START_NOT_STICKY
        }
        // 승인 결과가 있는 경우에만 세션을 시작하며, 종료 후 자동 재시작은 요청하지 않는다.
        startSession(resultCode, data)
        return START_NOT_STICKY
    }

    @MainThread
    fun startSession(resultCode: Int, resultData: Intent) {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (sessionStarted || sessionState != SessionState.INACTIVE) return
        sessionStarted = true
        sessionState = SessionState.STARTING
        try {
            // 화면 공유 중임을 알리고, 시스템이 요구하는 Foreground Service 상태로 전환한다.
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("화면 검사")
                .setContentText("화면을 캡처하고 있습니다.")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setOngoing(true)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            val projection = requireNotNull(
                getSystemService(MediaProjectionManager::class.java)
                    .getMediaProjection(resultCode, resultData)
            )
            mediaProjection = projection
            // 시스템의 공유 중단과 화면 크기 변경을 받아 세션 상태를 맞춘다.
            val callback = object : MediaProjection.Callback() {
                override fun onStop() {
                    if (mediaProjection === projection) stopSession()
                }

                override fun onCapturedContentResize(newWidth: Int, newHeight: Int) {
                    if (mediaProjection !== projection || newWidth <= 0 || newHeight <= 0) return
                    if (width == newWidth && height == newHeight) return
                    width = newWidth
                    height = newHeight
                    try {
                        virtualDisplay?.resize(width, height, densityDpi)
                        replaceReader()
                    } catch (error: Exception) {
                        Log.e(TAG, "캡처 크기 변경 실패", error)
                        stopSession()
                    }
                }
            }
            projectionCallback = callback
            projection.registerCallback(callback, handler)
            updateDisplaySize()
            // 화면 프레임을 받을 ImageReader를 만들고 가상 화면의 출력에 연결한다.
            imageReader = newReader()
            virtualDisplay = requireNotNull(projection.createVirtualDisplay(
                "ScreenCapture", width, height, densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader!!.surface, null, handler
            ))
            sessionState = SessionState.ACTIVE
            handler.postDelayed(clientConnectTimeout, CLIENT_CONNECT_TIMEOUT_MS)
        } catch (error: Exception) {
            Log.e(TAG, "화면 캡처 세션 시작 실패", error)
            stopSession()
        }
    }

    /** Caller hides its UI before this call. Success never ends the session. */
    @MainThread
    fun requestCapture(callback: (Result<ImageInput.ScreenCapture>) -> Unit) {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (sessionState != SessionState.ACTIVE || pendingCapture != null) {
            callback(Result.failure(IllegalStateException("캡처 가능한 세션이 없거나 요청 처리 중입니다.")))
            return
        }
        handler.removeCallbacks(clientConnectTimeout)
        // 한 번의 캡처 결과를 받을 곳을 보관하고, 새 프레임이 오지 않으면 시간 초과로 끝낸다.
        pendingCapture = callback
        handler.postDelayed(captureTimeout, CAPTURE_TIMEOUT_MS)
        try {
            // A new buffer queue cannot contain frames produced before this request.
            // Reuse the SAME VirtualDisplay, without consuming consent again.
            updateDisplaySize()
            virtualDisplay!!.resize(width, height, densityDpi)
            replaceReader()
        } catch (error: Exception) {
            completeCapture(Result.failure(error))
        }
    }

    private fun updateDisplaySize() {
        val manager = getSystemService(WindowManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = manager.maximumWindowMetrics.bounds
            width = bounds.width()
            height = bounds.height()
            densityDpi = resources.configuration.densityDpi
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            manager.defaultDisplay.getRealMetrics(metrics)
            width = metrics.widthPixels
            height = metrics.heightPixels
            densityDpi = metrics.densityDpi
        }
    }

    private fun newReader(): ImageReader =
        ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2).apply {
            setOnImageAvailableListener({ reader ->
                if (reader === imageReader && sessionState == SessionState.ACTIVE) {
                    receiveImage(reader)
                }
            }, handler)
        }

    // 이전 프레임이 섞이지 않도록 수신기를 교체하되 가상 화면은 재사용한다.
    private fun replaceReader() {
        virtualDisplay?.surface = null
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null
        imageReader = newReader()
        virtualDisplay?.surface = imageReader!!.surface
    }

    private fun receiveImage(reader: ImageReader) {
        val result = try {
            // 가장 최근 프레임을 꺼내고, 대기 중인 캡처 요청이 있을 때만 Bitmap으로 변환한다.
            val image = reader.acquireLatestImage() ?: return
            // Close before callback: caller may synchronously stopSession().
            image.use {
                if (pendingCapture == null) return
                Result.success(ImageInput.ScreenCapture(imageToBitmap(it)))
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
        completeCapture(result)
    }

    // 대기 요청은 한 번만 완료하고, 받을 곳이 없어진 Bitmap은 메모리에서 해제한다.
    private fun completeCapture(result: Result<ImageInput.ScreenCapture>) {
        val callback = pendingCapture
        pendingCapture = null
        handler.removeCallbacks(captureTimeout)
        if (callback != null) callback(result)
        else result.getOrNull()?.bitmap?.recycle()
    }

    @MainThread
    fun stopSession() {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (sessionState == SessionState.STOPPING) return
        sessionState = SessionState.STOPPING
        handler.removeCallbacks(clientConnectTimeout)
        handler.removeCallbacks(captureTimeout)
        val callback = pendingCapture
        pendingCapture = null
        val projection = mediaProjection
        val projectionListener = projectionCallback
        val display = virtualDisplay
        val reader = imageReader
        mediaProjection = null
        projectionCallback = null
        virtualDisplay = null
        imageReader = null
        // Continue cleanup even if one platform resource is already invalidated.
        // 수신 중단 → 가상 화면 해제 → 수신기 닫기 → 공유 종료 → 알림 제거 순으로 정리한다.
        cleanup { reader?.setOnImageAvailableListener(null, null) }
        cleanup { display?.release() }
        cleanup { reader?.close() }
        cleanup { if (projectionListener != null) projection?.unregisterCallback(projectionListener) }
        cleanup { projection?.stop() }
        cleanup { stopForeground(STOP_FOREGROUND_REMOVE) }
        sessionState = SessionState.INACTIVE
        stopSelf()
        // 종료 시 대기 중이던 요청에도 실패를 알려 호출자가 계속 기다리지 않게 한다.
        callback?.invoke(Result.failure(IllegalStateException("화면 공유가 종료되었습니다.")))
    }

    private inline fun cleanup(action: () -> Unit) {
        try { action() } catch (error: Exception) { Log.w(TAG, "캡처 자원 정리", error) }
    }

    // Existing rowStride / pixelStride padding conversion retained.
    private fun imageToBitmap(image: Image): Bitmap {
        // 프레임의 행 끝 여백까지 읽은 뒤 실제 화면 크기만 잘라 Bitmap으로 만든다.
        val plane = image.planes[0]
        val rowPadding = plane.rowStride - plane.pixelStride * image.width
        val paddedBitmap = Bitmap.createBitmap(
            image.width + rowPadding / plane.pixelStride, image.height, Bitmap.Config.ARGB_8888
        )
        var finalBitmap: Bitmap? = null
        try {
            paddedBitmap.copyPixelsFromBuffer(plane.buffer)
            finalBitmap = Bitmap.createBitmap(paddedBitmap, 0, 0, image.width, image.height)
            return finalBitmap
        } finally {
            if (paddedBitmap !== finalBitmap) paddedBitmap.recycle()
        }
    }

    override fun onDestroy() {
        stopSession()
        super.onDestroy()
    }
}
