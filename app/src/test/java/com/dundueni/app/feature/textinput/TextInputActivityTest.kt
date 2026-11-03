package com.dundueni.app.feature.textinput

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
class TextInputActivityTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun textInputActivityLaunchesSuccessfully() {
        val intent = Intent(context, TextInputActivity::class.java)
        ActivityScenario.launch<TextInputActivity>(intent).use {
            compose.onNodeWithText("텍스트 검사").assertIsDisplayed()
            compose.onNodeWithText("검사하기").assertIsDisplayed()
        }
    }
}
