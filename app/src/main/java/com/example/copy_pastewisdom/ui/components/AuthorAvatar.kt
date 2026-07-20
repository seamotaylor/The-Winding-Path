package com.example.copy_pastewisdom.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.copy_pastewisdom.data.QuoteRepository

@Composable
fun AuthorAvatar(author: String, imageUrl: String? = null, size: Dp) {
    val bg = remember(author) { 
        val colors = listOf(
            Color(0xFFEF5350), Color(0xFFEC407A), Color(0xFFAB47BC), 
            Color(0xFF7E57C2), Color(0xFF5C6BC0), Color(0xFF42A5F5), 
            Color(0xFF26A69A), Color(0xFF66BB6A), Color(0xFFFFA726)
        )
        val index = (author.hashCode() and Int.MAX_VALUE) % colors.size
        colors[index]
    }
    var err by remember(imageUrl) { mutableStateOf(false) }
    Surface(modifier = Modifier.size(size), shape = CircleShape, color = bg, tonalElevation = 2.dp) {
        Box(contentAlignment = Alignment.Center) {
            if (!imageUrl.isNullOrBlank() && !err) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .setHeader("User-Agent", "Mozilla/5.0")
                        .crossfade(true)
                        .build(),
                    contentDescription = author,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { err = true }
                )
            }
            if (imageUrl.isNullOrBlank() || err) {
                Text(QuoteRepository.getInitials(author), color = Color.White, fontWeight = FontWeight.Bold, style = if (size < 40.dp) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium)
            }
        }
    }
}
