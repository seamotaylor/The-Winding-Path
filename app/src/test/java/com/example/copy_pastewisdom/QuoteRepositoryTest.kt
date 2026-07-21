package com.example.copy_pastewisdom

import com.example.copy_pastewisdom.data.QuoteItem
import com.example.copy_pastewisdom.data.QuoteRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class QuoteRepositoryTest {

    @Before
    fun setup() {
        QuoteRepository.clearMetadata()
    }

    @Test
    fun `indexMetadata should follow Strict Tiered Priority across calls`() {
        val tier1_Global = QuoteItem("Lao Tzu", "Global Bio", "Q1", "global.jpg", priority = 1)
        val tier2_Archive = QuoteItem("Lao Tzu", "Archive Bio", "Q2", "archive.jpg", priority = 2)
        val tier3_Main = QuoteItem("Lao Tzu", "Main Bio", "Q3", "main.jpg", priority = 3)
        
        // 1. Global arrives first
        QuoteRepository.indexMetadata(listOf(tier1_Global))
        assertEquals("global.jpg", QuoteRepository.findAuthorImage("Lao Tzu"))

        // 2. Archive arrives - should overwrite Global
        QuoteRepository.indexMetadata(listOf(tier2_Archive))
        assertEquals("archive.jpg", QuoteRepository.findAuthorImage("Lao Tzu"))

        // 3. Main arrives - should overwrite Archive
        QuoteRepository.indexMetadata(listOf(tier3_Main))
        assertEquals("main.jpg", QuoteRepository.findAuthorImage("Lao Tzu"))

        // 4. Archive arrives AGAIN (e.g. on separate fetch) - should NOT overwrite Main
        QuoteRepository.indexMetadata(listOf(tier2_Archive))
        assertEquals("main.jpg", QuoteRepository.findAuthorImage("Lao Tzu"))
    }

    @Test
    fun `indexMetadata should enforce First In Sheet order within same Tier`() {
        val tier3_First = QuoteItem("Lao Tzu", "First Bio", "Q1", "first.jpg", priority = 3)
        val tier3_Second = QuoteItem("Lao Tzu", "Second Bio", "Q2", "second.jpg", priority = 3)
        
        // Process together
        QuoteRepository.indexMetadata(listOf(tier3_First, tier3_Second))
        assertEquals("first.jpg", QuoteRepository.findAuthorImage("Lao Tzu"))
        
        // Process separately across calls
        QuoteRepository.clearMetadata()
        QuoteRepository.indexMetadata(listOf(tier3_First))
        QuoteRepository.indexMetadata(listOf(tier3_Second))
        assertEquals("first.jpg", QuoteRepository.findAuthorImage("Lao Tzu"))
    }

    @Test
    fun `indexMetadata should fill gaps by looking at other rows`() {
        val tier3_NoBio = QuoteItem("Confucius", "", "Q1", "portrait.jpg", priority = 3)
        val tier2_WithBio = QuoteItem("Confucius", "A great bio.", "Q2", "archive_portrait.jpg", priority = 2)
        
        // 1. Load Tier 3 (Main tab) which has image but NO bio
        QuoteRepository.indexMetadata(listOf(tier3_NoBio))
        assertEquals("portrait.jpg", QuoteRepository.findAuthorImage("Confucius"))
        assertNull(QuoteRepository.findAuthorAbout("Confucius"))

        // 2. Load Tier 2 (Archive tab) which HAS a bio
        QuoteRepository.indexMetadata(listOf(tier2_WithBio))
        
        // RESULT: 
        // Image stays portrait.jpg (Tier 3 > Tier 2)
        // Bio becomes "A great bio" (Tier 2 > Tier 0)
        assertEquals("portrait.jpg", QuoteRepository.findAuthorImage("Confucius"))
        assertEquals("A great bio.", QuoteRepository.findAuthorAbout("Confucius"))
    }

    @Test
    fun `indexMetadata should NOT filter out bios containing the word global if they are not the placeholder`() {
        val tier3_LaoTzu = QuoteItem("Lao Tzu", "Lao Tzu is a global icon of wisdom.", "Q1", "lao.jpg", priority = 3)
        val tier1_Global = QuoteItem("Lao Tzu", "THE EXPANDED LIBRARY", "Q2", null, priority = 1)

        QuoteRepository.indexMetadata(listOf(tier3_LaoTzu, tier1_Global))

        assertEquals("Lao Tzu is a global icon of wisdom.", QuoteRepository.findAuthorAbout("Lao Tzu"))
    }
}
