package com.example.copy_pastewisdom

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthorIdentityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun verify_confucius_identity_integrity() {
        waitForQuotes()

        // 1. Open Browser
        composeTestRule.onNodeWithText("Browse All Quotes").performClick()

        // 2. Click Confucius in the list
        composeTestRule.onNodeWithTag("authors_list").performScrollToNode(hasText("Confucius", substring = true))
        composeTestRule.onNode(hasText("Confucius", substring = true) and hasAnyAncestor(hasTestTag("authors_list"))).performClick()

        // 3. Verify Detail View
        composeTestRule.onNodeWithText("Quotes by Confucius", substring = true).assertExists()
        
        // 4. Open the "About" sheet
        composeTestRule.onNodeWithText("Tap portrait for details & photos").performClick()
        
        // 5. Verify Bio NOT fallback (checking if non-placeholder content exists)
        composeTestRule.onNodeWithText("Mystery thinker").assertDoesNotExist()
        composeTestRule.onNodeWithText("THE EXPANDED LIBRARY").assertDoesNotExist()
    }

    @Test
    fun verify_lao_tzu_identity_integrity() {
        waitForQuotes()

        // 1. Open Browser
        composeTestRule.onNodeWithText("Browse All Quotes").performClick()

        // 2. Scroll to and Click Lao Tzu
        composeTestRule.onNodeWithTag("authors_list").performScrollToNode(hasText("Lao Tzu", substring = true))
        composeTestRule.onNode(hasText("Lao Tzu", substring = true) and hasAnyAncestor(hasTestTag("authors_list"))).performClick()
        
        // 3. Open About
        composeTestRule.onNodeWithText("Tap portrait for details & photos").performClick()
        
        // 4. Verify Bio NOT fallback
        composeTestRule.onNodeWithText("Mystery thinker").assertDoesNotExist()
        composeTestRule.onNodeWithText("THE EXPANDED LIBRARY").assertDoesNotExist()
    }

    private fun waitForQuotes() {
        composeTestRule.waitUntil(timeoutMillis = 20000) {
            composeTestRule.onAllNodes(hasText("Browse All Quotes")).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
