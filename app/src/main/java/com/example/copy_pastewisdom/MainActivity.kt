/* KEEP YOUR PACKAGE NAME HERE */
package com.example.copy_pastewisdom

import android.Manifest
import android.app.PendingIntent
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
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.copy_pastewisdom.ui.theme.CopyPasteWisdomTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Calendar
import java.util.concurrent.TimeUnit

// Add this for standard icons if not available
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.Refresh


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
}

// --- SECTION 3: WORKER & NOTIFICATIONS ---
class DailyQuoteWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val quotes = QuoteRepository.getQuotesFromCache(applicationContext)
        if (quotes.isNotEmpty()) {
            val randomQuote = quotes.random()
            showNotification(randomQuote)
        }
        return Result.success()
    }

    private fun showNotification(item: QuoteItem) {
        val channelId = "daily_quote_channel"
        
        // Create Notification Channel for API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Wisdom"
            val descriptionText = "Daily morning quotes"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // System launcher icon
            .setContentTitle("From ${item.author}")
            .setContentText("“${item.quote}”")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(applicationContext)) {
            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notify(1001, builder.build())
            }
        }
    }
}

object NotificationScheduler {
    private const val WORK_NAME = "DailyQuoteWork"

    fun scheduleDailyNotification(context: Context) {
        val (hour, minute) = QuoteRepository.getNotificationTime(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        // Calculate delay until selected time
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }
        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyQuoteWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest,
        )
    }

    fun cancelDailyNotification(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
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
        false // 24 hour mode false -> AM/PM
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
fun SettingsContent(
    notificationsEnabled: Boolean,
    notificationTime: Pair<Int, Int>,
    currentTheme: WisdomTheme,
    onThemeChange: (WisdomTheme) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onShowTimePicker: () -> Unit
) {
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
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val displayMinute = minute.toString().padStart(2, '0')
    val timeString = "$displayHour:$displayMinute $amPm"

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

    val shuffledQuotes = remember(quotes) { quotes.shuffled() }
    val dailyIndexInShuffled = remember(shuffledQuotes, dailyQuote) {
        shuffledQuotes.indexOf(dailyQuote).coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(initialPage = dailyIndexInShuffled) { shuffledQuotes.size }
    val scope = rememberCoroutineScope()
    var showAboutDialog by remember { mutableStateOf(value = false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(pagerState.currentPage) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val currentItem = shuffledQuotes[pagerState.currentPage]
    val isDaily = pagerState.currentPage == dailyIndexInShuffled

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
            val index = shuffledQuotes.indexOf(selected)
            if (index >= 0) {
                pagerState.scrollToPage(index)
            }
        }
    }

    if (showAboutDialog) {
        ModalBottomSheet(
            onDismissRequest = { showAboutDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            AuthorAboutContent(
                author = currentItem.author,
                about = authorAbout,
                imageUrls = authorImageUrls,
                onClose = { showAboutDialog = false }
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
            val item = shuffledQuotes[page]
            val authorImageUrlOnCard = remember(item.author, quotes) {
                quotes.find { it.author == item.author && !it.imageUrl.isNullOrBlank() }?.imageUrl
            }
            QuoteCard(
                item = item,
                authorImageUrl = authorImageUrlOnCard,
                isDaily = page == dailyIndexInShuffled,
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
                        scope.launch { pagerState.animateScrollToPage(dailyIndexInShuffled) }
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
                .fillMaxWidth(),
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
                    .padding(top = 8.dp)
                    .alpha(0.9f)
            ) {
                AuthorAvatar(author = item.author, imageUrl = authorImageUrl, size = 40.dp)
                
                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.author,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryText
                        )
                    )
                    TextButton(
                        onClick = onAboutClick,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text(
                            "Learn more",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary
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
    val authors = remember(quotes) { 
        quotes.asSequence().map { it.author }.distinct().sorted().toList() 
    }
    var selectedAuthor by remember { mutableStateOf<String?>(null) }
    var showAboutDialog by remember { mutableStateOf(value = false) }

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
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            AuthorAboutContent(
                author = selectedAuthor!!,
                about = currentAuthorAbout,
                imageUrls = currentAuthorImageUrls,
                onClose = { showAboutDialog = false }
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

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedAuthor == null) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(authors) { author ->
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
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { selectedAuthor = null }) {
                    Text("← Back to Authors")
                }
                if (currentAuthorAbout.isNotBlank()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { showAboutDialog = true }) {
                        Text("About Author")
                    }
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                val authorQuotes = quotes.filter { it.author == selectedAuthor }
                items(authorQuotes) { item ->
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
    val scope = rememberCoroutineScope()

    if (showFullScreenImage && imageUrls.isNotEmpty()) {
        val imagePagerState = rememberPagerState { imageUrls.size }
        Dialog(
            onDismissRequest = { showFullScreenImage = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = imagePagerState,
                    modifier = Modifier.fillMaxSize(),
                    // Disable paging when zoomed in to allow panning
                    userScrollEnabled = true 
                ) { page ->
                    var scale by remember { mutableStateOf(1f) }
                    var offset by remember { mutableStateOf(Offset.Zero) }

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

                                        // If we are zoomed in, or the user is starting a zoom gesture,
                                        // consume the events so the Pager doesn't swipe.
                                        if (scale > 1f || zoomChange != 1f) {
                                            event.changes.forEach { it.consume() }
                                            
                                            scale = (scale * zoomChange).coerceIn(1f, 5f)
                                            if (scale > 1f) {
                                                offset += panChange
                                            } else {
                                                offset = Offset.Zero
                                            }
                                        }
                                        // If scale is 1.0 and no zoom is happening, we don't consume,
                                        // allowing the HorizontalPager to receive the swipe gesture.
                                    } while (event.changes.any { it.pressed })
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { 
                                    if (scale > 1f) {
                                        scope.launch {
                                            launch {
                                                animate(initialValue = scale, targetValue = 1f) { value, _ ->
                                                    scale = value
                                                }
                                            }
                                            launch {
                                                animate(
                                                    initialValue = offset,
                                                    targetValue = Offset.Zero,
                                                    typeConverter = Offset.VectorConverter
                                                ) { value, _ ->
                                                    offset = value
                                                }
                                            }
                                        }
                                    } else {
                                        showFullScreenImage = false 
                                    }
                                })
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrls[page])
                                .addHeader("User-Agent", "CopyPasteWisdom/1.0 (https://github.com/seamotaylor/Copy-Paste-Wisdom) Coil/2.6.0")
                                .crossfade(true)
                                .build(),
                            contentDescription = author,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                ),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                
                // Indicators if more than one image
                if (imageUrls.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(imageUrls.size) { iteration ->
                            val color = if (imagePagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.5f)
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
                    onClick = { showFullScreenImage = false },
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
            modifier = Modifier.clickable(enabled = imageUrls.isNotEmpty()) { 
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
