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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.copy_pastewisdom.data.QuoteRepository
import com.example.copy_pastewisdom.data.QuoteState
import com.example.copy_pastewisdom.data.WisdomTheme
import com.example.copy_pastewisdom.logic.NotificationScheduler
import com.example.copy_pastewisdom.ui.components.*
import com.example.copy_pastewisdom.ui.viewmodels.MainViewModel

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
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
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

    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    if (showSettings) {
        SettingsBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState,
            notifEnabled = uiState.notificationsEnabled,
            notifTime = uiState.notificationTime,
            notifExpanded = uiState.notifExpanded,
            currentTheme = currentTheme,
            onThemeChange = onThemeChange,
            onToggleNotifications = { checked ->
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
            onShowTimePicker = { timePicker.show() },
            onToggleExpandedNotifs = { viewModel.setNotifExpanded(context, it) }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, Color.Black)))
    ) {
        AnimatedContent(
            targetState = uiState.isBrowsing,
            transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(300)) },
            label = "MainContentAnimation"
        ) { isBrowsing ->
            if (isBrowsing) {
                BrowseQuotesView(
                    viewModel = viewModel,
                    onBack = { viewModel.setBrowsing(false) }
                )
            } else {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        WisdomTopBar(
                            isDiscoverMode = uiState.isDiscoverMode,
                            onDiscoverToggle = { viewModel.toggleDiscoverMode(context) },
                            onRefresh = { viewModel.fetchQuotes(context) },
                            onSettings = { showSettings = true }
                        )
                    }
                ) { inner ->
                    Box(Modifier.padding(inner).fillMaxSize(), contentAlignment = Alignment.Center) {
                        when (val state = uiState.quoteState) {
                            is QuoteState.Loading -> CircularProgressIndicator()
                            is QuoteState.Success -> {
                                QuoteDisplay(
                                    quotes = uiState.shuffledQuotes,
                                    shuffledQuotes = uiState.shuffledQuotes,
                                    dailyQuoteIndex = uiState.dailyQuoteIndex,
                                    isDiscoverMode = uiState.isDiscoverMode,
                                    browseSelectedItem = uiState.browseSelectedItem,
                                    onBrowseClick = { viewModel.setBrowsing(true) }
                                )
                            }
                            is QuoteState.Error -> ErrorView(state.message) { viewModel.fetchQuotes(context) }
                        }
                    }
                }
            }
        }
        
        if (uiState.isLoadingGlobal) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .testTag("global_loading_overlay"), 
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
