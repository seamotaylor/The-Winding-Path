package com.example.copy_pastewisdom.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.copy_pastewisdom.data.QuoteItem
import com.example.copy_pastewisdom.data.QuoteRepository
import com.example.copy_pastewisdom.data.QuoteState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

data class MainUiState(
    val quoteState: QuoteState = QuoteState.Loading,
    val isDiscoverMode: Boolean = false,
    val globalQuotes: List<QuoteItem> = emptyList(),
    val globalAuthors: List<Pair<String, Int>> = emptyList(),
    val isLoadingGlobal: Boolean = false,
    val isBrowsing: Boolean = false,
    val browseSelectedItem: QuoteItem? = null,
    val notificationsEnabled: Boolean = false,
    val notificationTime: Pair<Int, Int> = Pair(8, 0)
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun init(context: Context) {
        _uiState.update { 
            it.copy(
                notificationsEnabled = QuoteRepository.isNotificationsEnabled(context),
                notificationTime = QuoteRepository.getNotificationTime(context)
            )
        }
        fetchQuotes(context)
    }

    fun fetchQuotes(context: Context) {
        viewModelScope.launch {
            QuoteRepository.clearMetadata()
            _uiState.update { it.copy(quoteState = QuoteState.Loading) }
            val newState = try {
                val ts = System.currentTimeMillis()
                val sid = "1vygt56OGsFT3w2tPp6Vqdwqon06rOS8dKxb-NSV0dHM"
                val baseUrl = "https://docs.google.com/spreadsheets/d/$sid/export?format=csv&gid="
                val combined = withContext(Dispatchers.IO) {
                    val mainDef = async { URL("${baseUrl}0&t=$ts").readText() }
                    val dwylDef = async { URL("${baseUrl}964551737&t=$ts").readText() }
                    val rM = mainDef.await(); val rD = dwylDef.await()
                    
                    val qM = QuoteRepository.parseCsv(rM, priority = 3)
                    val qD = QuoteRepository.parseCsv(rD, priority = 2)
                    
                    QuoteRepository.saveQuotesToCache(context, "$rM\n---TAB_BREAK---\n$rD")
                    // Priority Order: Main tab first ensures primary choices are seen first by the metadata engine
                    QuoteRepository.indexMetadata(qM + qD)
                    (qM + qD).distinctBy { listOf(it.author.lowercase().trim(), it.quote.trim().lowercase(), it.imageUrl) }
                }
                if (combined.isNotEmpty()) QuoteState.Success(combined) else throw Exception("Empty")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Fetch error: ${e.message}")
                val cached = withContext(Dispatchers.IO) { QuoteRepository.getQuotesFromCache(context) }
                if (cached.isNotEmpty()) QuoteState.Success(cached) else QuoteState.Error("Network Error. Check your connection.")
            }
            _uiState.update { it.copy(quoteState = newState) }
        }
    }

    fun toggleDiscoverMode() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (!currentState.isDiscoverMode && currentState.globalQuotes.isEmpty()) {
                _uiState.update { it.copy(isLoadingGlobal = true) }
                val (archive, authors) = try {
                    val quotes = QuoteRepository.getAllGlobalQuotes()
                    val grouped = QuoteRepository.getAllGlobalAuthors() // Pre-group for the browser
                    Pair(quotes, grouped)
                } catch (e: Exception) {
                    Pair(emptyList(), emptyList())
                }
                _uiState.update { it.copy(
                    isLoadingGlobal = false,
                    globalQuotes = archive,
                    globalAuthors = authors,
                    isDiscoverMode = archive.isNotEmpty()
                ) }
            } else {
                _uiState.update { it.copy(isDiscoverMode = !it.isDiscoverMode) }
            }
        }
    }

    fun setBrowsing(browsing: Boolean) {
        _uiState.update { it.copy(isBrowsing = browsing) }
    }

    fun selectBrowseItem(item: QuoteItem) {
        _uiState.update { it.copy(browseSelectedItem = item, isBrowsing = false) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        _uiState.update { it.copy(notificationTime = Pair(hour, minute)) }
    }
}
