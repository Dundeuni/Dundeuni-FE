package com.dundueni.app.feature.analysisresult

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.dundueni.app.ui.theme.DundueniFETheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AnalysisResultBackNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun systemBackDispatchesResultReturnAction() {
        var returned = false
        compose.setContent {
            DundueniFETheme {
                AnalysisResultRoute(
                    AnalysisResultSamples.forLevel(RiskLevel.CAUTION),
                    onReturn = { returned = true },
                    onHome = {},
                )
            }
        }

        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.runOnIdle { assertTrue(returned) }
    }

    @Test fun homeButtonDispatchesExplicitHomeAction() {
        var openedHome = false
        compose.setContent {
            DundueniFETheme {
                AnalysisResultRoute(
                    AnalysisResultSamples.forLevel(RiskLevel.CAUTION),
                    onReturn = {},
                    onHome = { openedHome = true },
                )
            }
        }

        compose.onNodeWithText("든든이 홈으로").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(openedHome) }
    }
}
