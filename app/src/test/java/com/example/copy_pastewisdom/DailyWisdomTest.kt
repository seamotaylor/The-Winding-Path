package com.example.copy_pastewisdom

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.copy_pastewisdom.data.QuoteItem
import com.example.copy_pastewisdom.data.QuoteRepository
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class DailyWisdomTest {

    private val context = mockk<Context>(relaxed = true)
    private val prefs = mockk<SharedPreferences>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        every { context.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.apply() } just Runs
        every { editor.commit() } returns true
        
        // Mocking KEY_CSV to return some quotes with a header.
        val csv = "Author,About,Quote,Image\nAuthor1,Bio,Quote 1,img1\nAuthor2,Bio,Quote 2,img2"
        every { prefs.getString("cached_quotes_v2_csv", null) } returns csv
        
        // Default: not cached
        every { prefs.getString("daily_quote_date", any()) } returns null
        every { prefs.getString("daily_quote_json", any()) } returns null
        
        mockkStatic(Calendar::class)
    }

    @Test
    fun `getDailyWisdom should be stable for the same day using cache`() = runBlocking {
        val calendar = mockk<Calendar>()
        every { Calendar.getInstance() } returns calendar
        every { calendar[Calendar.YEAR] } returns 2024
        every { calendar[Calendar.DAY_OF_YEAR] } returns 100

        // 1. First call - should calculate and cache
        val first = QuoteRepository.getDailyWisdom(context)
        assertNotNull(first)

        // Capture what was cached
        val dateSlot = slot<String>()
        val jsonSlot = slot<String>()
        verify { editor.putString("daily_quote_date", capture(dateSlot)) }
        verify { editor.putString("daily_quote_json", capture(jsonSlot)) }

        // 2. Mock cache hit for the second call
        every { prefs.getString("daily_quote_date", null) } returns dateSlot.captured
        every { prefs.getString("daily_quote_json", null) } returns jsonSlot.captured

        // Change the underlying CSV to prove it uses the cache
        every { prefs.getString("cached_quotes_v2_csv", null) } returns "Author,About,Quote,Image\nChanged,Bio,Changed,img"

        val second = QuoteRepository.getDailyWisdom(context)
        assertEquals("Should return the cached quote even if pool changes", first!!.quote, second!!.quote)
        assertEquals("Should return the cached author", first.author, second.author)
    }

    @Test
    fun `getDailyWisdom should respect library expansion setting`() = runBlocking {
        val calendar = mockk<Calendar>()
        every { Calendar.getInstance() } returns calendar
        every { calendar[Calendar.YEAR] } returns 2024
        every { calendar[Calendar.DAY_OF_YEAR] } returns 200

        // Mock global quotes
        mockkObject(QuoteRepository)
        // We need to keep some methods real, but mock the global fetch
        val globalQuote = QuoteItem("Global Author", "Bio", "Global Quote", priority = 1)
        coEvery { QuoteRepository.getAllGlobalQuotes() } returns listOf(globalQuote)
        
        // Force re-calculation (no cache)
        every { prefs.getString("daily_quote_date", any()) } returns null
        
        // Test Expanded = false
        every { prefs.getBoolean("library_expanded", false) } returns false
        val curated = QuoteRepository.getDailyWisdom(context)
        assertNotEquals("Global Author", curated?.author)

        // Test Expanded = true
        // We clear the cache to force a new selection for this test (or use a different day)
        every { calendar[Calendar.DAY_OF_YEAR] } returns 201
        every { prefs.getBoolean("library_expanded", false) } returns true
        
        // Since it's random, we might need a few tries or a specific seed to guarantee picking the global one,
        // but here we just want to verify the pool inclusion logic.
        // Actually, let's just check if it's possible. 
        // With a small pool (2 curated + 1 global), it should happen eventually.
        
        var foundGlobal = false
        for (i in 1..20) {
            every { calendar[Calendar.DAY_OF_YEAR] } returns 200 + i
            if (QuoteRepository.getDailyWisdom(context)?.author == "Global Author") {
                foundGlobal = true
                break
            }
        }
        assertTrue("Should eventually pick from global pool when expanded", foundGlobal)
        
        unmockkObject(QuoteRepository)
    }
}
