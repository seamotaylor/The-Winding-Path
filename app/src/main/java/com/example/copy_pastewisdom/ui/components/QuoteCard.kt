package com.example.copy_pastewisdom.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.copy_pastewisdom.data.QuoteItem
import com.example.copy_pastewisdom.ui.theme.CardSurface
import com.example.copy_pastewisdom.ui.theme.PrimaryText
import com.example.copy_pastewisdom.ui.theme.SecondaryText
import kotlin.math.absoluteValue

@Composable
fun QuoteCard(
    item: QuoteItem, 
    imgUrl: String?, 
    isDaily: Boolean, 
    isDiscoverMode: Boolean,
    pager: PagerState, 
    page: Int, 
    onAbout: () -> Unit
) {
    val headerTitle = when {
        isDaily -> "TODAY'S WISDOM"
        isDiscoverMode -> "THE EXPANDED LIBRARY"
        else -> "CURATED ANTHOLOGY"
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .graphicsLayer { 
                val pageOffset = ((pager.currentPage - page) + pager.currentPageOffsetFraction)
                val absOffset = pageOffset.absoluteValue
                
                // Scale effect
                val scale = lerp(0.85f, 1f, 1f - absOffset.coerceIn(0f, 1f))
                scaleX = scale
                scaleY = scale
                
                // Fade effect
                alpha = lerp(0.4f, 1f, 1f - absOffset.coerceIn(0f, 1f))
                
                // Rotation effect
                rotationZ = pageOffset * 5f
                
                // Elevation-like shadow adjustment via alpha or translation if needed
                translationY = absOffset * 40f
            }, 
        colors = CardDefaults.elevatedCardColors(containerColor = CardSurface), 
        elevation = CardDefaults.elevatedCardElevation(12.dp), 
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            Modifier.padding(32.dp).fillMaxWidth().verticalScroll(rememberScrollState()), 
            horizontalAlignment = Alignment.CenterHorizontally, 
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { 
                Text(
                    text = headerTitle, 
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 2.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = if (isDaily) MaterialTheme.colorScheme.primary else SecondaryText
                    )
                )
                if (isDaily) Text(
                    text = "Swipe for more", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = SecondaryText.copy(0.8f), 
                    modifier = Modifier.padding(top = 4.dp)
                ) 
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) { 
                Text("“", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary.copy(0.7f), modifier = Modifier.align(Alignment.Start))
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
                Text("”", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary.copy(0.7f), modifier = Modifier.align(Alignment.End)) 
            }
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.Center, 
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .alpha(0.9f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onAbout() }
                    .padding(8.dp)
            ) { 
                AuthorAvatar(item.author, imgUrl, 150.dp)
                Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f, false)) { 
                    Text(item.author, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = PrimaryText))
                    Text(
                        text = "Learn more", 
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary, 
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    ) 
                } 
            }
        }
    }
}
