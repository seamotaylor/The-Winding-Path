package com.example.copy_pastewisdom

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainJourneyTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun app_launches_and_opens_browser() {
        // 1. Wait for quotes to load (Check for button text)
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Browse All Quotes")).fetchSemanticsNodes().isNotEmpty()
        }

        // 2. Click Browse
        composeTestRule.onNodeWithText("Browse All Quotes").performClick()

        // 3. Verify Browser opened (Check for tab text)
        composeTestRule.onNodeWithText("Authors").assertExists()
        composeTestRule.onNodeWithText("Topics").assertExists()
    }
}
