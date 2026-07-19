package com.example.copy_pastewisdom.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.text.Normalizer
import java.util.regex.Pattern

object QuoteRepository {
    private const val PREFS_NAME = "quote_prefs"
    private const val KEY_CSV = "cached_quotes_csv"
    private const val KEY_NOTIFS_ENABLED = "notifs_enabled"
    private const val KEY_NOTIF_HOUR = "notif_hour"
    private const val KEY_NOTIF_MINUTE = "notif_minute"
    private const val KEY_THEME = "app_theme"

    private val authorMetadata = mutableMapOf<String, AuthorMetadata>()

    data class AuthorMetadata(val imageUrl: String? = null, val about: String? = null)

    fun getQuotesFromCache(context: Context): List<QuoteItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val data = prefs.getString(KEY_CSV, null) ?: return emptyList()
        val list = if (data.contains("---TAB_BREAK---")) data.split("---TAB_BREAK---").flatMap { parseCsv(it) } else parseCsv(data)
        indexMetadata(list)
        return list
    }

    fun saveQuotesToCache(context: Context, rawData: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { putString(KEY_CSV, rawData) }
    }

    fun indexMetadata(quotes: List<QuoteItem>) {
        val archiveItems = quotes.filter { it.about.contains("ARCHIVE", true) }
        val curatedItems = quotes.filter { !it.about.contains("ARCHIVE", true) }.reversed()
        
        (archiveItems + curatedItems).forEach { item ->
            val norm = normalizeAccents(item.author)
            val ex = authorMetadata[norm]
            if (!item.imageUrl.isNullOrBlank() || item.about.isNotBlank()) {
                authorMetadata[norm] = AuthorMetadata(
                    imageUrl = if (!item.imageUrl.isNullOrBlank()) item.imageUrl else ex?.imageUrl,
                    about = if (item.about.isNotBlank() && !item.about.contains("ARCHIVE", true)) item.about else ex?.about
                )
            }
        }
    }

    fun parseCsv(rawData: String): List<QuoteItem> {
        return rawData.lineSequence().drop(1).filter { it.contains(",") }.map { line ->
            val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
            val rawUrl = parts.getOrNull(3)?.trim()?.removeSurrounding("\"")
            QuoteItem(
                author = parts.getOrNull(0)?.trim()?.removeSurrounding("\"") ?: "Unknown",
                about = parts.getOrNull(1)?.trim()?.removeSurrounding("\"") ?: "",
                quote = parts.getOrNull(2)?.trim()?.removeSurrounding("\"") ?: "",
                imageUrl = formatImageUrl(rawUrl)
            )
        }.filter { it.author.isNotBlank() && (it.quote.isNotBlank() || !it.imageUrl.isNullOrBlank()) }.toList()
    }

    private fun formatImageUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (url.contains("drive.google.com/file/d/")) {
            val fileId = url.substringAfter("/d/").substringBefore("/")
            return "https://lh3.googleusercontent.com/d/$fileId"
        }
        return url
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { putBoolean(KEY_NOTIFS_ENABLED, enabled) } }
    fun isNotificationsEnabled(context: Context): Boolean { return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_NOTIFS_ENABLED, false) }
    fun setNotificationTime(context: Context, hour: Int, minute: Int) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { putInt(KEY_NOTIF_HOUR, hour); putInt(KEY_NOTIF_MINUTE, minute) } }
    fun getNotificationTime(context: Context): Pair<Int, Int> { val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE); return Pair(prefs.getInt(KEY_NOTIF_HOUR, 8), prefs.getInt(KEY_NOTIF_MINUTE, 0)) }
    fun setTheme(context: Context, theme: WisdomTheme) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { putString(KEY_THEME, theme.name) } }
    fun getTheme(context: Context): WisdomTheme { val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_THEME, WisdomTheme.Neutral.name); return try { WisdomTheme.valueOf(name!!) } catch (_: Exception) { WisdomTheme.Neutral } }

    fun getInitials(author: String): String {
        return author.split(" ").filter { it.isNotBlank() && it.first().isLetter() }.mapNotNull { it.firstOrNull()?.uppercase() }.take(3).let { if (it.isEmpty()) "?" else it.joinToString("") }
    }

    fun findAuthorImage(authorName: String, allQuotes: List<QuoteItem>): String? {
        val norm = normalizeAccents(authorName)
        authorMetadata[norm]?.imageUrl?.let { return it }
        val img = allQuotes.find { normalizeAccents(it.author) == norm && !it.imageUrl.isNullOrBlank() }?.imageUrl
        if (img != null) authorMetadata[norm] = AuthorMetadata(imageUrl = img, about = authorMetadata[norm]?.about)
        return img
    }

    fun findAuthorAbout(authorName: String): String? { return authorMetadata[normalizeAccents(authorName)]?.about }

    fun normalizeAccents(text: String): String {
        val temp = Normalizer.normalize(text, Normalizer.Form.NFD)
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(temp).replaceAll("").lowercase().trim()
    }

    private var globalQuotesCache: List<QuoteItem>? = null

    suspend fun getAllArchiveQuotes(): List<QuoteItem> {
        if (globalQuotesCache != null) return globalQuotesCache!!
        return fetchGlobalQuotes() ?: emptyList()
    }

    private suspend fun fetchGlobalQuotes(): List<QuoteItem>? = withContext(Dispatchers.IO) {
        try {
            val jsonText = URL("https://raw.githubusercontent.com/dwyl/quotes/master/quotes.json").readText()
            val array = JSONArray(jsonText)
            val list = mutableListOf<QuoteItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rawAuthor = obj.optString("author", "Unknown").trim()
                val tagList = obj.optString("tags", "").split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
                list.add(QuoteItem(author = if (rawAuthor == "type.fit") "Unknown" else rawAuthor, quote = obj.optString("text", ""), about = "EXTENDED ARCHIVE", tags = tagList))
            }
            globalQuotesCache = list
            list
        } catch (e: Exception) { Log.e("QuoteRepository", "Error fetching global quotes", e); null }
    }

    suspend fun findRandomArchiveQuote(): QuoteItem? {
        val all = getAllArchiveQuotes()
        return if (all.isNotEmpty()) all.random().copy(about = "ARCHIVE DISCOVERY") else null
    }

    suspend fun getAllArchiveAuthors(): List<Pair<String, Int>> = withContext(Dispatchers.IO) {
        getAllArchiveQuotes().asSequence().map { it.author }.filter { it.isNotBlank() && it != "Unknown" }.groupingBy { it }.eachCount().toList().sortedBy { it.first }
    }

    suspend fun getArchiveQuotesForAuthor(authorName: String): List<QuoteItem> = withContext(Dispatchers.IO) {
        val norm = normalizeAccents(authorName)
        getAllArchiveQuotes().filter { normalizeAccents(it.author) == norm }
    }

    suspend fun getAllArchiveTags(): List<Pair<String, Int>> = withContext(Dispatchers.IO) {
        getAllArchiveQuotes().asSequence().flatMap { it.tags }.groupingBy { it }.eachCount().toList().sortedBy { it.first }
    }

    suspend fun getArchiveQuotesByTag(tag: String): List<QuoteItem> = withContext(Dispatchers.IO) {
        getAllArchiveQuotes().filter { it.tags.contains(tag.lowercase()) }
    }

    suspend fun fetchZenQuote(): QuoteItem? = withContext(Dispatchers.IO) {
        try {
            val connection = URL("https://zenquotes.io/api/random").openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 8000; connection.readTimeout = 8000; connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            val code = connection.responseCode
            val stream = if (code >= 400) connection.errorStream else connection.inputStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (text.contains("Too many requests", true) || code == 429) return@withContext QuoteItem("System", "", "RATE_LIMIT")
            val array = JSONArray(text)
            if (array.length() > 0) {
                val obj = array.getJSONObject(0)
                return@withContext QuoteItem(author = obj.optString("a", "Unknown"), quote = obj.optString("q", ""), about = "GLOBAL DISCOVERY")
            }
        } catch (e: Exception) { Log.e("QuoteRepository", "ZenQuote failed", e) }
        null
    }
}
