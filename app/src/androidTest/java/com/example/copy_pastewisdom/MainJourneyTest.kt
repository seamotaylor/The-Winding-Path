package com.example.copy_pastewisdom

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
        waitForQuotes()
        
        // Click Browse
        composeTestRule.onNodeWithText("Browse All Quotes").performClick()

        // Verify Browser opened
        composeTestRule.onNodeWithText("Authors").assertExists()
        composeTestRule.onNodeWithText("Topics").assertExists()
    }

    @Test
    fun verify_card_labels_and_discovery_sync() {
        waitForQuotes()
        
        // 1. Initial state check - wait for cards to be ready
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodes(hasText("WISDOM", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }

        // 2. Toggle Discovery Mode
        composeTestRule.onNodeWithTag("discovery_toggle").performClick()

        // 3. Swipe to next card
        // Perform a slow swipe to ensure it triggers the pager update
        composeTestRule.onNodeWithTag("quote_pager").performTouchInput {
            swipeLeft(startX = width * 0.9f, endX = width * 0.1f, durationMillis = 500)
        }
        
        // Wait for card to settle and label to change
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodes(hasText("DISCOVERY", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }

        // 4. Open Browser and verify switch is synced
        composeTestRule.onNodeWithText("Browse All Quotes").performClick()
        
        // Ensure we are in the Authors tab where the switch is
        composeTestRule.onNodeWithText("Authors").performClick()

        // Wait for the background grouping of 30k authors to finish (Switch appears when loadingGlobalAuthors is false)
        // Increased timeout to 45 seconds for slow emulators/heavy data
        composeTestRule.waitUntil(timeoutMillis = 45000) {
            composeTestRule.onAllNodes(hasTestTag("browser_discovery_switch")).fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.onNodeWithTag("browser_discovery_switch").assertIsOn()
    }

    @Test
    fun verify_return_to_today_logic() {
        waitForQuotes()
        
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodes(hasText("WISDOM", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }

        // 1. Swipe away
        composeTestRule.onNodeWithTag("quote_pager").performTouchInput {
            swipeLeft(startX = width * 0.9f, endX = width * 0.1f, durationMillis = 500)
        }
        
        // 2. Verify return button appears
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasText("Return to Today's Wisdom")).fetchSemanticsNodes().isNotEmpty()
        }

        // 3. Click return
        composeTestRule.onNodeWithText("Return to Today's Wisdom").performClick()

        // 4. Verify button is gone
        composeTestRule.onNodeWithText("Return to Today's Wisdom").assertDoesNotExist()
    }

    @Test
    fun verify_browser_navigation_and_detail_view() {
        waitForQuotes()

        // 1. Open Browser
        composeTestRule.onNodeWithText("Browse All Quotes").performClick()

        // 2. Switch to Topics
        composeTestRule.onNodeWithText("Topics").performClick()
        
        // Wait for topics list items to load
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodes(hasText("#", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        // Verify at least one hashtag exists
        composeTestRule.onAllNodes(hasText("#", substring = true)).onFirst().assertExists()

        // 3. Switch back to Authors
        composeTestRule.onNodeWithText("Authors").performClick()

        // 4. Click Aristotle
        composeTestRule.onNodeWithText("Aristotle", substring = true).performClick()

        // 5. Verify Detail View
        composeTestRule.onNodeWithText("Quotes by Aristotle", substring = true).assertExists()
        
        // 6. Go Back and verify we stay in Browser
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("Discover").assertExists()
    }

    private fun waitForQuotes() {
        composeTestRule.waitUntil(timeoutMillis = 20000) {
            composeTestRule.onAllNodes(hasText("Browse All Quotes")).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
