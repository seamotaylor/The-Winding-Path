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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AuthorDisplayItem(
    val name: String,
    val quoteCount: Int,
    val imageUrl: String? = null
)

data class MainUiState(
    val quoteState: QuoteState = QuoteState.Loading,
    val isDiscoverMode: Boolean = false,
    val globalQuotes: List<QuoteItem> = emptyList(),
    val globalAuthors: List<Pair<String, Int>> = emptyList(),
    val displayAuthors: List<AuthorDisplayItem> = emptyList(),
    val isLoadingGlobal: Boolean = false,
    val isBrowsing: Boolean = false,
    val browseSelectedItem: QuoteItem? = null,
    val notificationsEnabled: Boolean = false,
    val notificationTime: Pair<Int, Int> = Pair(9, 0),
    val notifExpanded: Boolean = false
)

class MainViewModel(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _rawState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _rawState
        .map { state ->
            val displayAuthors = calculateAuthorsList(state)
            state.copy(displayAuthors = displayAuthors)
        }
        .flowOn(defaultDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    private fun updateState(update: (MainUiState) -> MainUiState) {
        _rawState.update { update(it) }
    }

    fun init(context: Context) {
        updateState { 
            it.copy(
                notificationsEnabled = QuoteRepository.isNotificationsEnabled(context),
                notificationTime = QuoteRepository.getNotificationTime(context),
                notifExpanded = QuoteRepository.isNotifExpanded(context)
            )
        }
        fetchQuotes(context)
    }

    fun fetchQuotes(context: Context) {
        viewModelScope.launch {
            QuoteRepository.clearMetadata()
            updateState { it.copy(quoteState = QuoteState.Loading) }
            val newState = try {
                val sid = "1vygt56OGsFT3w2tPp6Vqdwqon06rOS8dKxb-NSV0dHM"
                val combined = withContext(ioDispatcher) {
                    val mainDef = async { QuoteRepository.fetchRawSheetData(sid, "0") }
                    val dwylDef = async { QuoteRepository.fetchRawSheetData(sid, "964551737") }
                    val rM = mainDef.await(); val rD = dwylDef.await()
                    
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
            updateState { it.copy(quoteState = newState) }
            
            if (_rawState.value.isDiscoverMode) {
                loadGlobalContent()
            }
        }
    }

    private fun calculateAuthorsList(state: MainUiState): List<AuthorDisplayItem> {
        if (state.quoteState !is QuoteState.Success) return emptyList()
        
        val quotes = state.quoteState.quotes
        // Pre-group curated authors and normalize their names once
        val curatedCounts = quotes.asSequence()
            .filter { it.priority == 3 && it.quote.isNotBlank() }
            .groupingBy { it.author.trim() }
            .eachCount()
        
        val m = curatedCounts.toMutableMap()
        
        if (state.isDiscoverMode && state.globalAuthors.isNotEmpty()) {
            val curatedKeyLookup = m.keys.associateBy { QuoteRepository.normalizeAccents(it) }
            for (entry in state.globalAuthors) {
                val n = entry.first
                val c = entry.second
                val normN = QuoteRepository.normalizeAccents(n)
                val existingKey = curatedKeyLookup[normN]
                if (existingKey != null) {
                    m[existingKey] = (m[existingKey] ?: 0) + c
                } else {
                    m[n.trim()] = c
                }
            }
        }
        
        return m.keys.sortedBy { it.lowercase() }.map { name ->
            AuthorDisplayItem(
                name = name,
                quoteCount = m[name] ?: 0,
                imageUrl = QuoteRepository.findAuthorImage(name)
            )
        }
    }

    fun toggleDiscoverMode() {
        viewModelScope.launch {
            val currentState = _rawState.value
            if (!currentState.isDiscoverMode && currentState.globalQuotes.isEmpty()) {
                loadGlobalContent()
            } else {
                updateState { it.copy(isDiscoverMode = !it.isDiscoverMode) }
            }
        }
    }

    private fun loadGlobalContent() {
        viewModelScope.launch {
            updateState { it.copy(isLoadingGlobal = true) }
            try {
                val archive = QuoteRepository.getAllGlobalQuotes()
                val authors = QuoteRepository.getAllGlobalAuthors()
                updateState { it.copy(
                    isLoadingGlobal = false,
                    globalQuotes = archive,
                    globalAuthors = authors,
                    isDiscoverMode = archive.isNotEmpty()
                ) }
            } catch (e: Throwable) {
                Log.e("MainViewModel", "Error loading global content: ${e.message}", e)
                updateState { it.copy(isLoadingGlobal = false) }
            }
        }
    }

    fun setBrowsing(browsing: Boolean) {
        updateState { it.copy(isBrowsing = browsing) }
    }

    fun selectBrowseItem(item: QuoteItem) {
        updateState { it.copy(browseSelectedItem = item, isBrowsing = false) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        updateState { it.copy(notificationsEnabled = enabled) }
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        updateState { it.copy(notificationTime = Pair(hour, minute)) }
    }

    fun setNotifExpanded(context: Context, enabled: Boolean) {
        QuoteRepository.setNotifExpanded(context, enabled)
        updateState { it.copy(notifExpanded = enabled) }
    }
}
