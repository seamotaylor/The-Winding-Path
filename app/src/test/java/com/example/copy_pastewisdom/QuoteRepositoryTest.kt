package com.example.copy_pastewisdom

import com.example.copy_pastewisdom.data.QuoteItem
import com.example.copy_pastewisdom.data.QuoteRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class QuoteRepositoryTest {

    @Test
    fun `parseCsv should handle commas inside quotes`() {
        val csvData = """
            Author,About,Quote,Image URL
            "Aristotle","Greek","Educating the mind, and heart.","http://img.jpg"
        """.trimIndent()

        val result = QuoteRepository.parseCsv(csvData)

        assertEquals(1, result.size)
        assertEquals("Aristotle", result[0].author)
        assertEquals("Educating the mind, and heart.", result[0].quote)
    }

    @Test
    fun `parseCsv should skip invalid rows`() {
        val csvData = """
            Author,About,Quote,Image URL
            ,, ,
            "OnlyAuthor",,,
        """.trimIndent()

        val result = QuoteRepository.parseCsv(csvData)

        // It filters for non-blank author AND (non-blank quote OR non-blank image)
        assertEquals(0, result.size)
    }

    @Test
    fun `normalizeAccents should remove diacritics and lowercase`() {
        assertEquals("aristotle", QuoteRepository.normalizeAccents("Aristotle"))
        assertEquals("confucius", QuoteRepository.normalizeAccents("Confucius "))
        assertEquals("muller", QuoteRepository.normalizeAccents("Müller"))
        assertEquals("seneca", QuoteRepository.normalizeAccents("Sénëca"))
    }

    @Test
    fun `getInitials should extract up to 3 uppercase letters`() {
        assertEquals("A", QuoteRepository.getInitials("Aristotle"))
        assertEquals("MT", QuoteRepository.getInitials("Marcus Tullius"))
        assertEquals("MAA", QuoteRepository.getInitials("Marcus Aurelius Antoninus"))
        assertEquals("?", QuoteRepository.getInitials("123"))
        assertEquals("?", QuoteRepository.getInitials(""))
    }

    @Test
    fun `indexMetadata should let curated images win over archive`() {
        val archiveItem = QuoteItem("Seneca", "ARCHIVE", "Quote 1", "archive_img.jpg")
        val curatedItem = QuoteItem("Seneca", "Bio", "Quote 2", "curated_img.jpg")

        // In the app, quotes are combined as (curated + archive)
        val allQuotes = listOf(curatedItem, archiveItem)
        
        QuoteRepository.indexMetadata(allQuotes)
        
        val winner = QuoteRepository.findAuthorImage("Seneca", allQuotes)
        assertEquals("curated_img.jpg", winner)
    }

    @Test
    fun `indexMetadata should pick FIRST image from curated list`() {
        val firstCurated = QuoteItem("Aristotle", "Bio", "Quote 1", "img_1.jpg")
        val secondCurated = QuoteItem("Aristotle", "", "Quote 2", "img_2.jpg")
        
        // Items are processed archive first, then curated in REVERSE order.
        // So processed: reversed([1, 2]) -> 2 then 1. 1 wins.
        val allQuotes = listOf(firstCurated, secondCurated)
        
        QuoteRepository.indexMetadata(allQuotes)
        
        val winner = QuoteRepository.findAuthorImage("Aristotle", allQuotes)
        assertEquals("img_1.jpg", winner)
    }
}
