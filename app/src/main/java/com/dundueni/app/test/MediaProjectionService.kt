package com.dundueni.app.test

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
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
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat


/*
    Foreground Service 실행
        ↓
    resultCode + data 받기
        ↓
    MediaProjection 객체 생성
        ↓
    화면 크기 확인
        ↓
    ImageReader 생성
        ↓
    ImageReader 리스너 등록
        ↓
    VirtualDisplay 생성
        ↓
    MediaProjection
        ↓
    VirtualDisplay
        ↓
    ImageReader.surface
        ↓
    실제 화면 Image 수신
        ↓
    Image → Bitmap 변환
*/


class MediaProjectionService : Service() {

    companion object {

        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"

        private const val CHANNEL_ID = "media_projection_channel"
        private const val NOTIFICATION_ID = 1001

        private const val TAG = "MediaProjectionTest"
    }


    // 실제 화면 캡처 세션
    private var mediaProjection: MediaProjection? = null

    // 화면 이미지를 받을 객체
    private var imageReader: ImageReader? = null

    // 가상 화면
    private var virtualDisplay: VirtualDisplay? = null

    // 테스트 로그가 계속 도배되는 것을 방지
    private var firstImageReceived = false


    /*
        MediaProjection이 시스템에 의해 종료될 때 호출
    */
    private val mediaProjectionCallback =
        object : MediaProjection.Callback() {

            override fun onStop() {
                super.onStop()

                Log.d(
                    TAG,
                    "MediaProjection 종료됨"
                )

                virtualDisplay?.release()
                virtualDisplay = null

                imageReader?.close()
                imageReader = null

                mediaProjection = null
            }
        }


    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
    }


    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        /*
            1. Foreground Service 알림 생성
        */

        val notification =
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setContentTitle("화면 캡처 테스트")
                .setContentText("화면 캡처 기능 실행 중")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build()


        /*
            2. Foreground Service 시작
        */

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )

        } else {

            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }


        /*
            3. Activity에서 전달받은
               MediaProjection 권한 결과 확인
        */

        val resultCode =
            intent?.getIntExtra(
                EXTRA_RESULT_CODE,
                Activity.RESULT_CANCELED
            ) ?: Activity.RESULT_CANCELED


        @Suppress("DEPRECATION")
        val data =
            intent?.getParcelableExtra<Intent>(
                EXTRA_DATA
            )


        /*
            4. 사용자가 화면 캡처를 허용했는지 확인
        */

        if (
            resultCode == Activity.RESULT_OK &&
            data != null
        ) {

            /*
                5. MediaProjectionManager 가져오기
            */

            val projectionManager =
                getSystemService(
                    Context.MEDIA_PROJECTION_SERVICE
                ) as MediaProjectionManager


            /*
                6. MediaProjection 객체 생성
            */

            mediaProjection =
                projectionManager.getMediaProjection(
                    resultCode,
                    data
                )


            Log.d(
                TAG,
                "MediaProjection 객체 획득 성공"
            )


            /*
                7. MediaProjection 종료 Callback 등록
            */

            mediaProjection?.registerCallback(
                mediaProjectionCallback,
                Handler(Looper.getMainLooper())
            )


            /*
                8. 실제 화면 크기 가져오기
            */

            val windowManager =
                getSystemService(
                    Context.WINDOW_SERVICE
                ) as WindowManager


            val metrics = DisplayMetrics()


            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(
                metrics
            )


            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val densityDpi = metrics.densityDpi


            Log.d(
                TAG,
                "화면 정보: ${width}x${height}, densityDpi=$densityDpi"
            )


            /*
                9. ImageReader 생성

                VirtualDisplay에서 전달되는
                실제 화면 픽셀을 받을 객체
            */

            imageReader =
                ImageReader.newInstance(
                    width,
                    height,
                    PixelFormat.RGBA_8888,
                    2
                )


            Log.d(
                TAG,
                "ImageReader 생성 성공: ${width}x${height}"
            )


            /*
                10. ImageReader 리스너 등록

                실제 화면 이미지가 들어오면
                이 코드가 실행된다.
            */

            imageReader?.setOnImageAvailableListener(
                { reader ->

                    /*
                        가장 최신 화면 이미지 하나 가져오기
                    */

                    val image =
                        reader.acquireLatestImage()


                    if (image != null) {

                        try {

                            /*
                                현재는 테스트 단계이므로
                                첫 번째 이미지 하나만 Bitmap으로 변환한다.
                            */

                            if (!firstImageReceived) {

                                Log.d(
                                    TAG,
                                    "화면 이미지 수신 성공: ${image.width}x${image.height}"
                                )


                                /*
                                    Image → Bitmap 변환
                                */

                                val bitmap =
                                    imageToBitmap(image)


                                Log.d(
                                    TAG,
                                    "Bitmap 변환 성공: ${bitmap.width}x${bitmap.height}"
                                )


                                /*
                                    현재는 변환 성공 여부만 테스트.

                                    다음 단계에서는 이 Bitmap을
                                    PNG/JPEG 파일로 저장해서
                                    실제 캡처 화면을 눈으로 확인한다.
                                */

                                bitmap.recycle()

                                firstImageReceived = true
                            }

                        } catch (e: Exception) {

                            Log.e(
                                TAG,
                                "Bitmap 변환 실패",
                                e
                            )

                        } finally {

                            /*
                                Image는 반드시 close 해야 함.

                                안 닫으면 ImageReader가 가득 차서
                                다음 화면을 받을 수 없게 된다.
                            */

                            image.close()
                        }
                    }
                },

                Handler(
                    Looper.getMainLooper()
                )
            )


            /*
                11. VirtualDisplay 생성

                핵심 연결:

                실제 화면
                    ↓
                MediaProjection
                    ↓
                VirtualDisplay
                    ↓
                ImageReader.surface
            */

            virtualDisplay =
                mediaProjection?.createVirtualDisplay(
                    "ScreenCapture",
                    width,
                    height,
                    densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader?.surface,
                    null,
                    null
                )


            /*
                VirtualDisplay 생성 결과 확인
            */

            if (virtualDisplay != null) {

                Log.d(
                    TAG,
                    "VirtualDisplay 생성 성공"
                )

            } else {

                Log.e(
                    TAG,
                    "VirtualDisplay 생성 실패"
                )
            }


        } else {

            Log.d(
                TAG,
                "MediaProjection 권한 획득 실패"
            )
        }


        return START_NOT_STICKY
    }


    /*
        ImageReader에서 받은 Image를
        Android Bitmap으로 변환한다.

        ImageReader의 한 줄(row)은
        실제 화면 width보다 padding이 포함될 수 있기 때문에
        rowStride / pixelStride를 이용해서 처리한다.
    */
    private fun imageToBitmap(
        image: Image
    ): Bitmap {

        val plane =
            image.planes[0]

        val buffer =
            plane.buffer

        val pixelStride =
            plane.pixelStride

        val rowStride =
            plane.rowStride

        val rowPadding =
            rowStride - pixelStride * image.width


        /*
            padding까지 포함된 임시 Bitmap 생성
        */

        val paddedBitmap =
            Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )


        /*
            Image의 픽셀 데이터를 Bitmap에 복사
        */

        paddedBitmap.copyPixelsFromBuffer(
            buffer
        )


        /*
            실제 화면 크기만 잘라서 최종 Bitmap 생성
        */

        val finalBitmap =
            Bitmap.createBitmap(
                paddedBitmap,
                0,
                0,
                image.width,
                image.height
            )


        /*
            임시 Bitmap 메모리 해제
        */

        if (paddedBitmap != finalBitmap) {
            paddedBitmap.recycle()
        }


        return finalBitmap
    }


    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }


    override fun onDestroy() {

        /*
            VirtualDisplay 정리
        */

        virtualDisplay?.release()
        virtualDisplay = null


        /*
            ImageReader 정리
        */

        imageReader?.close()
        imageReader = null


        /*
            MediaProjection 정리
        */

        mediaProjection?.unregisterCallback(
            mediaProjectionCallback
        )

        mediaProjection?.stop()
        mediaProjection = null


        Log.d(
            TAG,
            "MediaProjectionService 종료"
        )


        super.onDestroy()
    }


    /*
        Foreground Service용
        NotificationChannel 생성
    */

    private fun createNotificationChannel() {

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "화면 캡처",
                NotificationManager.IMPORTANCE_LOW
            )


        val manager =
            getSystemService(
                NotificationManager::class.java
            )


        manager.createNotificationChannel(
            channel
        )
    }
}