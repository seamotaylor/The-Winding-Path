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
    private const val KEY_CSV = "cached_quotes_v2_csv"
    private const val KEY_NOTIFS_ENABLED = "notifs_enabled"
    private const val KEY_NOTIF_HOUR = "notif_hour"
    private const val KEY_NOTIF_MINUTE = "notif_minute"
    private const val KEY_THEME = "app_theme"

    private val authorMetadata = mutableMapOf<String, AuthorMetadata>()

    data class AuthorMetadata(
        val imageUrl: String? = null, 
        val about: String? = null,
        val imagePriority: Int = 0,
        val aboutPriority: Int = 0,
        val isCurated: Boolean = false
    )

    fun getQuotesFromCache(context: Context): List<QuoteItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val data = prefs.getString(KEY_CSV, null) ?: return emptyList()
        val list = if (data.contains("---TAB_BREAK---")) {
            val tabs = data.split("---TAB_BREAK---")
            val qM = if (tabs.isNotEmpty()) parseCsv(tabs[0], priority = 3) else emptyList()
            val qD = if (tabs.size > 1) parseCsv(tabs[1], priority = 2) else emptyList()
            qM + qD
        } else {
            parseCsv(data, priority = 3)
        }
        indexMetadata(list)
        return list
    }

    fun saveQuotesToCache(context: Context, rawData: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { putString(KEY_CSV, rawData) }
    }

    fun clearMetadata() {
        authorMetadata.clear()
    }

    fun indexMetadata(quotes: List<QuoteItem>) {
        val grouped = quotes.groupBy { normalizeAccents(it.author) }
        grouped.forEach { (norm, authorQuotes) ->
            val existing = authorMetadata[norm]
            
            // Find highest priority items in THIS incoming list
            // We use maxByOrNull because in a single list (like Main Tab), 
            // all items have priority 3. So firstOrNull in sheet order is handled naturally 
            // if we process the list in original order.
            
            // 1. Image logic
            val bestImageItem = authorQuotes.firstOrNull { it.imageUrl?.isNotBlank() == true }
            val incomingImgPriority = bestImageItem?.priority ?: 0
            val existingImgPriority = existing?.imagePriority ?: 0
            
            val finalImage = if (bestImageItem != null && incomingImgPriority >= existingImgPriority) {
                // EXCEPTION: If both are Tier 3 (Main Tab) or both Tier 2 (Archive), 
                // we ONLY update if existing is null to enforce "First in Sheet order wins".
                if (incomingImgPriority == existingImgPriority && existing?.imageUrl != null) {
                    existing.imageUrl
                } else {
                    bestImageItem.imageUrl
                }
            } else {
                existing?.imageUrl
            }
            
            val finalImgPriority = if (finalImage == bestImageItem?.imageUrl) incomingImgPriority else existingImgPriority

            // 2. Biography logic
            val bestAboutItem = authorQuotes.firstOrNull { it.about.isNotBlank() && it.about.trim().uppercase() != "THE EXPANDED LIBRARY" }
            val incomingAboutPriority = bestAboutItem?.priority ?: 0
            val existingAboutPriority = existing?.aboutPriority ?: 0

            val finalAbout = if (bestAboutItem != null && incomingAboutPriority >= existingAboutPriority) {
                if (incomingAboutPriority == existingAboutPriority && existing?.about != null) {
                    existing.about
                } else {
                    bestAboutItem.about
                }
            } else {
                existing?.about
            }
            
            val finalAboutPriority = if (finalAbout == bestAboutItem?.about) incomingAboutPriority else existingAboutPriority

            if (finalImage != null || finalAbout != null) {
                authorMetadata[norm] = AuthorMetadata(
                    imageUrl = finalImage,
                    about = finalAbout,
                    imagePriority = finalImgPriority,
                    aboutPriority = finalAboutPriority,
                    isCurated = finalImgPriority > 1 || finalAboutPriority > 1
                )
            }
        }
    }

    fun parseCsv(rawData: String, priority: Int = 0): List<QuoteItem> {
        return rawData.lineSequence().drop(1).filter { it.contains(",") }.map { line ->
            val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
            val rawUrl = parts.getOrNull(3)?.trim()?.removeSurrounding("\"")
            QuoteItem(
                author = parts.getOrNull(0)?.trim()?.removeSurrounding("\"") ?: "Unknown",
                about = parts.getOrNull(1)?.trim()?.removeSurrounding("\"") ?: "",
                quote = parts.getOrNull(2)?.trim()?.removeSurrounding("\"") ?: "",
                imageUrl = if (rawUrl.isNullOrBlank()) null else formatImageUrl(rawUrl.trim()),
                priority = priority
            )
        }.filter { it.author.isNotBlank() && (it.quote.isNotBlank() || it.imageUrl?.isNotBlank() == true) }.toList()
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
    fun getNotificationTime(context: Context): Pair<Int, Int> { val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE); return Pair(prefs.getInt(KEY_NOTIF_HOUR, 9), prefs.getInt(KEY_NOTIF_MINUTE, 0)) }
    fun setTheme(context: Context, theme: WisdomTheme) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { putString(KEY_THEME, theme.name) } }
    fun getTheme(context: Context): WisdomTheme { val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_THEME, WisdomTheme.Neutral.name); return try { WisdomTheme.valueOf(name!!) } catch (_: Exception) { WisdomTheme.Neutral } }

    fun getInitials(author: String): String {
        return author.split(" ").filter { it.isNotBlank() && it.first().isLetter() }.mapNotNull { it.firstOrNull()?.uppercase() }.take(3).let { if (it.isEmpty()) "?" else it.joinToString("") }
    }

    fun findAuthorImage(authorName: String): String? {
        val norm = normalizeAccents(authorName)
        return authorMetadata[norm]?.imageUrl
    }

    fun findAuthorAbout(authorName: String): String? { return authorMetadata[normalizeAccents(authorName)]?.about }

    suspend fun getAllImagesForAuthor(authorName: String, curatedQuotes: List<QuoteItem>): List<String> = withContext(Dispatchers.IO) {
        val norm = normalizeAccents(authorName)
        val curatedImages = curatedQuotes.filter { normalizeAccents(it.author) == norm && it.imageUrl?.isNotBlank() == true }
            .mapNotNull { it.imageUrl?.trim() }
        
        val globalImages = getAllGlobalQuotes().filter { normalizeAccents(it.author) == norm && it.imageUrl?.isNotBlank() == true }
            .mapNotNull { it.imageUrl?.trim() }
            
        (curatedImages + globalImages).distinctBy { normalizeUrl(it) }
    }

    suspend fun fetchRawSheetData(sid: String, gid: String): String = withContext(Dispatchers.IO) {
        val ts = System.currentTimeMillis()
        val url = "https://docs.google.com/spreadsheets/d/$sid/export?format=csv&gid=$gid&t=$ts"
        URL(url).readText()
    }

    fun normalizeAccents(text: String): String {
        val temp = Normalizer.normalize(text, Normalizer.Form.NFD)
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(temp).replaceAll("").lowercase().trim()
    }

    fun normalizeUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        return url.trim()
            .replace("http://", "https://")
            .removeSuffix("/")
            .lowercase()
    }

    private var globalQuotesCache: List<QuoteItem>? = null
    private var globalAuthorsCache: List<Pair<String, Int>>? = null
    private var globalTagsCache: List<Pair<String, Int>>? = null

    suspend fun getAllGlobalQuotes(): List<QuoteItem> {
        if (globalQuotesCache != null) return globalQuotesCache!!
        return fetchGlobalQuotes() ?: emptyList()
    }

    private suspend fun fetchGlobalQuotes(): List<QuoteItem>? = withContext(Dispatchers.IO) {
        try {
            Log.d("QuoteRepository", "Fetching global quotes library...")
            val jsonText = URL("https://raw.githubusercontent.com/dwyl/quotes/master/quotes.json").readText()
            val array = JSONArray(jsonText)
            Log.d("QuoteRepository", "Downloaded ${array.length()} raw global quotes.")
            val list = ArrayList<QuoteItem>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rawAuthor = obj.optString("author", "Unknown").trim()
                val tagsRaw = obj.optString("tags", "")
                val tagList = if (tagsRaw.isBlank()) emptyList() else tagsRaw.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
                list.add(QuoteItem(
                    author = if (rawAuthor == "type.fit") "Unknown" else rawAuthor, 
                    quote = obj.optString("text", ""), 
                    about = "THE EXPANDED LIBRARY", 
                    tags = tagList,
                    priority = 1
                ))
            }
            globalQuotesCache = list
            Log.d("QuoteRepository", "Successfully parsed ${list.size} unique global quotes.")
            list
        } catch (e: Throwable) { 
            Log.e("QuoteRepository", "Error fetching global quotes: ${e.message}", e)
            if (e is OutOfMemoryError) {
                globalQuotesCache = null // Ensure we don't hold onto partial data
            }
            null 
        }
    }

    suspend fun findRandomGlobalQuote(): QuoteItem? {
        val all = getAllGlobalQuotes()
        return if (all.isNotEmpty()) all.random().copy(about = "THE EXPANDED LIBRARY") else null
    }

    suspend fun getAllGlobalAuthors(): List<Pair<String, Int>> = withContext(Dispatchers.IO) {
        globalAuthorsCache?.let { return@withContext it }
        val result = getAllGlobalQuotes().asSequence()
            .map { it.author }
            .filter { it.isNotBlank() && it != "Unknown" }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedBy { it.first }
        globalAuthorsCache = result
        result
    }

    suspend fun getGlobalQuotesForAuthor(authorName: String): List<QuoteItem> = withContext(Dispatchers.IO) {
        val norm = normalizeAccents(authorName)
        getAllGlobalQuotes().filter { normalizeAccents(it.author) == norm }
    }

    suspend fun getAllGlobalTags(): List<Pair<String, Int>> = withContext(Dispatchers.IO) {
        globalTagsCache?.let { return@withContext it }
        val result = getAllGlobalQuotes().asSequence()
            .flatMap { it.tags }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedBy { it.first }
        globalTagsCache = result
        result
    }

    suspend fun getGlobalQuotesByTag(tag: String): List<QuoteItem> = withContext(Dispatchers.IO) {
        getAllGlobalQuotes().filter { it.tags.contains(tag.lowercase()) }
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
                return@withContext QuoteItem(
                    author = obj.optString("a", "Unknown"), 
                    quote = obj.optString("q", ""), 
                    about = "THE EXPANDED LIBRARY",
                    priority = 1
                )
            }
        } catch (e: Exception) { Log.e("QuoteRepository", "ZenQuote failed", e) }
        null
    }
}
