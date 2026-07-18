/* KEEP YOUR PACKAGE NAME HERE */
package com.example.copy_pastewisdom

import android.Manifest
import android.app.AlarmManager
import android.widget.Toast
import android.app.PendingIntent
import android.provider.Settings
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import kotlin.random.Random
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.AnnotatedString
import com.example.copy_pastewisdom.ui.theme.CopyPasteWisdomTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.text.Normalizer
import java.util.Calendar
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

// Add this for standard icons if not available
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowUpward


// --- SECTION 1: DATA MODELS ---
data class QuoteItem(
    val author: String,
    val about: String,
    val quote: String,
    val imageUrl: String? = null
)

sealed class QuoteState {
    object Loading : QuoteState()
    data class Success(val quotes: List<QuoteItem>) : QuoteState()
    data class Error(val message: String) : QuoteState()
}

// --- SECTION 2: REPOSITORY & CACHE LOGIC ---
enum class WisdomTheme(val label: String, val primaryColor: Color) {
    Neutral("Neutral", Color(0xFFE0E0E0)),
    Gold("Scholarly", Color(0xFFD4AF37)),
    Sage("Peaceful", Color(0xFF8FBC8F)),
    Blue("Intellectual", Color(0xFF78909C))
}

object QuoteRepository {
    private const val PREFS_NAME = "quote_prefs"
    private const val KEY_CSV = "cached_quotes_csv"
    private const val KEY_NOTIFS_ENABLED = "notifs_enabled"
    private const val KEY_NOTIF_HOUR = "notif_hour"
    private const val KEY_NOTIF_MINUTE = "notif_minute"
    private const val KEY_THEME = "app_theme"

    fun getQuotesFromCache(context: Context): List<QuoteItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val data = prefs.getString(KEY_CSV, null)
        return data?.let { parseCsv(it) } ?: emptyList()
    }

    fun saveQuotesToCache(context: Context, rawData: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_CSV, rawData)
        }
    }

    fun parseCsv(rawData: String): List<QuoteItem> {
        Log.d("QuoteRepository", "Parsing CSV data, length: ${rawData.length}")
        return rawData.lineSequence()
            .drop(1) // Exclude header row
            .filter { it.contains(",") }
            .map { line ->
                // Use a regex that only splits by comma if it's not inside double quotes
                val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                val rawUrl = parts.getOrNull(3)?.trim()?.removeSurrounding("\"")
                QuoteItem(
                    author = parts.getOrNull(0)?.trim()?.removeSurrounding("\"") ?: "Unknown",
                    about = parts.getOrNull(1)?.trim()?.removeSurrounding("\"") ?: "",
                    quote = parts.getOrNull(2)?.trim()?.removeSurrounding("\"") ?: "",
                    imageUrl = formatImageUrl(rawUrl)
                )
            }
            .filter { it.quote.isNotBlank() }
            .toList()
    }

    private fun formatImageUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        
        // Convert Google Drive "view" links to direct download links
        if (url.contains("drive.google.com/file/d/")) {
            val fileId = url.substringAfter("/d/").substringBefore("/")
            return "https://lh3.googleusercontent.com/d/$fileId"
        }
        
        return url
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_NOTIFS_ENABLED, enabled)
        }
    }

    fun isNotificationsEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIFS_ENABLED, false)
    }

    fun setNotificationTime(context: Context, hour: Int, minute: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putInt(KEY_NOTIF_HOUR, hour)
            putInt(KEY_NOTIF_MINUTE, minute)
        }
    }

    fun getNotificationTime(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Pair(
            prefs.getInt(KEY_NOTIF_HOUR, 8),
            prefs.getInt(KEY_NOTIF_MINUTE, 0),
        )
    }

    fun setTheme(context: Context, theme: WisdomTheme) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_THEME, theme.name)
        }
    }

    fun getTheme(context: Context): WisdomTheme {
        val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, WisdomTheme.Neutral.name)
        return try { WisdomTheme.valueOf(name!!) } catch (_: Exception) { WisdomTheme.Neutral }
    }

    fun normalizeAccents(text: String): String {
        val temp = Normalizer.normalize(text, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(temp).replaceAll("").lowercase().trim()
    }

    private var globalQuotesCache: List<QuoteItem>? = null

    private suspend fun fetchGlobalQuotes(): List<QuoteItem>? = withContext(Dispatchers.IO) {
        if (globalQuotesCache != null) return@withContext globalQuotesCache
        try {
            // Using a massive 30k+ quote archive from GitHub (dwyl)
            // This replaces the smaller DummyJSON and restricted ZenQuotes searches
            val jsonText = URL("https://raw.githubusercontent.com/dwyl/quotes/master/quotes.json").readText()
            val jsonArray = JSONArray(jsonText)
            val list = mutableListOf<QuoteItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val rawAuthor = obj.optString("author", "Unknown").trim()
                list.add(QuoteItem(
                    author = if (rawAuthor == "type.fit") "Unknown" else rawAuthor,
                    quote = obj.optString("text", ""),
                    about = "EXTENDED ARCHIVE"
                ))
            }
            globalQuotesCache = list
            list
        } catch (e: Exception) {
            Log.e("QuoteRepository", "Error fetching global quotes", e)
            null
        }
    }

    suspend fun findRandomArchiveQuote(): QuoteItem? {
        val allQuotes = fetchGlobalQuotes() ?: return null
        return if (allQuotes.isNotEmpty()) {
            val random = allQuotes.random()
            random.copy(about = "ARCHIVE DISCOVERY")
        } else null
    }

    suspend fun getAllArchiveAuthors(): List<String> = withContext(Dispatchers.IO) {
        val allQuotes = fetchGlobalQuotes() ?: return@withContext emptyList()
        allQuotes.asSequence()
            .map { it.author }
            .filter { it.isNotBlank() && it != "Unknown" }
            .distinct()
            .sorted()
            .toList()
    }

    suspend fun getArchiveQuotesForAuthor(authorName: String): List<QuoteItem> = withContext(Dispatchers.IO) {
        val allQuotes = fetchGlobalQuotes() ?: return@withContext emptyList()
        val normalizedTarget = normalizeAccents(authorName)
        allQuotes.filter { normalizeAccents(it.author) == normalizedTarget }
    }

    suspend fun fetchZenQuote(): QuoteItem? = withContext(Dispatchers.IO) {
        var attempts = 0
        val maxAttempts = 3
        
        while (attempts < maxAttempts) {
            attempts++
            try {
                val urlString = "https://zenquotes.io/api/random"
                
                val connection = URL(urlString).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                // Use a more common User-Agent to avoid blocks
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                
                val responseCode = connection.responseCode
                if (responseCode == 429) {
                    if (attempts < maxAttempts) {
                        kotlinx.coroutines.delay(1500)
                        continue
                    }
                    return@withContext QuoteItem("System", "", "RATE_LIMIT")
                }

                val stream = if (responseCode >= 400) connection.errorStream else connection.inputStream
                if (stream == null) continue
                
                val jsonText = stream.bufferedReader().use { it.readText() }
                
                if (jsonText.contains("Too many requests", ignoreCase = true)) {
                    if (attempts < maxAttempts) {
                        kotlinx.coroutines.delay(1500)
                        continue
                    }
                    return@withContext QuoteItem("System", "", "RATE_LIMIT")
                }

                val jsonArray = JSONArray(jsonText)
                if (jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    val quoteText = obj.optString("q", "")
                    val authorName = obj.optString("a", "Unknown")
                    
                    if (authorName == "zenquotes.io" || quoteText.contains("Too many requests", ignoreCase = true)) {
                        if (attempts < maxAttempts) {
                            kotlinx.coroutines.delay(1500)
                            continue
                        }
                        return@withContext QuoteItem("System", "", "RATE_LIMIT")
                    }

                    return@withContext QuoteItem(
                        author = authorName,
                        quote = quoteText,
                        about = "GLOBAL DISCOVERY",
                        imageUrl = null
                    )
                }
            } catch (e: Exception) {
                Log.e("QuoteRepository", "Attempt $attempts failed: ${e.message}")
                if (attempts == maxAttempts) return@withContext null
                kotlinx.coroutines.delay(1000)
            }
        }
        null
    }
}

// --- SECTION 3: SCHEDULING ---
object NotificationScheduler {
    fun scheduleDailyNotification(context: Context) {
        val (hour, minute) = QuoteRepository.getNotificationTime(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.HOUR_OF_DAY, 24)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                // Fallback to inexact if permission not granted
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    fun cancelDailyNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}

// --- SECTION 4: MAIN ACTIVITY & UI ---
private val DarkBackground = Color(0xFF121212)
private val PrimaryText = Color(0xFFEDEDED)
private val SecondaryText = Color(0xFFBDBDBD)
private val CardSurface = Color(0xFF1E1E1E)

@Composable
fun WisdomAppTheme(theme: WisdomTheme = WisdomTheme.Neutral, content: @Composable () -> Unit) {
    val wisdomColorScheme = darkColorScheme(
        primary = theme.primaryColor,
        onPrimary = Color.Black,
        secondary = theme.primaryColor.copy(alpha = 0.8f),
        onSecondary = Color.Black,
        background = DarkBackground,
        onBackground = PrimaryText,
        surface = CardSurface,
        onSurface = PrimaryText,
        surfaceVariant = Color(0xFF2C2C2C),
        onSurfaceVariant = SecondaryText
    )

    MaterialTheme(
        colorScheme = wisdomColorScheme,
        content = content
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var currentTheme by remember { mutableStateOf(QuoteRepository.getTheme(this)) }
            
            WisdomAppTheme(theme = currentTheme) {
                MainScreen(
                    context = this,
                    currentTheme = currentTheme,
                    onThemeChange = { newTheme ->
                        currentTheme = newTheme
                        QuoteRepository.setTheme(this, newTheme)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(context: Context, currentTheme: WisdomTheme, onThemeChange: (WisdomTheme) -> Unit) {
    var uiState by remember { mutableStateOf<QuoteState>(QuoteState.Loading) }
    var notificationsEnabled by remember { 
        mutableStateOf(QuoteRepository.isNotificationsEnabled(context)) 
    }
    var notificationTime by remember {
        mutableStateOf(QuoteRepository.getNotificationTime(context))
    }
    var isBrowsing by remember { mutableStateOf(value = false) }
    var browseSelectedItem by remember { mutableStateOf<QuoteItem?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult<String, Boolean>(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            notificationsEnabled = true
            QuoteRepository.setNotificationsEnabled(context, enabled = true)
            NotificationScheduler.scheduleDailyNotification(context)
        }
    }

    // Time Picker Dialog
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            notificationTime = Pair(hour, minute)
            QuoteRepository.setNotificationTime(context, hour, minute)
            if (notificationsEnabled) {
                NotificationScheduler.scheduleDailyNotification(context)
            }
        },
        notificationTime.first,
        notificationTime.second,
        true // 24 hour mode true
    )

    suspend fun fetchQuotes() {
        uiState = QuoteState.Loading
        uiState = try {
            val timestamp = System.currentTimeMillis()
            val url = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQbAfs-rGaKSvL5F8RZDLr90glOyKsKsTZsDYToO1QcqfpVIIr5XhBAvtuCtJJqz-ZwG191quZMRnwp/pub?gid=0&single=true&output=csv&t=$timestamp"
            Log.d("MainActivity", "Fetching quotes from: $url")
            val rawData = withContext(Dispatchers.IO) { URL(url).readText() }
            val quotes = QuoteRepository.parseCsv(rawData)
            
            if (quotes.isNotEmpty()) {
                QuoteRepository.saveQuotesToCache(context, rawData)
                QuoteState.Success(quotes)
            } else {
                throw Exception("No quotes found")
            }
        } catch (_: Exception) {
            val cached = QuoteRepository.getQuotesFromCache(context)
            if (cached.isNotEmpty()) {
                QuoteState.Success(cached)
            } else {
                QuoteState.Error("Network error and no cache available.")
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchQuotes()
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState
        ) {
            SettingsContent(
                notificationsEnabled = notificationsEnabled,
                notificationTime = notificationTime,
                currentTheme = currentTheme,
                onThemeChange = onThemeChange,
                onToggleNotifications = { isChecked ->
                    if (isChecked) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            notificationsEnabled = true
                            QuoteRepository.setNotificationsEnabled(context, enabled = true)
                            NotificationScheduler.scheduleDailyNotification(context)
                        }
                    } else {
                        notificationsEnabled = false
                        QuoteRepository.setNotificationsEnabled(context, enabled = false)
                        NotificationScheduler.cancelDailyNotification(context)
                    }
                },
                onShowTimePicker = { timePickerDialog.show() }
            )
        }
    }

    Scaffold(
        topBar = {
            if (!isBrowsing) {
                WisdomTopBar(
                    onRefresh = { scope.launch { fetchQuotes() } },
                    onSettings = { showSettings = true }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            Color(0xFF000000)
                        )
                    )
                )
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(300))
                }, label = ""
            ) { state ->
                when (state) {
                    is QuoteState.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    is QuoteState.Success -> {
                        if (isBrowsing) {
                            BrowseQuotesView(
                                quotes = state.quotes,
                                onQuoteSelected = { 
                                    browseSelectedItem = it
                                    isBrowsing = false 
                                },
                                onBack = { isBrowsing = false }
                            )
                        } else {
                            QuoteDisplay(
                                quotes = state.quotes,
                                externalSelectedQuote = browseSelectedItem,
                                onBrowseClick = { isBrowsing = true }
                            )
                        }
                    }
                    is QuoteState.Error -> ErrorView(
                        message = state.message,
                        onRetry = { scope.launch { fetchQuotes() } }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WisdomTopBar(onRefresh: () -> Unit, onSettings: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                "Copy-Paste Wisdom",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.5.sp,
                    color = SecondaryText
                )
            )
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SecondaryText)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = SecondaryText)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun AnimatedZoomDialog(
    onDismissRequest: () -> Unit,
    startRect: Rect? = null,
    content: @Composable (scale: Float, alpha: Float, offset: Offset, dismiss: () -> Unit) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Calculate initial scale and offset to match startRect
    val initialScale = if (startRect != null) {
        (startRect.width / screenWidth).coerceAtLeast(startRect.height / screenHeight)
    } else 0.4f

    val initialOffset = if (startRect != null) {
        Offset(
            x = startRect.center.x - (screenWidth / 2),
            y = startRect.center.y - (screenHeight / 2)
        )
    } else Offset.Zero

    val scale by animateFloatAsState(
        targetValue = if (isVisible && !isExiting) 1f else initialScale,
        animationSpec = if (!isExiting) {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        } else {
            tween(durationMillis = 200)
        },
        label = "scale",
        finishedListener = { if (isExiting) onDismissRequest() }
    )

    val offset by animateOffsetAsState(
        targetValue = if (isVisible && !isExiting) Offset.Zero else initialOffset,
        animationSpec = if (!isExiting) {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        } else {
            tween(durationMillis = 200)
        },
        label = "offset"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible && !isExiting) 1f else 0f,
        animationSpec = tween(durationMillis = if (isExiting) 150 else 250),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Dialog(
        onDismissRequest = { isExiting = true },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = alpha))
                .clickable { isExiting = true },
            contentAlignment = Alignment.Center
        ) {
            content(scale, alpha, offset) { isExiting = true }
        }
    }
}

@Composable
fun SettingsContent(
    notificationsEnabled: Boolean,
    notificationTime: Pair<Int, Int>,
    currentTheme: WisdomTheme,
    onThemeChange: (WisdomTheme) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onShowTimePicker: () -> Unit
) {
    var showFullScreenIcon by remember { mutableStateOf(false) }
    var iconRect by remember { mutableStateOf<Rect?>(null) }

    if (showFullScreenIcon) {
        AnimatedZoomDialog(
            onDismissRequest = { showFullScreenIcon = false },
            startRect = iconRect
        ) { scale, alpha, offset, dismiss ->
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = R.mipmap.ic_launcher,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .graphicsLayer(
                            scaleX = scale, 
                            scaleY = scale, 
                            alpha = alpha,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentScale = ContentScale.Fit
                )

                IconButton(
                    onClick = dismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .alpha(alpha)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        ThemeSelector(currentTheme = currentTheme, onThemeChange = onThemeChange)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
        
        Text(
            text = "Reminders",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        NotificationSettingsRow(enabled = notificationsEnabled, onToggle = onToggleNotifications)
        TimeSettingsRow(
            hour = notificationTime.first,
            minute = notificationTime.second,
            onClick = onShowTimePicker
        )

        // Android 13+ Exact Alarm Permission Check
        if (notificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = LocalContext.current.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val context = LocalContext.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Timing might be inexact due to system restrictions.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }) {
                        Text("Fix Now")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
        
        Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = R.mipmap.ic_launcher,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .onGloballyPositioned { coordinates ->
                        val position = coordinates.positionInWindow()
                        iconRect = Rect(position, coordinates.size.toSize())
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showFullScreenIcon = true }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "The labyrinth represents the philosophical journey—a single, winding path to the center of truth. Unlike a maze designed to confuse, the labyrinth is a meditative quest for wisdom and self-discovery.",
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText
            )
        }
    }
}

@Composable
fun ThemeSelector(currentTheme: WisdomTheme, onThemeChange: (WisdomTheme) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(WisdomTheme.values()) { theme ->
            val isSelected = theme == currentTheme
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onThemeChange(theme) }
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(theme.primaryColor, CircleShape)
                        .padding(4.dp)
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check, 
                                contentDescription = null, 
                                tint = Color.Black
                            )
                        }
                    }
                }
                Text(
                    text = theme.label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else SecondaryText
                )
            }
        }
    }
}


@Composable
fun NotificationSettingsRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = "Daily Notifications", style = MaterialTheme.typography.titleMedium)
            Text(text = "Receive wisdom every morning", style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
fun TimeSettingsRow(hour: Int, minute: Int, onClick: () -> Unit) {
    val displayHour = hour.toString().padStart(2, '0')
    val displayMinute = minute.toString().padStart(2, '0')
    val timeString = "$displayHour:$displayMinute"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = "Notification Time", style = MaterialTheme.typography.titleSmall)
            Text(text = "Currently set to $timeString", style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = onClick) {
            Text("Set Time")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDisplay(
    quotes: List<QuoteItem>,
    externalSelectedQuote: QuoteItem? = null,
    onBrowseClick: () -> Unit,
) {
    val dayOfYear = Calendar.getInstance()[Calendar.DAY_OF_YEAR]
    val dailyQuote = quotes[dayOfYear % quotes.size]

    var reshuffleSeed by remember { mutableStateOf(0) }

    val shuffledQuotes = remember(quotes, reshuffleSeed) { 
        if (quotes.isEmpty()) emptyList() else quotes.shuffled(kotlin.random.Random(reshuffleSeed)) 
    }
    val dailyIndexInShuffled = remember(shuffledQuotes, dailyQuote) {
        shuffledQuotes.indexOf(dailyQuote).coerceAtLeast(0)
    }

    val infinitePageCount = 1000000
    val initialPage = remember(shuffledQuotes, dailyIndexInShuffled) {
        if (shuffledQuotes.isEmpty()) 0 
        else (infinitePageCount / 2) - ((infinitePageCount / 2) % shuffledQuotes.size) + dailyIndexInShuffled
    }

    val pagerState = rememberPagerState(initialPage = initialPage) { 
        if (shuffledQuotes.isEmpty()) 0 else infinitePageCount 
    }
    val scope = rememberCoroutineScope()
    val aboutSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAboutDialog by remember { mutableStateOf(value = false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(pagerState.currentPage) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val currentItem = shuffledQuotes[pagerState.currentPage % shuffledQuotes.size]
    val isDaily = (pagerState.currentPage % shuffledQuotes.size) == dailyIndexInShuffled

    val authorAbout = remember(currentItem.author, quotes) {
        quotes.find { (it.author == currentItem.author) && it.about.isNotBlank() }?.about ?: ""
    }

    val authorImageUrls = remember(currentItem.author, quotes) {
        quotes.filter { it.author == currentItem.author && !it.imageUrl.isNullOrBlank() }
            .map { it.imageUrl!! }
            .distinct()
    }

    LaunchedEffect(externalSelectedQuote) {
        externalSelectedQuote?.let { selected ->
            val indexInList = shuffledQuotes.indexOf(selected)
            if (indexInList >= 0) {
                val currentBase = pagerState.currentPage - (pagerState.currentPage % shuffledQuotes.size)
                pagerState.scrollToPage(currentBase + indexInList)
            }
        }
    }

    if (showAboutDialog) {
        ModalBottomSheet(
            onDismissRequest = { showAboutDialog = false },
            sheetState = aboutSheetState
        ) {
            AuthorAboutContent(
                author = currentItem.author,
                about = authorAbout,
                imageUrls = authorImageUrls,
                onClose = { 
                    scope.launch { aboutSheetState.hide() }.invokeOnCompletion {
                        showAboutDialog = false
                    }
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Pager Section
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(vertical = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { page ->
            val actualPage = page % shuffledQuotes.size
            val item = shuffledQuotes[actualPage]
            val authorImageUrlOnCard = remember(item.author, quotes) {
                quotes.find { it.author == item.author && !it.imageUrl.isNullOrBlank() }?.imageUrl
            }
            QuoteCard(
                item = item,
                authorImageUrl = authorImageUrlOnCard,
                isDaily = actualPage == dailyIndexInShuffled,
                pagerState = pagerState,
                page = page,
                onAboutClick = { showAboutDialog = true }
            )
        }

        // CTA Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 48.dp),
        ) {
            if (!isDaily) {
                TextButton(
                    onClick = {
                        val nextSeed = reshuffleSeed + 1
                        val newShuffled = quotes.shuffled(kotlin.random.Random(nextSeed))
                        val newDailyIndex = newShuffled.indexOf(dailyQuote).coerceAtLeast(0)
                        
                        reshuffleSeed = nextSeed
                        
                        scope.launch {
                            val currentBase = pagerState.currentPage - (pagerState.currentPage % newShuffled.size)
                            pagerState.animateScrollToPage(currentBase + newDailyIndex) 
                        }
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        "Return to Today's Wisdom",
                        style = MaterialTheme.typography.labelLarge.copy(color = SecondaryText)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(64.dp))
            }

            Button(
                onClick = onBrowseClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    "Browse All Quotes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun QuoteCard(
    item: QuoteItem,
    authorImageUrl: String?,
    isDaily: Boolean,
    pagerState: PagerState,
    page: Int,
    onAboutClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .graphicsLayer {
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                val scale = lerp(0.9f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                scaleX = scale
                scaleY = scale
                alpha = lerp(0.5f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
            },
        colors = CardDefaults.elevatedCardColors(containerColor = CardSurface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isDaily) "TODAY'S WISDOM" else "SHUFFLED WISDOM",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDaily) MaterialTheme.colorScheme.primary else SecondaryText
                    )
                )
                if (isDaily) {
                    Text(
                        text = "Swipe for more",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Quote Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "“",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.Start)
                )

                Text(
                    text = item.quote,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontStyle = FontStyle.Italic,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Medium,
                        fontSize = 26.sp,
                        color = PrimaryText
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Text(
                    text = "”",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }

            // Author Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .alpha(0.9f)
            ) {
                AuthorAvatar(author = item.author, imageUrl = authorImageUrl, size = 150.dp)
                
                Spacer(modifier = Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = item.author,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    )
                    TextButton(
                        onClick = onAboutClick,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            "Learn more",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseQuotesView(
    quotes: List<QuoteItem>,
    onQuoteSelected: (QuoteItem) -> Unit,
    onBack: () -> Unit
) {
    val curatedAuthors = remember(quotes) { 
        quotes.asSequence().map { it.author }.distinct().sorted().toList() 
    }
    var showAllArchive by remember { mutableStateOf(false) }
    var allArchiveAuthors by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingArchiveAuthors by remember { mutableStateOf(false) }

    val fullAuthorList = remember(curatedAuthors, allArchiveAuthors, showAllArchive) {
        if (showAllArchive) {
            val merged = mutableMapOf<String, String>() // Map<NormalizedName, OriginalName>
            
            // Add archive authors first (lower priority)
            allArchiveAuthors.forEach { name ->
                merged[QuoteRepository.normalizeAccents(name)] = name
            }
            
            // Overwrite with curated authors (higher priority for spelling/accents)
            curatedAuthors.forEach { name ->
                merged[QuoteRepository.normalizeAccents(name)] = name
            }
            
            merged.values.sortedWith(String.CASE_INSENSITIVE_ORDER)
        } else {
            curatedAuthors
        }
    }

    var selectedAuthor by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val aboutSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAboutDialog by remember { mutableStateOf(value = false) }
    
    // Search & Navigation States
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // External Quote States
    var luckyQuote by remember { mutableStateOf<QuoteItem?>(null) }
    var authorArchiveQuotes by remember { mutableStateOf<List<QuoteItem>>(emptyList()) }
    var isFetchingArchiveForAuthor by remember { mutableStateOf(false) }
    var isFetchingLucky by remember { mutableStateOf(false) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubbingChar by remember { mutableStateOf("") }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val filteredAuthorList = remember(fullAuthorList, searchQuery) {
        if (searchQuery.isBlank()) {
            fullAuthorList
        } else {
            fullAuthorList.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    LaunchedEffect(showAllArchive) {
        if (showAllArchive && allArchiveAuthors.isEmpty()) {
            isLoadingArchiveAuthors = true
            allArchiveAuthors = QuoteRepository.getAllArchiveAuthors()
            isLoadingArchiveAuthors = false
        }
    }

    LaunchedEffect(selectedAuthor) {
        if (selectedAuthor != null) {
            searchQuery = "" // Reset search when entering detail view
            isFetchingArchiveForAuthor = true
            authorArchiveQuotes = QuoteRepository.getArchiveQuotesForAuthor(selectedAuthor!!)
            isFetchingArchiveForAuthor = false
        } else {
            authorArchiveQuotes = emptyList()
        }
    }

    // System Back Press Handling
    BackHandler {
        if (selectedAuthor != null) {
            selectedAuthor = null
        } else {
            onBack()
        }
    }

    val currentAuthorAbout = remember(selectedAuthor, quotes) {
        quotes.find { (it.author == selectedAuthor) && it.about.isNotBlank() }?.about ?: ""
    }

    val currentAuthorImageUrls = remember(selectedAuthor, quotes) {
        quotes.filter { it.author == selectedAuthor && !it.imageUrl.isNullOrBlank() }
            .map { it.imageUrl!! }
            .distinct()
    }

    if (showAboutDialog && (selectedAuthor != null)) {
        ModalBottomSheet(
            onDismissRequest = { showAboutDialog = false },
            sheetState = aboutSheetState
        ) {
            AuthorAboutContent(
                author = selectedAuthor!!,
                about = currentAuthorAbout,
                imageUrls = currentAuthorImageUrls,
                onClose = { 
                    scope.launch { aboutSheetState.hide() }.invokeOnCompletion {
                        showAboutDialog = false
                    }
                }
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (selectedAuthor == null) "Authors" else "Quotes by $selectedAuthor",
                style = MaterialTheme.typography.headlineSmall
            )
            TextButton(onClick = onBack) {
                Text("Close")
            }
        }

        if (selectedAuthor == null) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = { Text("Search authors...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedAuthor == null) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header for Lucky Quote
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Discover New Wisdom",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    isFetchingLucky = true
                                    val result = QuoteRepository.fetchZenQuote()
                                    if (result != null) {
                                        if (result.quote == "RATE_LIMIT") {
                                            Toast.makeText(context, "API Cooldown: Please wait 30 seconds", Toast.LENGTH_LONG).show()
                                        } else {
                                            luckyQuote = result
                                        }
                                    } else {
                                        // Fail-safe: Pull from local archive instead of showing error
                                        val fallback = QuoteRepository.findRandomArchiveQuote()
                                        if (fallback != null) {
                                            luckyQuote = fallback
                                            Toast.makeText(context, "Using archived discovery (web offline)", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Connection lost. Try again later.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    isFetchingLucky = false
                                }
                            },
                            enabled = !isFetchingLucky,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isFetchingLucky) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Text("I'm Feeling Lucky")
                            }
                        }
                    }

                    luckyQuote?.let { item ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            onClick = {
                                clipboardManager.setText(AnnotatedString("“${item.quote}” — ${item.author}"))
                                Toast.makeText(context, "Quote copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "“${item.quote}”",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AuthorAvatar(author = item.author, size = 24.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "— ${item.author}", style = MaterialTheme.typography.labelLarge)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = item.about,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Toggle for Archive
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Extended Archive", style = MaterialTheme.typography.titleSmall)
                            Text(text = "Browse 30,000+ global quotes", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                        }
                        if (isLoadingArchiveAuthors) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Switch(checked = showAllArchive, onCheckedChange = { showAllArchive = it })
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(end = if (showAllArchive && searchQuery.isBlank()) 32.dp else 0.dp)
                        ) {
                            items(filteredAuthorList) { author ->
                                val authorImageUrl = remember(author, quotes) {
                                    quotes.find { it.author == author }?.imageUrl
                                }
                                Surface(
                                    onClick = { selectedAuthor = author },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AuthorAvatar(author = author, imageUrl = authorImageUrl, size = 40.dp)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = author,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                            }
                        }

                        // Fast-Scroll Bar with Letter Bubble
                        if (showAllArchive && searchQuery.isBlank() && filteredAuthorList.isNotEmpty()) {
                            val totalItems = filteredAuthorList.size
                            var trackHeight by remember { mutableStateOf(0f) }
                            
                            val scrollPercentage by remember(totalItems) {
                                derivedStateOf {
                                    if (totalItems == 0) 0f
                                    else (listState.firstVisibleItemIndex.toFloat() / totalItems).coerceIn(0f, 1f)
                                }
                            }

                            val handleHeightPx = with(LocalDensity.current) { 80.dp.toPx() }
                            val bubbleSizePx = with(LocalDensity.current) { 80.dp.toPx() }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .width(32.dp)
                                    .zIndex(1f)
                                    .onGloballyPositioned { trackHeight = it.size.height.toFloat() }
                                    .pointerInput(totalItems) {
                                        detectVerticalDragGestures(
                                            onDragStart = { isScrubbing = true },
                                            onDragEnd = { isScrubbing = false },
                                            onDragCancel = { isScrubbing = false }
                                        ) { change, _ ->
                                            val y = change.position.y
                                            val percentage = (y / trackHeight).coerceIn(0f, 1f)
                                            val index = (percentage * totalItems).toInt().coerceIn(0, totalItems - 1)
                                            
                                            scrubbingChar = filteredAuthorList.getOrNull(index)?.firstOrNull()?.uppercase()?.toString() ?: ""
                                            
                                            scope.launch { listState.scrollToItem(index) }
                                        }
                                    }
                            ) {
                                // Track
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(2.dp)
                                        .align(Alignment.Center)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
                                )
                                
                                // Handle
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .offset { 
                                            IntOffset(0, (scrollPercentage * (trackHeight - handleHeightPx)).toInt()) 
                                        }
                                        .padding(horizontal = 8.dp)
                                        .background(
                                            color = if (isScrubbing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                )

                                // Letter Bubble (Visible only while scrubbing - Moving with Handle)
                                if (isScrubbing) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset {
                                                IntOffset(
                                                    -64.dp.toPx().toInt(),
                                                    (scrollPercentage * (trackHeight - handleHeightPx)).toInt() - (bubbleSizePx / 2).toInt() + (handleHeightPx / 2).toInt()
                                                )
                                            }
                                            .size(80.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = scrubbingChar,
                                            style = MaterialTheme.typography.headlineLarge.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 40.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { 
                    selectedAuthor = null 
                }) {
                    Text("← Back to Authors")
                }
                Spacer(modifier = Modifier.weight(1f))
                if (currentAuthorAbout.isNotBlank()) {
                    TextButton(onClick = { showAboutDialog = true }) {
                        Text("About Author")
                    }
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                val curatedForAuthor = quotes.filter { it.author == selectedAuthor }
                
                // Show Curated first
                if (curatedForAuthor.isNotEmpty()) {
                    item {
                        Text(
                            "CURATED WISDOM", 
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(curatedForAuthor) { item ->
                        Surface(
                            onClick = { onQuoteSelected(item) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = "“${item.quote}”",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Show Archive
                if (authorArchiveQuotes.isNotEmpty()) {
                    item {
                        Text(
                            "EXTENDED ARCHIVE", 
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(authorArchiveQuotes) { item ->
                        Surface(
                            onClick = { 
                                clipboardManager.setText(AnnotatedString("“${item.quote}” — ${item.author}"))
                                Toast.makeText(context, "Quote copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = "“${item.quote}”",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else if (isFetchingArchiveForAuthor) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthorAvatar(author: String, imageUrl: String? = null, size: androidx.compose.ui.unit.Dp) {
    val backgroundColor = remember(author) {
        val colors = listOf(
            Color(0xFFEF5350), Color(0xFFEC407A), Color(0xFFAB47BC),
            Color(0xFF7E57C2), Color(0xFF5C6BC0), Color(0xFF42A5F5),
            Color(0xFF26A69A), Color(0xFF66BB6A), Color(0xFFFFA726)
        )
        colors[author.hashCode().coerceAtLeast(0) % colors.size]
    }

    var isError by remember(imageUrl) { mutableStateOf(false) }

    Surface(
        modifier = Modifier.size(size),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = backgroundColor,
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!imageUrl.isNullOrBlank() && !isError) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .addHeader("User-Agent", "CopyPasteWisdom/1.0 (https://github.com/seamotaylor/Copy-Paste-Wisdom) Coil/2.6.0")
                        .crossfade(true)
                        .build(),
                    contentDescription = author,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { 
                        Log.e("AuthorAvatar", "Failed to load image for $author: $imageUrl", it.result.throwable)
                        isError = true 
                    }
                )
            }
            
            if (imageUrl.isNullOrBlank() || isError) {
                Text(
                    text = author.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = if (size < 40.dp) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun AuthorAboutContent(
    author: String,
    about: String,
    imageUrls: List<String> = emptyList(),
    onClose: () -> Unit
) {
    var showFullScreenImage by remember { mutableStateOf(false) }
    var portraitRect by remember { mutableStateOf<Rect?>(null) }
    val scope = rememberCoroutineScope()

    if (showFullScreenImage && imageUrls.isNotEmpty()) {
        val infiniteImageCount = 1000000
        val initialImagePage = remember(imageUrls) {
            if (imageUrls.size <= 1) 0
            else (infiniteImageCount / 2) - ((infiniteImageCount / 2) % imageUrls.size)
        }
        val imagePagerState = rememberPagerState(initialPage = initialImagePage) { 
            if (imageUrls.size > 1) infiniteImageCount else imageUrls.size 
        }
        AnimatedZoomDialog(
            onDismissRequest = { showFullScreenImage = false },
            startRect = portraitRect
        ) { scale, alpha, offset, dismiss ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale, 
                        scaleY = scale, 
                        alpha = alpha,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = imagePagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = true 
                ) { page ->
                    val actualPage = page % imageUrls.size
                    var itemScale by remember { mutableStateOf(1f) }
                    var itemOffset by remember { mutableStateOf(Offset.Zero) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RectangleShape)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    do {
                                        val event = awaitPointerEvent()
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()

                                        if (itemScale > 1f || zoomChange != 1f) {
                                            event.changes.forEach { it.consume() }
                                            
                                            itemScale = (itemScale * zoomChange).coerceIn(1f, 5f)
                                            if (itemScale > 1f) {
                                                itemOffset += panChange
                                            } else {
                                                itemOffset = Offset.Zero
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { 
                                    if (itemScale > 1f) {
                                        scope.launch {
                                            launch {
                                                animate(initialValue = itemScale, targetValue = 1f) { value, _ ->
                                                    itemScale = value
                                                }
                                            }
                                            launch {
                                                animate(
                                                    initialValue = itemOffset,
                                                    targetValue = Offset.Zero,
                                                    typeConverter = Offset.VectorConverter
                                                ) { value, _ ->
                                                    itemOffset = value
                                                }
                                            }
                                        }
                                    } else {
                                        dismiss()
                                    }
                                })
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrls[actualPage])
                                .addHeader("User-Agent", "CopyPasteWisdom/1.0 (https://github.com/seamotaylor/Copy-Paste-Wisdom) Coil/2.6.0")
                                .crossfade(true)
                                .build(),
                            contentDescription = author,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .graphicsLayer(
                                    scaleX = itemScale,
                                    scaleY = itemScale,
                                    translationX = itemOffset.x,
                                    translationY = itemOffset.y
                                ),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                
                if (imageUrls.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(imageUrls.size) { iteration ->
                            val color = if ((imagePagerState.currentPage % imageUrls.size) == iteration) Color.White else Color.White.copy(alpha = 0.5f)
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .background(color, CircleShape)
                                    .size(8.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = dismiss,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "About $author",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Hero image / Closeup
        Box(
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInWindow()
                    portraitRect = Rect(position, coordinates.size.toSize())
                }
                .clickable(enabled = imageUrls.isNotEmpty()) { 
                    showFullScreenImage = true 
                },
            contentAlignment = Alignment.BottomEnd
        ) {
            AuthorAvatar(author = author, imageUrl = imageUrls.firstOrNull(), size = 120.dp)
            
            if (imageUrls.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(32.dp)
                        .offset(x = (-4).dp, y = (-4).dp),
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Simple paragraph splitting for readability
        // Only split if punctuation is followed by a space and a Capital letter
        val paragraphs = remember(about) {
            about.split(Regex("(?<=[.!?])\\s+(?=[A-Z])"))
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(paragraphs) { paragraph ->
                if (paragraph.isNotBlank()) {
                    Text(
                        text = paragraph,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 26.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Text(text = message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}
