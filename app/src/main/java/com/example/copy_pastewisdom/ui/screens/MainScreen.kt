package com.example.copy_pastewisdom.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.copy_pastewisdom.R
import com.example.copy_pastewisdom.data.QuoteRepository
import com.example.copy_pastewisdom.data.QuoteState
import com.example.copy_pastewisdom.data.WisdomTheme
import com.example.copy_pastewisdom.logic.NotificationScheduler
import com.example.copy_pastewisdom.ui.components.*
import com.example.copy_pastewisdom.ui.theme.SecondaryText
import com.example.copy_pastewisdom.ui.viewmodels.MainViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    context: Context, 
    currentTheme: WisdomTheme, 
    onThemeChange: (WisdomTheme) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var showSettings by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            viewModel.setNotificationsEnabled(true)
            QuoteRepository.setNotificationsEnabled(context, true)
            NotificationScheduler.scheduleDailyNotification(context)
        }
    }
    
    val timePicker = TimePickerDialog(context, { _, h, m -> 
        viewModel.setNotificationTime(h, m)
        QuoteRepository.setNotificationTime(context, h, m)
        if (uiState.notificationsEnabled) NotificationScheduler.scheduleDailyNotification(context)
    }, uiState.notificationTime.first, uiState.notificationTime.second, true)

    // Initial load
    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    if (showSettings) ModalBottomSheet(onDismissRequest = { showSettings = false }, sheetState = sheetState) {
        SettingsContent(
            notifEnabled = uiState.notificationsEnabled,
            notifTime = uiState.notificationTime,
            currentTheme = currentTheme,
            onThemeChange = onThemeChange,
            onToggle = { checked ->
                if (checked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setNotificationsEnabled(true)
                        QuoteRepository.setNotificationsEnabled(context, true)
                        NotificationScheduler.scheduleDailyNotification(context)
                    }
                } else {
                    viewModel.setNotificationsEnabled(false)
                    QuoteRepository.setNotificationsEnabled(context, false)
                    NotificationScheduler.cancelDailyNotification(context)
                }
            },
            onShowTime = { timePicker.show() }
        )
    }

    Scaffold(
        topBar = {
            if (!uiState.isBrowsing) WisdomTopBar(
                isDiscoverMode = uiState.isDiscoverMode,
                onDiscoverToggle = { viewModel.toggleDiscoverMode() },
                onRefresh = { viewModel.fetchQuotes(context) },
                onSettings = { showSettings = true }
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, Color.Black)))
                .padding(inner),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = if (uiState.isLoadingGlobal) QuoteState.Loading else uiState.quoteState,
                transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(300)) },
                label = "MainContentAnimation"
            ) { state ->
                when (state) {
                    is QuoteState.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    is QuoteState.Success -> {
                        if (uiState.isBrowsing) {
                            BrowseQuotesView(
                                quotes = state.quotes,
                                isDiscoverMode = uiState.isDiscoverMode,
                                globalAuthors = uiState.globalAuthors,
                                onDiscoverToggle = { viewModel.toggleDiscoverMode() },
                                onQuoteSelected = { viewModel.selectBrowseItem(it) },
                                onBack = { viewModel.setBrowsing(false) }
                            )
                        } else {
                            QuoteDisplay(
                                quotes = if (uiState.isDiscoverMode) (state.quotes + uiState.globalQuotes).distinctBy { it.quote.trim().lowercase() } else state.quotes,
                                isDiscoverMode = uiState.isDiscoverMode,
                                curatedDailyQuote = remember(state.quotes) { 
                                    val q = state.quotes.filter { it.quote.isNotBlank() }
                                    if (q.isEmpty()) null else q[Calendar.getInstance()[Calendar.DAY_OF_YEAR] % q.size] 
                                },
                                externalSelectedQuote = uiState.browseSelectedItem,
                                onBrowseClick = { viewModel.setBrowsing(true) }
                            )
                        }
                    }
                    is QuoteState.Error -> ErrorView(state.message) { viewModel.fetchQuotes(context) }
                }
            }
        }
    }
}

@Composable
fun SettingsContent(notifEnabled: Boolean, notifTime: Pair<Int, Int>, currentTheme: WisdomTheme, onThemeChange: (WisdomTheme) -> Unit, onToggle: (Boolean) -> Unit, onShowTime: () -> Unit) {
    var showF by remember { mutableStateOf(false) }; var iconRect by remember { mutableStateOf<Rect?>(null) }
    if (showF) AnimatedZoomDialog({ showF = false }, iconRect) { s, a, o, d ->
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(model = R.mipmap.ic_launcher, contentDescription = null, modifier = Modifier.fillMaxWidth().padding(16.dp).graphicsLayer(scaleX = s, scaleY = s, alpha = a, translationX = o.x, translationY = o.y), contentScale = ContentScale.Fit)
            IconButton(onClick = d, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).alpha(a)) { Icon(Icons.Default.Close, null, tint = Color.White) }
        }
    }
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 24.dp))
        Text("Appearance", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        ThemeSelector(currentTheme, onThemeChange)
        Spacer(Modifier.height(16.dp)); HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(0.1f))
        Text("Reminders", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        NotificationSettingsRow(notifEnabled, onToggle)
        TimeSettingsRow(notifTime.first, notifTime.second, onShowTime)
        Spacer(Modifier.height(16.dp)); HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(0.1f))
        Text("About", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = R.mipmap.ic_launcher, contentDescription = null, modifier = Modifier.size(64.dp).onGloballyPositioned { iconRect = Rect(it.positionInWindow(), it.size.toSize()) }.clip(RoundedCornerShape(12.dp)).clickable { showF = true })
            Spacer(Modifier.width(16.dp)); Text("The labyrinth represents the philosophical journey—a single, winding path to the center of truth.", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
        }
    }
}

@Composable
fun ThemeSelector(currentTheme: WisdomTheme, onThemeChange: (WisdomTheme) -> Unit) {
    LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
        items(WisdomTheme.entries) { theme ->
            val isSelected = theme == currentTheme
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onThemeChange(theme) }) {
                Box(modifier = Modifier.size(48.dp).background(theme.primaryColor, CircleShape).padding(4.dp)) {
                    if (isSelected) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.3f), CircleShape).padding(8.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, tint = Color.Black) }
                }
                Text(theme.label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp), color = if (isSelected) MaterialTheme.colorScheme.primary else SecondaryText)
            }
        }
    }
}

@Composable
fun NotificationSettingsRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column { Text("Daily Notifications", style = MaterialTheme.typography.titleMedium); Text("Receive wisdom every morning", style = MaterialTheme.typography.bodySmall) }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
fun TimeSettingsRow(hour: Int, minute: Int, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column { Text("Notification Time", style = MaterialTheme.typography.titleSmall); Text("Currently set to ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}", style = MaterialTheme.typography.bodySmall) }
        Button(onClick = onClick) { Text("Set Time") }
    }
}
