package com.example.copy_pastewisdom.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.example.copy_pastewisdom.R
import com.example.copy_pastewisdom.ui.theme.SecondaryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WisdomTopBar(isDiscoverMode: Boolean, onDiscoverToggle: () -> Unit, onRefresh: () -> Unit, onSettings: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { 
            Text(
                text = stringResource(R.string.app_name), 
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Light, color = SecondaryText)
            ) 
        },
        navigationIcon = { 
            IconButton(
                onClick = onDiscoverToggle,
                modifier = Modifier.testTag("discovery_toggle"),
                colors = if (isDiscoverMode) {
                    IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                } else {
                    IconButtonDefaults.iconButtonColors()
                }
            ) { 
                Icon(
                    imageVector = if (isDiscoverMode) Icons.Filled.TravelExplore else Icons.Outlined.TravelExplore, 
                    contentDescription = null, 
                    tint = if (isDiscoverMode) Color.White else SecondaryText
                ) 
            } 
        },
        actions = { 
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null, tint = SecondaryText) }
            IconButton(
                onClick = onSettings,
                modifier = Modifier.testTag("settings_button")
            ) { 
                Icon(Icons.Default.Settings, null, tint = SecondaryText) 
            } 
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}
