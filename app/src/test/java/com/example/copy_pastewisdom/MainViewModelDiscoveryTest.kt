package com.example.copy_pastewisdom

import android.content.Context
import android.util.Log
import com.example.copy_pastewisdom.data.QuoteItem
import com.example.copy_pastewisdom.data.QuoteRepository
import com.example.copy_pastewisdom.data.QuoteState
import com.example.copy_pastewisdom.ui.viewmodels.MainViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelDiscoveryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(QuoteRepository)
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        
        every { QuoteRepository.isNotificationsEnabled(any()) } returns false
        every { QuoteRepository.getNotificationTime(any()) } returns (9 to 0)
        every { QuoteRepository.isLibraryExpanded(any()) } returns false
        every { QuoteRepository.setLibraryExpanded(any(), any()) } just Runs
        every { QuoteRepository.normalizeAccents(any()) } answers { it.invocation.args[0].toString().lowercase().trim() }
        every { QuoteRepository.findAuthorImage(any()) } returns null
        
        viewModel = MainViewModel(ioDispatcher = testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `calculateAuthorsList should include all tiers when isDiscoverMode is true`() = runTest {
        val curatedQuote = QuoteItem("Curated Author", "", "Quote 1", priority = 3)
        val archiveQuote = QuoteItem("Archive Author", "", "Quote 2", priority = 2)
        val globalQuote = QuoteItem("Global Author", "", "Quote 3", priority = 1)
        
        // Setup initial success state with curated and archive
        val mockContext = mockk<Context>(relaxed = true)
        
        // Manual state update to avoid full fetchQuotes complexity
        // Accessing private calculateAuthorsList is hard, so we use toggleDiscoverMode which calls it
        
        coEvery { QuoteRepository.getAllGlobalQuotes() } returns listOf(globalQuote)
        coEvery { QuoteRepository.getAllGlobalAuthors() } returns listOf("Global Author" to 1)
        
        // We need to trigger a state that has quotes
        // fetchQuotes is the easiest way to set quoteState to Success
        coEvery { QuoteRepository.fetchRawSheetData(any(), "0") } returns "Author,Bio,Quote\nCurated Author,Bio,Quote 1"
        coEvery { QuoteRepository.fetchRawSheetData(any(), "964551737") } returns "Author,Bio,Quote\nArchive Author,Bio,Quote 2"
        
        every { QuoteRepository.clearMetadata() } just Runs
        every { QuoteRepository.saveQuotesToCache(any(), any()) } just Runs
        every { QuoteRepository.indexMetadata(any()) } just Runs
        every { QuoteRepository.parseCsv(any(), 3) } returns listOf(curatedQuote)
        every { QuoteRepository.parseCsv(any(), 2) } returns listOf(archiveQuote)

        viewModel.fetchQuotes(mockContext)
        
        // Initially (Discover off), only Curated Author should show
        assertEquals(1, viewModel.uiState.value.displayAuthors.size)
        assertEquals("Curated Author", viewModel.uiState.value.displayAuthors[0].name)
        
        // Toggle Discover Mode
        viewModel.toggleDiscoverMode(mockContext)
        
        // Now should have all 3
        val authors = viewModel.uiState.value.displayAuthors
        assertEquals(3, authors.size)
        val names = authors.map { it.name }.toSet()
        assertTrue(names.contains("Curated Author"))
        assertTrue(names.contains("Archive Author"))
        assertTrue(names.contains("Global Author"))

        // Verify shuffledQuotes also updated
        val shuffledPool = viewModel.uiState.value.shuffledQuotes
        assertEquals(3, shuffledPool.size)
    }

    @Test
    fun `shuffledQuotes should only contain priority 3 when Discover Mode is off`() = runTest {
        val mockContext = mockk<Context>(relaxed = true)
        val curatedQuote = QuoteItem("A", "", "Q1", priority = 3)
        val archiveQuote = QuoteItem("B", "", "Q2", priority = 2)
        
        coEvery { QuoteRepository.fetchRawSheetData(any(), any()) } returns "..."
        every { QuoteRepository.parseCsv(any(), 3) } returns listOf(curatedQuote)
        every { QuoteRepository.parseCsv(any(), 2) } returns listOf(archiveQuote)
        coEvery { QuoteRepository.getDailyWisdom(any()) } returns curatedQuote
        every { QuoteRepository.clearMetadata() } just Runs
        every { QuoteRepository.saveQuotesToCache(any(), any()) } just Runs
        every { QuoteRepository.indexMetadata(any()) } just Runs

        viewModel.fetchQuotes(mockContext)

        assertEquals(1, viewModel.uiState.value.shuffledQuotes.size)
        assertEquals("Q1", viewModel.uiState.value.shuffledQuotes[0].quote)
    }

    @Test
    fun `init should fetch global quotes if library is expanded`() = runTest {
        val mockContext = mockk<Context>(relaxed = true)
        val curatedQuote = QuoteItem("Curated Author", "", "Quote 1", priority = 3)
        val globalQuote = QuoteItem("Global Author", "", "Quote 3", priority = 1)
        
        every { QuoteRepository.isLibraryExpanded(any()) } returns true
        coEvery { QuoteRepository.getAllGlobalQuotes() } returns listOf(globalQuote)
        coEvery { QuoteRepository.getAllGlobalAuthors() } returns listOf("Global Author" to 1)
        
        // Mock fetchQuotes dependencies
        coEvery { QuoteRepository.fetchRawSheetData(any(), any()) } returns "Author,Bio,Quote\nCurated Author,Bio,Quote 1"
        every { QuoteRepository.parseCsv(any(), 3) } returns listOf(curatedQuote)
        every { QuoteRepository.parseCsv(any(), 2) } returns emptyList()
        every { QuoteRepository.clearMetadata() } just Runs
        every { QuoteRepository.saveQuotesToCache(any(), any()) } just Runs
        every { QuoteRepository.indexMetadata(any()) } just Runs
        coEvery { QuoteRepository.getDailyWisdom(any()) } returns curatedQuote

        viewModel.init(mockContext)
        
        // Advance time or wait for state updates if needed, though Unconfined should handle it
        advanceUntilIdle()
        
        assertTrue("Discover mode should be on", viewModel.uiState.value.isDiscoverMode)
        assertTrue("Global quotes should be loaded", viewModel.uiState.value.globalQuotes.contains(globalQuote))
        
        val authors = viewModel.uiState.value.displayAuthors
        val names = authors.map { it.name }.toSet()
        assertTrue("Curated author should be in display list", names.contains("Curated Author"))
        assertTrue("Global author should be in display list", names.contains("Global Author"))
    }
}
