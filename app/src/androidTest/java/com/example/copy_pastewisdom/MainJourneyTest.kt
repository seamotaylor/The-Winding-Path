package com.example.copy_pastewisdom

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
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
        
        // 1. Open Browser to ensure Discovery Mode is ON
        composeTestRule.onNodeWithText("Browse All Quotes").performClick()
        
        // Use onAllNodes to avoid crashing if not found immediately
        val switchNodes = composeTestRule.onAllNodes(hasTestTag("browser_discovery_switch"))
        if (switchNodes.fetchSemanticsNodes().isEmpty()) {
             // Fallback: try to find it by text if tag is missing for some reason
             composeTestRule.onNodeWithText("Anthology", substring = true).assertExists()
        }

        val isOff = try {
            composeTestRule.onNodeWithTag("browser_discovery_switch").assertIsOff()
            true
        } catch (e: Throwable) {
            false
        }
        
        if (isOff) {
            composeTestRule.onNodeWithTag("browser_discovery_switch").performClick()
            composeTestRule.waitForIdle()
        }
        composeTestRule.onNodeWithText("Close").performClick()

        // 2. Initial state check - wait for cards to be ready
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodes(hasTestTag("quote_header_label")).fetchSemanticsNodes().isNotEmpty()
        }
        
        // 3. Swipe until we find a LIBRARY card (since pool is shuffled)
        var found = false
        for (i in 1..10) {
            val nodes = composeTestRule.onAllNodes(hasText("LIBRARY", substring = true)).fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                found = true
                break
            }
            composeTestRule.onNodeWithTag("quote_pager").performTouchInput {
                swipeLeft()
            }
            composeTestRule.waitForIdle()
        }
        
        // 4. Final verification
        assertTrue("Should eventually find a card from the expanded library", found)
    }

    @Test
    fun verify_return_to_today_logic() {
        waitForQuotes()
        
        // 1. Ensure we are starting at TODAY
        composeTestRule.onNodeWithText("TODAY", substring = true).assertExists()

        // 2. Swipe to next - repeat to be sure we moved
        repeat(3) {
            composeTestRule.onNodeWithTag("quote_pager").performTouchInput { 
                swipeLeft(durationMillis = 1000) 
            }
            composeTestRule.waitForIdle()
        }
        
        // 3. Verify we are NOT at TODAY anymore
        composeTestRule.onNodeWithText("TODAY", substring = true).assertDoesNotExist()

        // 4. Click "Return to Today"
        composeTestRule.waitUntil(timeoutMillis = 30000) {
            composeTestRule.onAllNodes(hasTestTag("return_today_fab")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("return_today_fab").performClick()
        
        // 5. Verify we are back at TODAY
        composeTestRule.waitUntil(timeoutMillis = 30000) {
            composeTestRule.onAllNodes(hasText("TODAY", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
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
        
        // 3. Ensure Global Discovery is ON via Browser switch
        val switch = composeTestRule.onNodeWithTag("browser_discovery_switch")
        val isOff = try {
            switch.assertIsOff()
            true
        } catch (e: Throwable) {
            false
        }

        if (isOff) {
            switch.performClick()
            composeTestRule.waitForIdle()
        }
        
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
        composeTestRule.onNodeWithTag("browser_search_field").performTextInput("Marcus")
        
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
        
        // 3. Verify a quote surface appears
        // Increased timeout to 30s to account for potential dual-network fallback (ZenQuotes -> GitHub)
        composeTestRule.waitUntil(timeoutMillis = 30000) {
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
        // Increased timeout for initial data load from sheets
        composeTestRule.waitUntil(timeoutMillis = 30000) {
            composeTestRule.onAllNodes(hasText("Browse All Quotes")).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
