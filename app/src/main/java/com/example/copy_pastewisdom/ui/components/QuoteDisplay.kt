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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDisplay(
    quotes: List<QuoteItem>,
    shuffledQuotes: List<QuoteItem>,
    dailyQuoteIndex: Int,
    isDiscoverMode: Boolean,
    browseSelectedItem: QuoteItem?,
    onBrowseClick: () -> Unit
) {
    if (shuffledQuotes.isEmpty()) return

    val infiniteCount = 1000000
    val initPage = remember(shuffledQuotes, dailyQuoteIndex) { 
        (infiniteCount / 2) - ((infiniteCount / 2) % shuffledQuotes.size) + (if (dailyQuoteIndex >= 0) dailyQuoteIndex else 0)
    }
    
    val pagerState = rememberPagerState(initPage) { infiniteCount }

    // Force jump to the daily quote on first load once data is ready
    LaunchedEffect(shuffledQuotes) {
        if (shuffledQuotes.isNotEmpty() && pagerState.currentPage == 0) {
            pagerState.scrollToPage(initPage)
        }
    }

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(pagerState.currentPage) { 
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) 
    }
    
    val currentItem = shuffledQuotes.getOrNull(pagerState.currentPage % shuffledQuotes.size)
    val aboutSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAbout by remember { mutableStateOf(false) }
    var authorImages by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(browseSelectedItem) {
        browseSelectedItem?.let { s -> 
            val idx = shuffledQuotes.indexOf(s)
            if (idx >= 0) {
                pagerState.scrollToPage(pagerState.currentPage - (pagerState.currentPage % shuffledQuotes.size) + idx)
            }
        }
    }

    LaunchedEffect(currentItem, showAbout) {
        if (showAbout && currentItem != null) {
            authorImages = QuoteRepository.getAllImagesForAuthor(currentItem.author, quotes)
        }
    }
    
    if (showAbout && currentItem != null) {
        ModalBottomSheet(onDismissRequest = { showAbout = false }, sheetState = aboutSheetState) {
            AuthorAboutContent(
                author = currentItem.author,
                about = QuoteRepository.findAuthorAbout(currentItem.author) ?: "",
                imageUrls = authorImages
            ) { 
                scope.launch { aboutSheetState.hide() }.invokeOnCompletion { showAbout = false } 
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), 
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState, 
            modifier = Modifier.weight(1f).fillMaxHeight().testTag("quote_pager"), 
            contentPadding = PaddingValues(vertical = 32.dp), 
            verticalAlignment = Alignment.CenterVertically,
            key = { it } // Use page index as key for stability
        ) { p ->
            val item = shuffledQuotes[p % shuffledQuotes.size]
            val isStrictlyDaily = dailyQuoteIndex >= 0 && (p % shuffledQuotes.size) == dailyQuoteIndex
            
            QuoteCard(
                item = item, 
                imgUrl = QuoteRepository.findAuthorImage(item.author), 
                isDaily = isStrictlyDaily, 
                isDiscoverMode = isDiscoverMode,
                pager = pagerState, 
                page = p,
                onAbout = { showAbout = true }
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, 
            modifier = Modifier.padding(bottom = 48.dp)
        ) {
            if ((pagerState.currentPage % shuffledQuotes.size) != dailyQuoteIndex && dailyQuoteIndex >= 0) {
                TextButton(
                    onClick = { 
                        scope.launch { 
                            pagerState.animateScrollToPage(initPage) 
                        }
                    }, 
                    modifier = Modifier.padding(bottom = 16.dp).testTag("return_today_fab")
                ) { 
                    Text(
                        "Return to Today's Wisdom", 
                        style = MaterialTheme.typography.labelLarge.copy(color = SecondaryText)
                    ) 
                }
            } else {
                Spacer(Modifier.height(64.dp))
            }
            
            Button(
                onClick = onBrowseClick, 
                shape = RoundedCornerShape(16.dp), 
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp)
            ) { 
                Text(
                    "Browse All Quotes", 
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                ) 
            }
        }
    }
}
