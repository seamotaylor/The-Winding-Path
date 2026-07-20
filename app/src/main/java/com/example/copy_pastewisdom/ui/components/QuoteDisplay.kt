package com.example.copy_pastewisdom.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.copy_pastewisdom.data.QuoteItem
import com.example.copy_pastewisdom.data.QuoteRepository
import com.example.copy_pastewisdom.ui.theme.SecondaryText
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDisplay(
    quotes: List<QuoteItem>, 
    isDiscoverMode: Boolean,
    curatedDailyQuote: QuoteItem?, 
    externalSelectedQuote: QuoteItem?, 
    onBrowseClick: () -> Unit
) {
    val displayable = remember(quotes) { quotes.filter { it.quote.isNotBlank() } }
    val daily = curatedDailyQuote ?: if (displayable.isNotEmpty()) displayable[Calendar.getInstance()[Calendar.DAY_OF_YEAR] % displayable.size] else null
    var seed by remember { mutableIntStateOf(0) }
    val shuffled = remember(displayable, seed) { if (displayable.isEmpty()) emptyList() else displayable.shuffled(Random(seed)) }
    
    // Find the daily index by content rather than instance to handle the combined global list
    val dailyIdx = remember(shuffled, daily) { 
        if (daily == null) -1 
        else shuffled.indexOfFirst { it.quote.trim().equals(daily.quote.trim(), ignoreCase = true) }
    }
    val infiniteCount = 1000000
    val initPage = remember(shuffled, dailyIdx) { 
        if (shuffled.isEmpty()) 0 
        else (infiniteCount / 2) - ((infiniteCount / 2) % shuffled.size) + (if (dailyIdx >= 0) dailyIdx else 0)
    }
    val pagerState = rememberPagerState(initPage) { if (shuffled.isEmpty()) 0 else infiniteCount }

    // Force jump to the daily quote on first load once data is ready
    LaunchedEffect(shuffled) {
        if (shuffled.isNotEmpty() && pagerState.currentPage == 0) {
            pagerState.scrollToPage(initPage)
        }
    }

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(pagerState.currentPage) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    val currentItem = if (shuffled.isEmpty()) null else shuffled.getOrNull(pagerState.currentPage % shuffled.size)
    val aboutSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAbout by remember { mutableStateOf(false) }

    LaunchedEffect(externalSelectedQuote) {
        externalSelectedQuote?.let { s -> 
            val idx = shuffled.indexOf(s)
            if (idx >= 0 && shuffled.isNotEmpty()) {
                pagerState.scrollToPage(pagerState.currentPage - (pagerState.currentPage % shuffled.size) + idx)
            }
        }
    }
    
    if (showAbout && currentItem != null) ModalBottomSheet(onDismissRequest = { showAbout = false }, sheetState = aboutSheetState) {
        val authorImages = (quotes.filter { QuoteRepository.normalizeAccents(it.author) == QuoteRepository.normalizeAccents(currentItem.author) && !it.imageUrl.isNullOrBlank() }
            .map { it.imageUrl!! } + 
            listOfNotNull(QuoteRepository.findAuthorImage(currentItem.author, quotes))).distinct()
            
        AuthorAboutContent(
            author = currentItem.author,
            about = QuoteRepository.findAuthorAbout(currentItem.author) ?: "",
            imageUrls = authorImages
        ) { 
            scope.launch { aboutSheetState.hide() }.invokeOnCompletion { showAbout = false } 
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState, 
            modifier = Modifier.weight(1f).fillMaxHeight().testTag("quote_pager"), 
            contentPadding = PaddingValues(vertical = 32.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) { p ->
            if (shuffled.isNotEmpty()) {
                val item = shuffled[p % shuffled.size]
                // Content-based check to handle duplicates between curated and global lists
                val isStrictlyDaily = daily != null && item.quote.trim().equals(daily.quote.trim(), ignoreCase = true)
                QuoteCard(
                    item = item, 
                    imgUrl = QuoteRepository.findAuthorImage(item.author, quotes), 
                    isDaily = isStrictlyDaily, 
                    isDiscoverMode = isDiscoverMode,
                    pager = pagerState, 
                    page = p
                ) { showAbout = true }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 48.dp)) {
            if (shuffled.isNotEmpty() && (pagerState.currentPage % shuffled.size) != dailyIdx && dailyIdx >= 0) {
                TextButton(onClick = { 
                    val nextSeed = seed + 1
                    val newList = displayable.shuffled(Random(nextSeed))
                    val newIdx = if (daily != null) {
                        newList.indexOfFirst { it.quote.trim().equals(daily.quote.trim(), ignoreCase = true) }.coerceAtLeast(0)
                    } else 0
                    seed = nextSeed
                    scope.launch { 
                        if (newList.isNotEmpty()) {
                            pagerState.animateScrollToPage((infiniteCount / 2) - ((infiniteCount / 2) % newList.size) + newIdx) 
                        }
                    } 
                }, modifier = Modifier.padding(bottom = 16.dp)) { Text("Return to Today's Wisdom", style = MaterialTheme.typography.labelLarge.copy(color = SecondaryText)) }
            } else Spacer(Modifier.height(64.dp))
            Button(onBrowseClick, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp)) { Text("Browse All Quotes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
        }
    }
}
