package com.dundueni.app.feature.analysis

import android.content.ComponentName
import android.content.pm.PackageManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class IntegratedManifestTest {
    private val context = RuntimeEnvironment.getApplication()
    private val pm = context.packageManager

    @Test fun existingActivitiesAndBothPb05ActivitiesRemainRegistered() {
        for ((name, exported) in mapOf(
            "MainActivity" to true,
            "feature.analysis.AnalysisActivity" to false,
            "feature.photopicker.PhotoPickerActivity" to true,
            "feature.capture.MediaProjectionTestActivity" to false,
            "feature.analysisresult.AnalysisResultActivity" to false,
            "feature.analysisresult.AnalysisResultPreviewActivity" to false,
        )) {
            val info = pm.getActivityInfo(ComponentName(context.packageName, "${context.packageName}.$name"), 0)
            assertEquals(name, exported, info.exported)
        }
    }

    @Test fun foregroundServicesAndPermissionsSurviveMerge() {
        for (name in listOf("feature.capture.MediaProjectionService", "feature.floatingbutton.FloatingButtonService")) {
            val info = pm.getServiceInfo(ComponentName(context.packageName, "${context.packageName}.$name"), 0)
            assertFalse(info.exported)
        }
        val permissions = pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty().toSet()
        for (permission in listOf("INTERNET", "SYSTEM_ALERT_WINDOW", "FOREGROUND_SERVICE", "FOREGROUND_SERVICE_MEDIA_PROJECTION", "FOREGROUND_SERVICE_SPECIAL_USE")) {
            assertTrue(permission, "android.permission.$permission" in permissions)
        }
    }

    @Test fun floatingScanActivitiesUseHiddenTemporaryTasks() {
        val mainAffinity = pm.getActivityInfo(
            ComponentName(context.packageName, "${context.packageName}.MainActivity"), 0,
        ).taskAffinity
        assertNotNull(mainAffinity)
        for (name in listOf(
            "feature.photopicker.PhotoPickerActivity",
            "feature.analysis.AnalysisActivity",
            "feature.textinput.TextInputActivity",
        )) {
            val info = pm.getActivityInfo(ComponentName(context.packageName, "${context.packageName}.$name"), 0)
            assertTrue("$name should launch outside the home task", info.taskAffinity.isNullOrEmpty())
        }
        for (name in listOf(
            "feature.url.UrlInputActivity",
            "feature.phone.PhoneInputActivity",
        )) {
            val info = pm.getActivityInfo(ComponentName(context.packageName, "${context.packageName}.$name"), 0)
            assertEquals("$name should share the main task", mainAffinity, info.taskAffinity)
        }
    }
}
