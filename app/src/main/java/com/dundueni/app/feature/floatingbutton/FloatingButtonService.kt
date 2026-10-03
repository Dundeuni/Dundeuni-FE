package com.dundueni.app.feature.floatingbutton

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.dundueni.app.MainActivity
import com.dundueni.app.R

class FloatingButtonService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingButton: View? = null

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

        floatingButton = FloatingButtonView.create(this)

        // 72dp → px 변환
        val buttonSize = (72 * resources.displayMetrics.density).toInt()

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

        windowManager.addView(floatingButton, params)
    }

    // Foreground Service 시작
    private fun startAsForegroundService() {

        val intent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("든든이 실행 중")
            .setContentText("안심 버튼을 사용할 수 있습니다.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

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

        // Floating Button 제거
        floatingButton?.let { view ->

            if (::windowManager.isInitialized) {
                windowManager.removeView(view)
            }
        }

        floatingButton = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}