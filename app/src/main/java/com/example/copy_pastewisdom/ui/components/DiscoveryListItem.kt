package com.example.copy_pastewisdom.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DiscoveryListItem(title: String, subtitle: String? = null, imageUrl: String? = null, showAvatar: Boolean = true, onClick: () -> Unit) {
    Surface(
        onClick = onClick, 
        modifier = Modifier.fillMaxWidth(), 
        shape = MaterialTheme.shapes.medium, 
        color = Color.Transparent
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { 
            if (showAvatar) { 
                AuthorAvatar(title.removePrefix("#"), imageUrl, 40.dp)
                Spacer(Modifier.width(16.dp)) 
            }
            Column(Modifier.weight(1f)) { 
                Text(
                    text = title, 
                    style = MaterialTheme.typography.bodyLarge, 
                    fontWeight = FontWeight.Bold,
                    color = Color.White // Hardcode to white for guaranteed visibility on dark background
                ) 
            }
            if (subtitle != null) Text(
                text = subtitle, 
                style = MaterialTheme.typography.labelSmall, 
                color = Color.LightGray, 
                textAlign = TextAlign.End
            )
        }
    }
}
