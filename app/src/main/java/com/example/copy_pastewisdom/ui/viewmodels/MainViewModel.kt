package com.example.copy_pastewisdom.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.copy_pastewisdom.data.QuoteItem
import com.example.copy_pastewisdom.data.QuoteRepository
import com.example.copy_pastewisdom.data.QuoteState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val quoteState: QuoteState = QuoteState.Loading,
    val isBrowsing: Boolean = false,
    val isDiscoverMode: Boolean = false,
    val isLoadingGlobal: Boolean = false,
    val globalQuotes: List<QuoteItem> = emptyList(),
    val globalAuthors: List<Pair<String, Int>> = emptyList(),
    val displayAuthors: List<AuthorDisplayItem> = emptyList(),
    val notificationsEnabled: Boolean = false,
    val notificationTime: Pair<Int, Int> = 7 to 0,
    val notifExpanded: Boolean = false,
    val browseSelectedItem: QuoteItem? = null,
    
    // Browser State
    val searchQuery: String = "",
    val luckyQuote: QuoteItem? = null,
    val isFetchingLucky: Boolean = false,
    val selectedAuthor: String? = null,
    val selectedTopic: String? = null,
    
    // Display State
    val shuffledQuotes: List<QuoteItem> = emptyList(),
    val dailyQuoteIndex: Int = -1
)

data class AuthorDisplayItem(
    val name: String,
    val quoteCount: Int,
    val imageUrl: String?
)

class MainViewModel(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun init(context: Context) {
        _uiState.update { it.copy(
            notificationsEnabled = QuoteRepository.isNotificationsEnabled(context),
            notificationTime = QuoteRepository.getNotificationTime(context),
            notifExpanded = QuoteRepository.isNotifExpanded(context)
        ) }
        fetchQuotes(context)
    }

    fun fetchQuotes(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(quoteState = QuoteState.Loading) }
            val newState = try {
                val sid = "1vygt56OGsFT3w2tPp6Vqdwqon06rOS8dKxb-NSV0dHM"
                val combined = withContext(ioDispatcher) {
                    QuoteRepository.clearMetadata()
                    val mainDef = async { QuoteRepository.fetchRawSheetData(sid, "0") }
                    val dwylDef = async { QuoteRepository.fetchRawSheetData(sid, "964551737") }
                    val rM = mainDef.await()
                    val rD = dwylDef.await()

                    val qM = QuoteRepository.parseCsv(rM, priority = 3)
                    val qD = QuoteRepository.parseCsv(rD, priority = 2)

                    QuoteRepository.saveQuotesToCache(context, "$rM\n---TAB_BREAK---\n$rD")
                    QuoteRepository.indexMetadata(qM + qD)
                    (qM + qD).distinctBy { listOf(it.author.lowercase().trim(), it.quote.trim().lowercase(), it.imageUrl) }
                }
                if (combined.isNotEmpty()) QuoteState.Success(combined) else throw Exception("Empty")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Fetch error: ${e.message}")
                val cached = withContext(ioDispatcher) { QuoteRepository.getQuotesFromCache(context) }
                if (cached.isNotEmpty()) QuoteState.Success(cached) else QuoteState.Error("Network Error. Check your connection.")
            }
            
            _uiState.update { 
                val updatedState = it.copy(quoteState = newState)
                updatedState.copy(displayAuthors = calculateAuthorsList(updatedState))
            }
            
            if (newState is QuoteState.Success) {
                updateShuffledQuotes(context)
            }
        }
    }

    private fun getFilteredQuotes(state: MainUiState): List<QuoteItem> {
        val success = state.quoteState as? QuoteState.Success ?: return emptyList()
        val sheetQuotes = success.quotes
        return if (state.isDiscoverMode) {
            (sheetQuotes + state.globalQuotes).distinctBy { 
                listOf(it.author.lowercase().trim(), it.quote.trim().lowercase()) 
            }
        } else {
            sheetQuotes.filter { it.priority == 3 }
        }
    }

    private fun calculateAuthorsList(state: MainUiState): List<AuthorDisplayItem> {
        val quotes = getFilteredQuotes(state)
        val m = quotes.asSequence()
            .filter { it.quote.isNotBlank() }
            .groupingBy { it.author.trim() }
            .eachCount()

        return m.keys.sortedBy { it.lowercase() }.map { name ->
            AuthorDisplayItem(
                name = name,
                quoteCount = m[name] ?: 0,
                imageUrl = QuoteRepository.findAuthorImage(name)
            )
        }
    }

    private fun updateShuffledQuotes(context: Context) {
        val quotes = getFilteredQuotes(_uiState.value)
        val displayable = quotes.filter { it.quote.isNotBlank() }
        val shuffled = displayable.shuffled()
        val daily = QuoteRepository.getDailyWisdom(context)
        val dailyIdx = if (daily == null) 0
            else shuffled.indexOfFirst { it.quote.trim().equals(daily.quote.trim(), ignoreCase = true) }.let { if (it == -1) 0 else it }
            
        _uiState.update { it.copy(
            shuffledQuotes = shuffled,
            dailyQuoteIndex = dailyIdx
        ) }
    }

    fun toggleDiscoverMode(context: Context) {
        viewModelScope.launch {
            val current = _uiState.value.isDiscoverMode
            if (!current && _uiState.value.globalQuotes.isEmpty()) {
                fetchGlobalQuotes(context)
            } else {
                _uiState.update { state ->
                    val newState = state.copy(isDiscoverMode = !current)
                    newState.copy(displayAuthors = calculateAuthorsList(newState))
                }
                updateShuffledQuotes(context)
            }
        }
    }

    private fun fetchGlobalQuotes(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingGlobal = true) }
            try {
                val archive = QuoteRepository.getAllGlobalQuotes()
                val authors = QuoteRepository.getAllGlobalAuthors()
                _uiState.update { state ->
                    val newState = state.copy(
                        isLoadingGlobal = false,
                        globalQuotes = archive,
                        globalAuthors = authors,
                        isDiscoverMode = archive.isNotEmpty()
                    )
                    newState.copy(displayAuthors = calculateAuthorsList(newState))
                }
                updateShuffledQuotes(context)
            } catch (e: Throwable) {
                Log.e("MainViewModel", "Error loading global content: ${e.message}", e)
                _uiState.update { it.copy(isLoadingGlobal = false) }
            }
        }
    }

    fun setBrowsing(browsing: Boolean) {
        _uiState.update { it.copy(isBrowsing = browsing) }
        if (!browsing) {
            clearBrowserState()
        }
    }

    private fun clearBrowserState() {
        _uiState.update { it.copy(
            searchQuery = "",
            selectedAuthor = null,
            selectedTopic = null
        ) }
    }

    fun selectBrowseItem(item: QuoteItem) {
        _uiState.update { it.copy(browseSelectedItem = item, isBrowsing = false) }
        clearBrowserState()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectAuthor(author: String?) {
        _uiState.update { it.copy(selectedAuthor = author) }
    }

    fun selectTopic(topic: String?) {
        _uiState.update { it.copy(selectedTopic = topic) }
    }

    fun fetchLuckyQuote(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingLucky = true) }
            val res = QuoteRepository.fetchZenQuote()
            val finalQuote = if (res != null && res.quote != "RATE_LIMIT") {
                res
            } else {
                QuoteRepository.findRandomGlobalQuote()
            }
            _uiState.update { it.copy(luckyQuote = finalQuote, isFetchingLucky = false) }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        _uiState.update { it.copy(notificationTime = hour to minute) }
    }

    fun setNotifExpanded(context: Context, expanded: Boolean) {
        _uiState.update { it.copy(notifExpanded = expanded) }
        QuoteRepository.setNotifExpanded(context, expanded)
    }
}
