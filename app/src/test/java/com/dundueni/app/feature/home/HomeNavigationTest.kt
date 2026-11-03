package com.dundueni.app.feature.home

import android.content.Intent
import androidx.activity.ComponentActivity
import com.dundueni.app.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HomeNavigationTest {
    @Test fun returnToHomeClearsScanStackAndFinishesCurrentActivity() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()

        activity.returnToHome()

        val intent = shadowOf(activity).nextStartedActivity
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
        assertTrue(activity.isFinishing)
    }
}
