package com.example.copy_pastewisdom

import android.content.Context
import android.content.SharedPreferences
import com.example.copy_pastewisdom.data.QuoteRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class DailyWisdomTest {

    private val context = mockk<Context>(relaxed = true)
    private val prefs = mockk<SharedPreferences>(relaxed = true)

    @Before
    fun setup() {
        every { context.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE) } returns prefs
        // Mocking KEY_CSV to return some quotes with a header. 
        // Using "cached_quotes_v2_csv" as found in the codebase.
        val csv = "Author,About,Quote,Image\nAuthor1,Bio,Quote 1,img1\nAuthor2,Bio,Quote 2,img2\nAuthor3,Bio,Quote 3,img3\nAuthor4,Bio,Quote 4,img4"
        every { prefs.getString("cached_quotes_v2_csv", null) } returns csv
        
        mockkStatic(Calendar::class)
    }

    @Test
    fun `getDailyWisdom should be stable for the same day`() {
        val calendar = mockk<Calendar>()
        every { Calendar.getInstance() } returns calendar
        every { calendar[Calendar.YEAR] } returns 2024
        every { calendar[Calendar.DAY_OF_YEAR] } returns 100

        val first = QuoteRepository.getDailyWisdom(context)
        val second = QuoteRepository.getDailyWisdom(context)

        assertNotNull("Daily wisdom should not be null", first)
        assertEquals("Daily wisdom should be identical for the same day", first, second)
    }

    @Test
    fun `getDailyWisdom should change for different days`() {
        val calendar = mockk<Calendar>()
        every { Calendar.getInstance() } returns calendar
        
        // Day 1
        every { calendar[Calendar.YEAR] } returns 2024
        every { calendar[Calendar.DAY_OF_YEAR] } returns 100
        val day1 = QuoteRepository.getDailyWisdom(context)

        // Day 2
        every { calendar[Calendar.YEAR] } returns 2024
        every { calendar[Calendar.DAY_OF_YEAR] } returns 101
        val day2 = QuoteRepository.getDailyWisdom(context)

        assertNotNull("Day 1 wisdom should not be null", day1)
        assertNotNull("Day 2 wisdom should not be null", day2)
        assertNotEquals("Daily wisdom should likely change for different days", day1, day2)
    }
}
