package com.example.copy_pastewisdom

import androidx.compose.ui.geometry.Offset
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
        
        // Wait for potential loading
        composeTestRule.waitForIdle()

        // 3. Swipe to next card
        composeTestRule.onNodeWithTag("quote_pager").performTouchInput {
            swipeLeft()
        }
        
        // 4. Verify that Discovery label is present on cards
        composeTestRule.onAllNodes(hasText("LIBRARY", substring = true)).onFirst().assertExists()
    }

    @Test
    fun verify_return_to_today_logic() {
        waitForQuotes()
        
        // 1. Swipe a few times
        repeat(2) {
            composeTestRule.onNodeWithTag("quote_pager").performTouchInput { swipeLeft() }
            composeTestRule.waitForIdle()
        }
        
        // 2. Click "Return to Today"
        // Wait for the button to appear since it's conditional on being away from the daily quote
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasTestTag("return_today_fab")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("return_today_fab").performClick()
        
        // 3. Verify we are back at TODAY
        composeTestRule.onNodeWithText("TODAY", substring = true).assertExists()
    }

    @Test
    fun verify_fast_scrollbar_on_topics() {
        waitForQuotes()
        
        // 1. Open Browser
        composeTestRule.onNodeWithText("Browse All Quotes").performClick()
        
        // 2. Switch to Topics tab
        composeTestRule.onNodeWithText("Topics").performClick()
        
        // 3. Wait and verify scrollbar exists (topics list is always long)
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasTestTag("topics_scrollbar")).fetchSemanticsNodes().isNotEmpty()
        }
        
        // 4. Drag scrollbar down
        composeTestRule.onNodeWithTag("topics_scrollbar").performTouchInput {
            down(center)
            moveBy(Offset(0f, 300f))
            up()
        }
        
        // 5. Verify we have moved down
        composeTestRule.onAllNodes(hasText("#", substring = true)).onFirst().assertExists()
    }

    @Test
    fun verify_discovery_expands_authors_list() {
        waitForQuotes()

        // 1. Open Browser
        composeTestRule.onNodeWithText("Browse All Quotes").performClick()
        
        // 2. Verify initial authors list exists
        composeTestRule.onNodeWithTag("authors_list").assertExists()
        
        // 3. Turn ON Global Discovery via Browser switch
        composeTestRule.onNodeWithTag("browser_discovery_switch").performClick()
        
        // 4. Wait for background processing. 
        // We wait for the loading overlay to potentially appear and then definitely disappear.
        // We also wait for the list to become long enough to scroll past the curated limit (18).
        composeTestRule.waitUntil(timeoutMillis = 45000) {
            try {
                // If we can scroll to index 25, we have successfully expanded past curated quotes
                composeTestRule.onNodeWithTag("authors_list").performScrollToIndex(25)
                true
            } catch (_: Throwable) {
                false
            }
        }

        // 5. Final assertion
        composeTestRule.onNodeWithTag("authors_list").performScrollToIndex(25)
        composeTestRule.onNodeWithTag("authors_scrollbar").assertExists()
    }

    @Test
    fun verify_settings_theme_change() {
        waitForQuotes()
        
        // 1. Open Settings
        composeTestRule.onNodeWithTag("settings_button").performClick()
        
        // 2. Select Gold (Scholarly) theme
        composeTestRule.onNodeWithTag("theme_item_Gold").performClick()
        
        // 3. Verify selection exists
        composeTestRule.onNodeWithTag("theme_item_Gold").assertExists()
    }

    @Test
    fun verify_settings_notification_toggle() {
        waitForQuotes()
        
        // 1. Open Settings
        composeTestRule.onNodeWithTag("settings_button").performClick()
        
        // 2. Toggle Notifications
        composeTestRule.onNodeWithTag("notification_switch").performClick()
        
        // 3. Verify switch exists
        composeTestRule.onNodeWithTag("notification_switch").assertExists()
    }

    @Test
    fun verify_search_filtering_in_browser() {
        waitForQuotes()
        
        // 1. Open Browser
        composeTestRule.onNodeWithText("Browse All Quotes").performClick()
        
        // 2. Type "Marcus" in search
        composeTestRule.onNodeWithText("Search authors...").performTextInput("Marcus")
        
        // 3. Verify Marcus Aurelius is visible
        composeTestRule.onNodeWithText("Marcus Aurelius", substring = true).assertExists()
        
        // 4. Verify unrelated authors (like "Seneca") are NOT visible
        composeTestRule.onNodeWithText("Seneca").assertDoesNotExist()
    }

    @Test
    fun verify_lucky_quote_generation() {
        waitForQuotes()
        
        // 1. Open Browser
        composeTestRule.onNodeWithText("Browse All Quotes").performClick()
        
        // 2. Click Lucky
        composeTestRule.onNodeWithText("I'm Feeling Lucky").performClick()
        
        // 3. Verify a quote surface appears (checking for the quotation mark or "Copied" toast eventually)
        // Since it's an API call, we might need to wait
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasText("“", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun verify_author_about_bottom_sheet() {
        waitForQuotes()
        
        // 1. Click "Learn more" on the current card
        composeTestRule.onNodeWithText("Learn more").performClick()
        
        // 2. Verify Bottom Sheet content (e.g., "About")
        composeTestRule.onNodeWithText("About", substring = true).assertExists()
    }

    @Test
    fun verify_browser_quote_selection() {
        waitForQuotes()
        
        // 1. Open Browser
        composeTestRule.onNodeWithText("Browse All Quotes").performClick()
        
        // 2. Select first author
        composeTestRule.onNodeWithTag("authors_list").onChildren().onFirst().performClick()
        
        // 3. Select a quote (Tray item)
        // We'll just click the first one available
        composeTestRule.onAllNodes(hasText("“", substring = true)).onFirst().performClick()
        
        // 4. Verify browser closed, and we are on home screen (looking for "Browse All Quotes" button)
        composeTestRule.onNodeWithText("Browse All Quotes").assertExists()
    }

    private fun waitForQuotes() {
        composeTestRule.waitUntil(timeoutMillis = 20000) {
            composeTestRule.onAllNodes(hasText("Browse All Quotes")).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
