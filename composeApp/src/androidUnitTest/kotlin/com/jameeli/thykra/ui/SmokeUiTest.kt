package com.jameeli.thykra.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Headless Compose UI smoke test. Runs on the JVM via Robolectric — no Android
 * emulator required. Verifies the test stack itself is wired correctly so we
 * can extend it to real screens in [com.jameeli.thykra.ui] piece by piece.
 *
 * Run locally: `./gradlew :composeApp:testDebugUnitTest`
 */
// Pin to API 33 because compileSdk = 36 outruns Robolectric's bundled
// android-all jars (4.14.1 ships up to API 34). Bump when Robolectric
// catches up.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-xxhdpi")
class SmokeUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun renders_static_text() {
        composeRule.setContent {
            Text(text = "Thykra UI test harness ready")
        }
        composeRule.onNodeWithText("Thykra UI test harness ready").assertIsDisplayed()
    }

    @Test
    fun button_click_updates_state() {
        composeRule.setContent { Counter() }
        composeRule.onNodeWithText("Count: 0").assertIsDisplayed()
        composeRule.onNodeWithText("Increment").performClick()
        composeRule.onNodeWithText("Count: 1").assertIsDisplayed()
    }
}

@Composable
private fun Counter() {
    var count by remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Count: $count")
        Button(onClick = { count += 1 }) {
            Text("Increment")
        }
    }
}
