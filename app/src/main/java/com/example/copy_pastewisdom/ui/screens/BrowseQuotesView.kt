package com.example.copy_pastewisdom.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import com.example.copy_pastewisdom.ui.viewmodels.AuthorDisplayItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseQuotesView(
    quotes: List<QuoteItem>, 
    isDiscoverMode: Boolean,
    displayAuthors: List<AuthorDisplayItem>,
    onDiscoverToggle: () -> Unit,
    onQuoteSelected: (QuoteItem) -> Unit, 
    onBack: () -> Unit
) {
    var selAuth by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    
    val browserPagerState = rememberPagerState { 2 }
    val authState = rememberLazyListState()
    val topState = rememberLazyListState()
    
    // Simple client-side search filtering
    val fAuth = remember(displayAuthors, query) {
        if (query.isBlank()) displayAuthors else displayAuthors.filter { it.name.contains(query, true) }
    }
    
    var globalTags by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var selTop by remember { mutableStateOf<String?>(null) }
    var topQuotes by remember { mutableStateOf<List<QuoteItem>>(emptyList()) }
    
    var loadingTags by remember { mutableStateOf(false) }
    var loadingTopQ by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    
    val fTags = remember(globalTags, query) {
        if (query.isBlank()) globalTags else globalTags.filter { it.first.contains(query, true) }
    }
    
    var authorGlobalQuotes by remember { mutableStateOf<List<QuoteItem>>(emptyList()) }
    var loadingAuthorGlobalQuotes by remember { mutableStateOf(false) }
    var authorImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var isFetchingImages by remember { mutableStateOf(false) }
    var luckyQuote by remember { mutableStateOf<QuoteItem?>(null) }
    var isFetchingLucky by remember { mutableStateOf(false) }
    
    val aboutSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAbout by remember { mutableStateOf(false) }

    LaunchedEffect(browserPagerState.currentPage) { if (browserPagerState.currentPage == 1 && globalTags.isEmpty()) { loadingTags = true; globalTags = QuoteRepository.getAllGlobalTags(); loadingTags = false } }
    LaunchedEffect(selTop) { if (selTop != null) { loadingTopQ = true; topQuotes = QuoteRepository.getGlobalQuotesByTag(selTop!!); loadingTopQ = false } }
    LaunchedEffect(selAuth) { 
        if (selAuth != null) { 
            loadingAuthorGlobalQuotes = true
            isFetchingImages = true
            authorGlobalQuotes = QuoteRepository.getGlobalQuotesForAuthor(selAuth!!)
            authorImages = QuoteRepository.getAllImagesForAuthor(selAuth!!, quotes)
            loadingAuthorGlobalQuotes = false 
            isFetchingImages = false
        } 
    }
    
    BackHandler {
        if (selAuth != null) selAuth = null
        else if (selTop != null) selTop = null
        else onBack()
    }

    if (showAbout && selAuth != null) ModalBottomSheet(onDismissRequest = { showAbout = false }, sheetState = aboutSheetState) {
        AuthorAboutContent(
            author = selAuth!!,
            about = QuoteRepository.findAuthorAbout(selAuth!!) ?: "",
            imageUrls = authorImages
        ) { 
            scope.launch { aboutSheetState.hide() }.invokeOnCompletion { showAbout = false } 
        }
    }

    // Wrap the browser content in a Surface with explicit background and content color
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
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
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                TextButton(onClick = onBack) { 
                    Text("Close", color = MaterialTheme.colorScheme.onBackground) 
                } 
            }
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (selAuth == null && selTop == null) {
                    Column(Modifier.fillMaxSize()) {
                        SecondaryTabRow(selectedTabIndex = browserPagerState.currentPage, containerColor = Color.Transparent, divider = {}) { 
                            Tab(selected = browserPagerState.currentPage == 0, onClick = { scope.launch { browserPagerState.animateScrollToPage(0) } }) { Text("Authors", Modifier.padding(12.dp)) }
                            Tab(selected = browserPagerState.currentPage == 1, onClick = { scope.launch { browserPagerState.animateScrollToPage(1) } }) { Text("Topics", Modifier.padding(12.dp)) } 
                        }
                        OutlinedTextField(
                            value = query, 
                            onValueChange = { query = it }, 
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), 
                            placeholder = { Text(if (browserPagerState.currentPage == 0) "Search authors..." else "Search topics...") }, 
                            leadingIcon = { Icon(Icons.Default.Search, null) }, 
                            singleLine = true, 
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        
                        HorizontalPager(state = browserPagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { pageIndex ->
                            if (pageIndex == 0) {
                                Column(Modifier.fillMaxSize()) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Discover New Wisdom", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        Button(onClick = { scope.launch { isFetchingLucky = true; val res = QuoteRepository.fetchZenQuote(); if (res != null) { if (res.quote == "RATE_LIMIT") Toast.makeText(context, "API Cooldown", Toast.LENGTH_SHORT).show() else luckyQuote = res } else { val fall = QuoteRepository.findRandomGlobalQuote(); if (fall != null) { luckyQuote = fall; Toast.makeText(context, "Using global discovery", Toast.LENGTH_SHORT).show() } }; isFetchingLucky = false } }, enabled = !isFetchingLucky, shape = RoundedCornerShape(8.dp)) { if (isFetchingLucky) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp) else Text("I'm Feeling Lucky") }
                                    }
                                    luckyQuote?.let { item -> Surface(onClick = { clipboard.setText(AnnotatedString("“${item.quote}” — ${item.author}")); Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Column(Modifier.padding(16.dp)) { Text("“${item.quote}”", style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic)); Spacer(Modifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically) { AuthorAvatar(item.author, null, 24.dp); Spacer(Modifier.width(8.dp)); Text("— ${item.author}", style = MaterialTheme.typography.labelLarge) } } } }
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { 
                                        Text(
                                            text = if (isDiscoverMode) "The Expanded Library" else "Curated Anthology", 
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Switch(
                                            checked = isDiscoverMode, 
                                            onCheckedChange = { onDiscoverToggle() },
                                            modifier = Modifier.testTag("browser_discovery_switch")
                                        ) 
                                    }
                                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)); Box(Modifier.fillMaxSize()) {
                                        LazyColumn(
                                            state = authState, 
                                            modifier = Modifier.fillMaxSize().testTag("authors_list"), 
                                            contentPadding = PaddingValues(end = if (isDiscoverMode && query.isBlank()) 32.dp else 0.dp)
                                        ) { 
                                            items(fAuth) { a -> 
                                                DiscoveryListItem(a.name, "${a.quoteCount} quotes", a.imageUrl) { selAuth = a.name }
                                                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)) 
                                            } 
                                        }
                                        if (isDiscoverMode && query.isBlank() && fAuth.isNotEmpty()) {
                                            FastScrollBar(
                                                listState = authState,
                                                totalItems = fAuth.size,
                                                modifier = Modifier.align(Alignment.CenterEnd).testTag("authors_scrollbar")
                                            ) { 
                                                fAuth.getOrNull(it)?.name?.firstOrNull()?.uppercase() ?: "" 
                                            }
                                        }
                                    }
                                }
                            } else Box(Modifier.fillMaxSize()) {
                                if (loadingTags) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                                else { 
                                    LazyColumn(
                                        state = topState, 
                                        modifier = Modifier.fillMaxSize(), 
                                        contentPadding = PaddingValues(end = if (query.isBlank()) 32.dp else 0.dp)
                                    ) { 
                                        items(fTags) { (t, c) -> 
                                            DiscoveryListItem("#$t", "$c quotes", showAvatar = false) { selTop = t }
                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)) 
                                        } 
                                    }
                                    if (query.isBlank() && fTags.isNotEmpty()) {
                                        FastScrollBar(
                                            listState = topState,
                                            totalItems = fTags.size,
                                            modifier = Modifier.align(Alignment.CenterEnd).testTag("topics_scrollbar")
                                        ) { 
                                            fTags.getOrNull(it)?.first?.firstOrNull()?.uppercase() ?: "" 
                                        }
                                    }
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
                                        AuthorAvatar(author = currentAuthor, imageUrl = QuoteRepository.findAuthorImage(currentAuthor), size = 120.dp)
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text(text = currentAuthor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                    Text(text = "Tap portrait for details & photos", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                                }
                            }
                            val cur = quotes.filter { 
                                QuoteRepository.normalizeAccents(it.author) == QuoteRepository.normalizeAccents(currentAuthor) && it.quote.isNotBlank()
                            }
                            val arq = authorGlobalQuotes.filter { aq -> 
                                aq.quote.isNotBlank() && cur.none { it.quote.trim().equals(aq.quote.trim(), true) } 
                            }
                            
                            if (cur.isNotEmpty()) { 
                                item { Text("CURATED ANTHOLOGY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) }
                                items(cur) { item ->
                                    QuoteTrayItem(
                                        item = item, 
                                        showAvatar = false,
                                        showAuthor = false
                                    ) { onQuoteSelected(item) }
                                } 
                            }
                            if (loadingAuthorGlobalQuotes) { item { Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) } } }
                            else if (arq.isNotEmpty()) { 
                                item { Text("THE EXPANDED LIBRARY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) }
                                items(arq) { item -> 
                                    QuoteTrayItem(
                                        item = item, 
                                        showAvatar = false,
                                        showAuthor = false
                                    ) { 
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
}
