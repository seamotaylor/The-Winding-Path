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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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

// Add this for standard icons if not available
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close


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
        return rawData.lineSequence()
            .drop(1) // Exclude header row
            .filter { it.contains(",") }
            .map { line ->
                // Use a regex that only splits by comma if it's not inside double quotes
                val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                QuoteItem(
                    author = parts.getOrNull(0)?.trim()?.removeSurrounding("\"") ?: "Unknown",
                    about = parts.getOrNull(1)?.trim()?.removeSurrounding("\"") ?: "",
                    quote = parts.getOrNull(2)?.trim()?.removeSurrounding("\"") ?: "",
                    imageUrl = parts.getOrNull(3)?.trim()?.removeSurrounding("\"")
                )
            }
            .filter { it.quote.isNotBlank() }
            .toList()
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
            .setContentTitle("From ${item.author}")
            .setContentText("“${item.quote}”")
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
            dailyWorkRequest,
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
                        context = this,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier, context: Context) {
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
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
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

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState
        ) {
            SettingsContent(
                notificationsEnabled = notificationsEnabled,
                notificationTime = notificationTime,
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
        modifier = modifier,
        topBar = {
            if (!isBrowsing) {
                CenterAlignedTopAppBar(
                    title = { Text("Wisdom", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
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
                        ) { isBrowsing = false }
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
                    onRetry = {
                        scope.launch { fetchQuotes() }
                    },
                )
            }
        }
    }
}

@Composable
fun SettingsContent(
    notificationsEnabled: Boolean,
    notificationTime: Pair<Int, Int>,
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
            modifier = Modifier.padding(bottom = 16.dp)
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

    val currentItem = shuffledQuotes[pagerState.currentPage]
    val isDaily = pagerState.currentPage == dailyIndexInShuffled

    val authorAbout = remember(currentItem.author, quotes) {
        quotes.find { (it.author == currentItem.author) && it.about.isNotBlank() }?.about ?: ""
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
                onClose = { showAboutDialog = false }
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Pager Section (Centered in the remaining space)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) { page ->
                val item = shuffledQuotes[page]
                val pageIsDaily = page == dailyIndexInShuffled

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = if (pageIsDaily) "TODAY'S WISDOM" else "RANDOM WISDOM",
                            style = MaterialTheme.typography.labelLarge.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = if (pageIsDaily) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        )

                        if (pageIsDaily) {
                            Text(
                                text = "Swipe to see more",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Decorative curly quotes
                        Text(
                            text = "“",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = item.quote,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontStyle = FontStyle.Italic,
                                lineHeight = 36.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "”",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.align(Alignment.End)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Independently styled Author
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            AuthorAvatar(author = item.author, size = 32.dp)
                            
                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = item.author,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    letterSpacing = 0.5.sp,
                                ),
                                textAlign = TextAlign.Center,
                            )
                            if (authorAbout.isNotBlank() && page == pagerState.currentPage) {
                                IconButton(
                                    onClick = { showAboutDialog = true },
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "About author",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Stationary Buttons Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
            ) {
                Box(
                    modifier = Modifier.height(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!isDaily) {
                        TextButton(
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(dailyIndexInShuffled) }
                            },
                        ) {
                            Text("Back to Today's Wisdom", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                FilledTonalButton(
                    onClick = onBrowseClick,
                    shape = MaterialTheme.shapes.extraLarge,
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    Text("Browse All Quotes", style = MaterialTheme.typography.titleMedium)
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

    if (showAboutDialog && (selectedAuthor != null)) {
        ModalBottomSheet(
            onDismissRequest = { showAboutDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            AuthorAboutContent(
                author = selectedAuthor!!,
                about = currentAuthorAbout,
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
                    Surface(
                        onClick = { selectedAuthor = author },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AuthorAvatar(author = author, size = 40.dp)
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
fun AuthorAvatar(author: String, size: androidx.compose.ui.unit.Dp) {
    val backgroundColor = remember(author) {
        val colors = listOf(
            Color(0xFFEF5350), Color(0xFFEC407A), Color(0xFFAB47BC),
            Color(0xFF7E57C2), Color(0xFF5C6BC0), Color(0xFF42A5F5),
            Color(0xFF26A69A), Color(0xFF66BB6A), Color(0xFFFFA726)
        )
        colors[author.hashCode().coerceAtLeast(0) % colors.size]
    }

    Surface(
        modifier = Modifier.size(size),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = backgroundColor,
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = author.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun AuthorAboutContent(
    author: String,
    about: String,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp)
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
        
        // Simple paragraph splitting for readability
        val paragraphs = remember(about) {
            about.split(Regex("(?<=[.!?])\\s+"))
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
