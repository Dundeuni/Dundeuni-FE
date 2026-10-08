package com.dundueni.app.feature.analysis

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import java.io.ByteArrayInputStream
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AnalysisActivityTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context = RuntimeEnvironment.getApplication()

    private fun assertMockResult() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("위험도: HIGH").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("위험도: HIGH").assertIsDisplayed()
        compose.onNodeWithText("Mock 결과: 개인정보 입력을 요구하는 상황을 가정합니다.").assertIsDisplayed()
        compose.onNodeWithText("Mock 안내: 공식 채널에서 내용을 확인하세요.").assertIsDisplayed()
    }

    @Test fun photoUriLaunchReachesResultAndSurvivesRecreation() {
        val uri = Uri.parse("content://test/photo/flow")
        shadowOf(context.contentResolver).registerInputStream(uri, ByteArrayInputStream(byteArrayOf(1, 2, 3)))
        ActivityScenario.launch<AnalysisActivity>(AnalysisActivity.forPhoto(context, uri)).use { scenario ->
            assertMockResult()
            scenario.recreate()
            assertMockResult()
        }
    }

    @Test fun capturedPngLaunchReachesResultAndCleansUpOnClose() {
        val file = File.createTempFile("capture_", ".png", AnalysisActivity.captureDirectory(context))
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        try {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            ActivityScenario.launch<AnalysisActivity>(AnalysisActivity.forCapture(context, file)).use {
                assertMockResult()
            }
            assertFalse(file.exists())
        } finally {
            bitmap.recycle()
            file.delete()
        }
    }
}
