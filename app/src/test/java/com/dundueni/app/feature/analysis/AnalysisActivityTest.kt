package com.dundueni.app.feature.analysis

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
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
            compose.onAllNodesWithText("조금 더 확인해보세요").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("조금 더 확인해보세요").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("디자인 미리보기 · 샘플 데이터").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("분석 근거 1").performScrollTo().assertIsDisplayed()
    }

    @Test fun photoUriLaunchReachesResultAndSurvivesRecreation() {
        val uri = Uri.parse("content://test/photo/flow")
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val bytes = try {
            java.io.ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
        shadowOf(context.contentResolver).registerInputStream(uri, ByteArrayInputStream(bytes))
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
