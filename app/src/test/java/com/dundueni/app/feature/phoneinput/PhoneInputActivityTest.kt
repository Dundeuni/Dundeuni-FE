package com.dundueni.app.feature.phoneinput

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PhoneInputActivityTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun phoneInputActivityLaunchesSuccessfully() {
        val intent = Intent(context, PhoneInputActivity::class.java)
        ActivityScenario.launch<PhoneInputActivity>(intent).use {
            compose.onNodeWithText("전화번호 검사").assertIsDisplayed()
            compose.onNodeWithText("조회하기").assertIsDisplayed()
        }
    }
}
