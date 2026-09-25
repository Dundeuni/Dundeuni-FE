package com.dundueni.app.test

import android.app.Activity
import android.content.Context
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

import android.content.Intent
import androidx.core.content.ContextCompat

// MediaProjection 사용 권한 요청 테스트

class MediaProjectionTestActivity : ComponentActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager

    private val captureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK && result.data != null) {

                Log.d("MediaProjectionTest", "화면 캡처 권한 허용됨")

                val serviceIntent = Intent(
                    this,
                    MediaProjectionService::class.java
                ).apply {
                    putExtra(
                        MediaProjectionService.EXTRA_RESULT_CODE,
                        result.resultCode
                    )

                    putExtra(
                        MediaProjectionService.EXTRA_DATA,
                        result.data
                    )
                }

                ContextCompat.startForegroundService(
                    this,
                    serviceIntent
                )

            } else {

                Log.d("MediaProjectionTest", "화면 캡처 권한 거부됨")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        captureLauncher.launch(
            mediaProjectionManager.createScreenCaptureIntent()
        )
    }
}
