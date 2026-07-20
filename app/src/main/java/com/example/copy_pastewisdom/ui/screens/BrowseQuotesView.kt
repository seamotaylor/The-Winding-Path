package com.example.copy_pastewisdom.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.copy_pastewisdom.data.QuoteItem
import com.example.copy_pastewisdom.data.QuoteRepository
import com.example.copy_pastewisdom.ui.components.*
import com.example.copy_pastewisdom.ui.theme.SecondaryText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseQuotesView(
    quotes: List<QuoteItem>, 
    isDiscoverMode: Boolean,
    globalAuthors: List<Pair<String, Int>>,
    onDiscoverToggle: () -> Unit,
    onQuoteSelected: (QuoteItem) -> Unit, 
    onBack: () -> Unit
) {
    val curatedCounts = remember(quotes) { quotes.asSequence().filter { it.quote.isNotBlank() }.groupingBy { it.author.trim() }.eachCount() }
    
    // Fast production calculation using pre-grouped global authors
    val countsMap by produceState(initialValue = curatedCounts, curatedCounts, globalAuthors, isDiscoverMode) {
        value = withContext(Dispatchers.Default) {
            val m = curatedCounts.toMutableMap()
            if (isDiscoverMode) {
                val curatedKeyLookup = m.keys.associateBy { QuoteRepository.normalizeAccents(it) }
                globalAuthors.forEach { (n, c) -> 
                    val normN = QuoteRepository.normalizeAccents(n)
                    val existingKey = curatedKeyLookup[normN]
                    if (existingKey != null) { m[existingKey] = (m[existingKey] ?: 0) + c } else { m[n.trim()] = c }
                }
            }
            m
        }
    }
    
    val fullList = remember(countsMap) { countsMap.keys.sortedBy { it.lowercase() } }
    var selAuth by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    
    val browserPagerState = rememberPagerState { 2 }
    val authState = rememberLazyListState(); val topState = rememberLazyListState()
    
    var globalTags by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var selTop by remember { mutableStateOf<String?>(null) }
    var topQuotes by remember { mutableStateOf<List<QuoteItem>>(emptyList()) }
    
    var loadingTags by remember { mutableStateOf(false) }
    var loadingTopQ by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope(); val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    
    // Background list filtering
    val fAuth by produceState(initialValue = emptyList<String>(), fullList, query) {
        value = withContext(Dispatchers.Default) { if (query.isBlank()) fullList else fullList.filter { it.contains(query, true) } }
    }
    val fTags by produceState(initialValue = emptyList<Pair<String, Int>>(), globalTags, query) {
        value = withContext(Dispatchers.Default) { if (query.isBlank()) globalTags else globalTags.filter { it.first.contains(query, true) } }
    }
    
    var authorGlobalQuotes by remember { mutableStateOf<List<QuoteItem>>(emptyList()) }
    var loadingAuthorGlobalQuotes by remember { mutableStateOf(false) }
    var luckyQuote by remember { mutableStateOf<QuoteItem?>(null) }
    var isFetchingLucky by remember { mutableStateOf(false) }
    
    val aboutSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAbout by remember { mutableStateOf(false) }

    LaunchedEffect(browserPagerState.currentPage) { if (browserPagerState.currentPage == 1 && globalTags.isEmpty()) { loadingTags = true; globalTags = QuoteRepository.getAllGlobalTags(); loadingTags = false } }
    LaunchedEffect(selTop) { if (selTop != null) { loadingTopQ = true; topQuotes = QuoteRepository.getGlobalQuotesByTag(selTop!!); loadingTopQ = false } }
    LaunchedEffect(selAuth) { if (selAuth != null) { loadingAuthorGlobalQuotes = true; authorGlobalQuotes = QuoteRepository.getGlobalQuotesForAuthor(selAuth!!); loadingAuthorGlobalQuotes = false } }
    
    BackHandler {
        if (selAuth != null) selAuth = null
        else if (selTop != null) selTop = null
        else onBack()
    }

    if (showAbout && selAuth != null) ModalBottomSheet(onDismissRequest = { showAbout = false }, sheetState = aboutSheetState) {
        val authorImages = (quotes.filter { QuoteRepository.normalizeAccents(it.author) == QuoteRepository.normalizeAccents(selAuth!!) && !it.imageUrl.isNullOrBlank() }
            .map { it.imageUrl!! } + 
            listOfNotNull(QuoteRepository.findAuthorImage(selAuth!!, quotes))).distinct()
            
        AuthorAboutContent(
            author = selAuth!!,
            about = QuoteRepository.findAuthorAbout(selAuth!!) ?: "",
            imageUrls = authorImages
        ) { 
            scope.launch { aboutSheetState.hide() }.invokeOnCompletion { showAbout = false } 
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selAuth != null || selTop != null) {
                    IconButton(onClick = { if (selAuth != null) selAuth = null else selTop = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(
                    text = when { 
                        selAuth != null -> "Quotes by $selAuth"
                        selTop != null -> "Topic: #$selTop"
                        else -> "Discover" 
                    }, 
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            TextButton(onBack) { Text("Close") } 
        }
        
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (selAuth == null && selTop == null) {
                Column(Modifier.fillMaxSize()) {
                    SecondaryTabRow(selectedTabIndex = browserPagerState.currentPage, containerColor = Color.Transparent, divider = {}) { 
                        Tab(selected = browserPagerState.currentPage == 0, onClick = { scope.launch { browserPagerState.animateScrollToPage(0) } }) { Text("Authors", Modifier.padding(12.dp)) }
                        Tab(selected = browserPagerState.currentPage == 1, onClick = { scope.launch { browserPagerState.animateScrollToPage(1) } }) { Text("Topics", Modifier.padding(12.dp)) } 
                    }
                    OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(vertical = 8.dp), placeholder = { Text(if (browserPagerState.currentPage == 0) "Search authors..." else "Search topics...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(12.dp))
                    Spacer(Modifier.height(8.dp))
                    
                    HorizontalPager(state = browserPagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { pageIndex ->
                        if (pageIndex == 0) {
                            Column(Modifier.fillMaxSize()) {
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Discover New Wisdom", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    Button(onClick = { scope.launch { isFetchingLucky = true; val res = QuoteRepository.fetchZenQuote(); if (res != null) { if (res.quote == "RATE_LIMIT") Toast.makeText(context, "API Cooldown", Toast.LENGTH_SHORT).show() else luckyQuote = res } else { val fall = QuoteRepository.findRandomGlobalQuote(); if (fall != null) { luckyQuote = fall; Toast.makeText(context, "Using global discovery", Toast.LENGTH_SHORT).show() } }; isFetchingLucky = false } }, enabled = !isFetchingLucky, shape = RoundedCornerShape(8.dp)) { if (isFetchingLucky) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp) else Text("I'm Feeling Lucky") }
                                }
                                luckyQuote?.let { item -> Surface(onClick = { clipboard.setText(AnnotatedString("“${item.quote}” — ${item.author}")); Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Column(Modifier.padding(16.dp)) { Text("“${item.quote}”", style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic)); Spacer(Modifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically) { AuthorAvatar(item.author, null, 24.dp); Spacer(Modifier.width(8.dp)); Text("— ${item.author}", style = MaterialTheme.typography.labelLarge) } } } }
                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) { 
                                    Text("Global Discovery", style = MaterialTheme.typography.titleSmall)
                                    Switch(
                                        checked = isDiscoverMode, 
                                        onCheckedChange = { onDiscoverToggle() },
                                        modifier = Modifier.testTag("browser_discovery_switch")
                                    ) 
                                }
                                HorizontalDivider(Modifier.padding(vertical = 8.dp)); Box(Modifier.fillMaxSize()) {
                                    LazyColumn(
                                        state = authState, 
                                        modifier = Modifier.fillMaxSize().testTag("authors_list"), 
                                        contentPadding = PaddingValues(end = if (isDiscoverMode && query.isBlank()) 32.dp else 0.dp)
                                    ) { 
                                        items(fAuth) { a -> 
                                            DiscoveryListItem(a, countsMap[a]?.let { "$it quotes" }, QuoteRepository.findAuthorImage(a, quotes)) { selAuth = a }
                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp)) 
                                        } 
                                    }
                                    if (isDiscoverMode && query.isBlank() && fAuth.isNotEmpty()) { Box(Modifier.align(Alignment.CenterEnd)) { FastScrollBar(authState, fAuth.size) { fAuth.getOrNull(it)?.firstOrNull()?.uppercase() ?: "" } } }
                                }
                            }
                        } else Box(Modifier.fillMaxSize()) {
                            if (loadingTags) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                            else { LazyColumn(state = topState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(end = if (query.isBlank()) 32.dp else 0.dp)) { 
                                    items(fTags) { (t, c) -> DiscoveryListItem("#$t", "$c quotes", showAvatar = false) { selTop = t }; HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp)) } 
                                }
                                if (query.isBlank() && fTags.isNotEmpty()) { Box(Modifier.align(Alignment.CenterEnd)) { FastScrollBar(topState, fTags.size) { fTags.getOrNull(it)?.first?.firstOrNull()?.uppercase() ?: "" } } }
                            }
                        }
                    }
                }
            } else if (selTop != null) {
                Column(Modifier.fillMaxSize()) {
                    if (loadingTopQ) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                    else LazyColumn(Modifier.fillMaxSize()) { items(topQuotes) { item -> QuoteTrayItem(item = item) { clipboard.setText(AnnotatedString("“${item.quote}” — ${item.author}")); Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show() } } }
                }
            } else {
                val currentAuthor = selAuth
                LazyColumn(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (currentAuthor != null) {
                        item {
                            Column(modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(modifier = Modifier.clip(CircleShape).clickable { showAbout = true }) {
                                    AuthorAvatar(author = currentAuthor, imageUrl = QuoteRepository.findAuthorImage(currentAuthor, quotes), size = 120.dp)
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(text = currentAuthor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(text = "Tap portrait for details & photos", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                            }
                        }
                        val cur = quotes.filter { QuoteRepository.normalizeAccents(it.author) == QuoteRepository.normalizeAccents(currentAuthor) }
                        val arq = authorGlobalQuotes.filter { aq -> cur.none { it.quote.trim().equals(aq.quote.trim(), true) } }
                        
                        if (cur.isNotEmpty()) { 
                            item { Text("CURATED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) }
                            items(cur) { item ->
                                QuoteTrayItem(item = item, showAvatar = false) { onQuoteSelected(item) }
                            } 
                        }
                        if (loadingAuthorGlobalQuotes) { item { Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) } } }
                        else if (arq.isNotEmpty()) { 
                            item { Text("GLOBAL DISCOVERY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) }
                            items(arq) { item -> 
                                QuoteTrayItem(item = item, showAvatar = false) { 
                                    clipboard.setText(AnnotatedString("“${item.quote}” — ${item.author}")); Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show() 
                                } 
                            } 
                        }
                    }
                }
            }
        }
    }
}
