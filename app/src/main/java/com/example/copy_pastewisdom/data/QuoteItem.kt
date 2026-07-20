package com.example.copy_pastewisdom.data

data class QuoteItem(
    val author: String,
    val about: String,
    val quote: String,
    val imageUrl: String? = null,
    val tags: List<String> = emptyList(),
    val priority: Int = 0
)

sealed class QuoteState {
    data object Loading : QuoteState()
    data class Success(val quotes: List<QuoteItem>) : QuoteState()
    data class Error(val message: String) : QuoteState()
}
