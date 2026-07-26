package com.example.copy_pastewisdom.ui.components

import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil.compose.AsyncImage
import com.example.copy_pastewisdom.R
import com.example.copy_pastewisdom.data.WisdomTheme
import com.example.copy_pastewisdom.ui.theme.SecondaryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    notifEnabled: Boolean,
    notifTime: Pair<Int, Int>,
    notifExpanded: Boolean,
    currentTheme: WisdomTheme,
    onThemeChange: (WisdomTheme) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onShowTimePicker: () -> Unit,
    onToggleExpandedNotifs: (Boolean) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        SettingsContent(
            notifEnabled = notifEnabled,
            notifTime = notifTime,
            notifExpanded = notifExpanded,
            currentTheme = currentTheme,
            onThemeChange = onThemeChange,
            onToggle = onToggleNotifications,
            onShowTime = onShowTimePicker,
            onExpandedToggle = onToggleExpandedNotifs
        )
    }
}

@Composable
private fun SettingsContent(
    notifEnabled: Boolean,
    notifTime: Pair<Int, Int>,
    notifExpanded: Boolean,
    currentTheme: WisdomTheme,
    onThemeChange: (WisdomTheme) -> Unit,
    onToggle: (Boolean) -> Unit,
    onShowTime: () -> Unit,
    onExpandedToggle: (Boolean) -> Unit
) {
    var showZoom by remember { mutableStateOf(false) }
    var iconRect by remember { mutableStateOf<Rect?>(null) }
    
    if (showZoom) {
        AnimatedZoomDialog(
            onDismissRequest = { showZoom = false },
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
                    Icon(Icons.Default.Close, null, tint = Color.White)
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
            "Settings",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Text(
            "Appearance",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        ThemeSelector(currentTheme, onThemeChange)
        
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(
            Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(0.1f)
        )
        
        Text(
            "Reminders",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        NotificationSettingsRow(notifEnabled, onToggle)
        NotificationContentRow(notifExpanded, onExpandedToggle)
        TimeSettingsRow(notifTime.first, notifTime.second, onShowTime)
        
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(
            Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(0.1f)
        )
        
        Text(
            "About",
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
                    .onGloballyPositioned {
                        iconRect = Rect(it.positionInWindow(), it.size.toSize())
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showZoom = true }
            )
            Spacer(Modifier.width(16.dp))
            Text(
                stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText
            )
        }
    }
}

@Composable
private fun ThemeSelector(currentTheme: WisdomTheme, onThemeChange: (WisdomTheme) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(WisdomTheme.entries) { theme ->
            val isSelected = theme == currentTheme
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onThemeChange(theme) }
                    .testTag("theme_item_${theme.name}")
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
                                .background(Color.Black.copy(0.3f), CircleShape)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.Black)
                        }
                    }
                }
                Text(
                    theme.label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else SecondaryText
                )
            }
        }
    }
}

@Composable
private fun NotificationSettingsRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text("Daily Notifications", style = MaterialTheme.typography.titleMedium)
            Text("Receive wisdom every morning", style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            modifier = Modifier.testTag("notification_switch")
        )
    }
}

@Composable
private fun NotificationContentRow(expanded: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text("Daily Wisdom Source", style = MaterialTheme.typography.titleMedium)
            Text(
                if (expanded) "Includes full library" else "Curated wisdom only",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(
            checked = expanded,
            onCheckedChange = onToggle,
            modifier = Modifier.testTag("notification_expanded_switch")
        )
    }
}

@Composable
private fun TimeSettingsRow(hour: Int, minute: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Notification Time", style = MaterialTheme.typography.titleSmall)
            Text(
                "Currently set to ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Button(onClick = onClick) {
            Text("Set Time")
        }
    }
}
