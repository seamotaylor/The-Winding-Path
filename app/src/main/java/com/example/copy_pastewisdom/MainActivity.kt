/* KEEP YOUR PACKAGE NAME HERE */
package com.example.copy_pastewisdom

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.copy_pastewisdom.ui.theme.CopyPasteWisdomTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Calendar
import java.util.concurrent.TimeUnit

// --- SECTION 1: DATA MODELS ---
data class QuoteItem(val author: String, val about: String, val quote: String)

sealed class QuoteState {
    object Loading : QuoteState()
    data class Success(val quotes: List<QuoteItem>) : QuoteState()
    data class Error(val message: String) : QuoteState()
}

// --- SECTION 2: REPOSITORY & CACHE LOGIC ---
object QuoteRepository {
    private const val PREFS_NAME = "quote_prefs"
    private const val KEY_CSV = "cached_quotes_csv"
    private const val KEY_NOTIFS_ENABLED = "notifs_enabled"
    private const val KEY_NOTIF_HOUR = "notif_hour"
    private const val KEY_NOTIF_MINUTE = "notif_minute"

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
        return rawData.lines()
            .drop(1) // Exclude header row
            .filter { it.contains(",") }
            .map { line ->
                // Use a regex that only splits by comma if it's not inside double quotes
                val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                QuoteItem(
                    author = parts.getOrNull(0)?.trim()?.removeSurrounding("\"") ?: "Unknown",
                    about = parts.getOrNull(1)?.trim()?.removeSurrounding("\"") ?: "",
                    quote = parts.getOrNull(2)?.trim()?.removeSurrounding("\"") ?: ""
                )
            }
            .filter { it.quote.isNotBlank() }
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
            prefs.getInt(KEY_NOTIF_MINUTE, 0)
        )
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

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // System launcher icon
            .setContentTitle("Your Daily Wisdom")
            .setContentText("\"${item.quote}\"")
            .setSubText(item.author)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
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
            dailyWorkRequest
        )
    }

    fun cancelDailyNotification(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

// --- SECTION 4: MAIN ACTIVITY & UI ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CopyPasteWisdomTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        context = this
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier, context: Context) {
    var uiState by remember { mutableStateOf<QuoteState>(QuoteState.Loading) }
    var notificationsEnabled by remember { 
        mutableStateOf(QuoteRepository.isNotificationsEnabled(context)) 
    }
    var notificationTime by remember {
        mutableStateOf(QuoteRepository.getNotificationTime(context))
    }
    var isBrowsing by remember { mutableStateOf(false) }
    var browseSelectedItem by remember { mutableStateOf<QuoteItem?>(null) }
    val scope = rememberCoroutineScope()

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            notificationsEnabled = true
            QuoteRepository.setNotificationsEnabled(context, true)
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
            val url = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQbAfs-rGaKSvL5F8RZDLr90glOyKsKsTZsDYToO1QcqfpVIIr5XhBAvtuCtJJqz-ZwG191quZMRnwp/pub?gid=0&single=true&output=csv"
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

    Column(modifier = modifier.fillMaxSize()) {
        // Notification Toggle Section
        NotificationSettingsRow(
            enabled = notificationsEnabled,
            onToggle = { isChecked ->
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        notificationsEnabled = true
                        QuoteRepository.setNotificationsEnabled(context, true)
                        NotificationScheduler.scheduleDailyNotification(context)
                    }
                } else {
                    notificationsEnabled = false
                    QuoteRepository.setNotificationsEnabled(context, false)
                    NotificationScheduler.cancelDailyNotification(context)
                }
            }
        )

        // Time Selection Section
        TimeSettingsRow(
            hour = notificationTime.first,
            minute = notificationTime.second,
            onClick = { timePickerDialog.show() }
        )
        
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when (val state = uiState) {
                is QuoteState.Loading -> CircularProgressIndicator()
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
                is QuoteState.Error -> ErrorView(message = state.message, onRetry = {
                    scope.launch { fetchQuotes() }
                })
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

@Composable
fun QuoteDisplay(
    quotes: List<QuoteItem>,
    externalSelectedQuote: QuoteItem? = null,
    onBrowseClick: () -> Unit
) {
    val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    val dailyIndex = dayOfYear % quotes.size
    
    var currentItem by remember { mutableStateOf(quotes[dailyIndex]) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val isDaily = currentItem == quotes[dailyIndex]

    LaunchedEffect(externalSelectedQuote) {
        externalSelectedQuote?.let { currentItem = it }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(text = "About ${currentItem.author}") },
            text = { Text(text = currentItem.about) },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isDaily) "TODAY'S WISDOM" else "RANDOM WISDOM",
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Black
                ),
                color = if (isDaily) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Independently styled Quote
            Text(
                text = "\"${currentItem.quote}\"",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontStyle = FontStyle.Italic,
                    lineHeight = 40.sp,
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Independently styled Author
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "— ${currentItem.author}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 18.sp,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center
                )
                if (currentItem.about.isNotBlank()) {
                    TextButton(onClick = { showAboutDialog = true }) {
                        Text("About", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(onClick = {
                currentItem = quotes.random()
            }) {
                Text("Next Random Quote")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = onBrowseClick) {
                Text("Browse All Quotes")
            }
            
            if (!isDaily) {
                TextButton(onClick = {
                    currentItem = quotes[dailyIndex]
                }) {
                    Text("Back to Today's Quote", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun BrowseQuotesView(
    quotes: List<QuoteItem>,
    onQuoteSelected: (QuoteItem) -> Unit,
    onBack: () -> Unit
) {
    val authors = remember(quotes) { quotes.map { it.author }.distinct().sorted() }
    var selectedAuthor by remember { mutableStateOf<String?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // System Back Press Handling
    BackHandler {
        if (selectedAuthor != null) {
            selectedAuthor = null
        } else {
            onBack()
        }
    }

    val currentAuthorAbout = remember(selectedAuthor, quotes) {
        quotes.find { it.author == selectedAuthor }?.about ?: ""
    }

    if (showAboutDialog && selectedAuthor != null) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(text = "About $selectedAuthor") },
            text = { Text(text = currentAuthorAbout) },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
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
                    Surface(
                        onClick = { selectedAuthor = author },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = author,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
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
                            text = "\"${item.quote}\"",
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
