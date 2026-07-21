package com.example.copy_pastewisdom

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

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
        
        // Stub mandatory calls for MainViewModel initialization and flow
        every { QuoteRepository.isNotificationsEnabled(any()) } returns false
        every { QuoteRepository.getNotificationTime(any()) } returns Pair(9, 0)
        every { QuoteRepository.clearMetadata() } just Runs
        every { QuoteRepository.parseCsv(any(), any()) } answers {
            val raw = it.invocation.args[0] as String
            val priority = it.invocation.args[1] as Int
            // Simplified parsing for tests
            raw.lineSequence().drop(1).map { line ->
                val parts = line.split(",")
                QuoteItem(
                    author = parts.getOrNull(0) ?: "",
                    about = parts.getOrNull(1) ?: "",
                    quote = parts.getOrNull(2) ?: "",
                    priority = priority
                )
            }.toList()
        }
        every { QuoteRepository.saveQuotesToCache(any(), any()) } just Runs
        every { QuoteRepository.indexMetadata(any()) } just Runs
        
        coEvery { QuoteRepository.getQuotesFromCache(any()) } returns emptyList()
        viewModel = MainViewModel(ioDispatcher = testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `init should fetch quotes and clear metadata`() = runTest {
        val mockContext = mockk<android.content.Context>(relaxed = true)
        coEvery { QuoteRepository.fetchRawSheetData(any(), any()) } returns "Author,Bio,Quote\nA,B,Q"
        
        viewModel.init(mockContext)
        
        // Use coVerify if the call is within a coroutine
        verify { QuoteRepository.clearMetadata() }
        assertTrue(viewModel.uiState.value.quoteState is QuoteState.Success)
    }

    @Test
    fun `fetchQuotes should emit Error state if network fails and cache empty`() = runTest {
        val mockContext = mockk<android.content.Context>(relaxed = true)
        coEvery { QuoteRepository.fetchRawSheetData(any(), any()) } throws Exception("Network down")
        coEvery { QuoteRepository.getQuotesFromCache(any()) } returns emptyList()
        
        viewModel.fetchQuotes(mockContext)
        
        assertTrue(viewModel.uiState.value.quoteState is QuoteState.Error)
    }
}
