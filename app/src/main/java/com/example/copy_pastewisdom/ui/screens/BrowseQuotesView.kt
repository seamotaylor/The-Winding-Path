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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.copy_pastewisdom.data.QuoteItem
import com.example.copy_pastewisdom.data.QuoteRepository
import com.example.copy_pastewisdom.ui.components.*
import com.example.copy_pastewisdom.ui.theme.SecondaryText
import com.example.copy_pastewisdom.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseQuotesView(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    
    val browserPagerState = rememberPagerState { 2 }
    
    var globalTags by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var topQuotes by remember { mutableStateOf<List<QuoteItem>>(emptyList()) }
    var loadingTags by remember { mutableStateOf(false) }
    var loadingTopQ by remember { mutableStateOf(false) }
    
    var authorGlobalQuotes by remember { mutableStateOf<List<QuoteItem>>(emptyList()) }
    var loadingAuthorGlobalQuotes by remember { mutableStateOf(false) }
    var authorImages by remember { mutableStateOf<List<String>>(emptyList()) }

    val aboutSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAbout by remember { mutableStateOf(false) }

    LaunchedEffect(browserPagerState.currentPage) { 
        if (browserPagerState.currentPage == 1 && globalTags.isEmpty()) { 
            loadingTags = true
            globalTags = QuoteRepository.getAllGlobalTags()
            loadingTags = false 
        } 
    }
    
    LaunchedEffect(uiState.selectedTopic) { 
        val topic = uiState.selectedTopic
        if (topic != null) { 
            loadingTopQ = true
            topQuotes = QuoteRepository.getGlobalQuotesByTag(topic)
            loadingTopQ = false 
        } 
    }
    
    LaunchedEffect(uiState.selectedAuthor) { 
        val author = uiState.selectedAuthor
        if (author != null) { 
            loadingAuthorGlobalQuotes = true
            authorGlobalQuotes = QuoteRepository.getGlobalQuotesForAuthor(author)
            authorImages = QuoteRepository.getAllImagesForAuthor(author, (uiState.quoteState as? com.example.copy_pastewisdom.data.QuoteState.Success)?.quotes ?: emptyList())
            loadingAuthorGlobalQuotes = false 
        } 
    }
    
    BackHandler {
        if (uiState.selectedAuthor != null) viewModel.selectAuthor(null)
        else if (uiState.selectedTopic != null) viewModel.selectTopic(null)
        else onBack()
    }

    if (showAbout && uiState.selectedAuthor != null) {
        ModalBottomSheet(onDismissRequest = { showAbout = false }, sheetState = aboutSheetState) {
            AuthorAboutContent(
                author = uiState.selectedAuthor!!,
                about = QuoteRepository.findAuthorAbout(uiState.selectedAuthor!!) ?: "",
                imageUrls = authorImages
            ) { 
                scope.launch { aboutSheetState.hide() }.invokeOnCompletion { showAbout = false } 
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
            BrowserHeader(
                selectedAuthor = uiState.selectedAuthor,
                selectedTopic = uiState.selectedTopic,
                onBackClick = {
                    if (uiState.selectedAuthor != null) viewModel.selectAuthor(null)
                    else viewModel.selectTopic(null)
                },
                onClose = onBack
            )
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    uiState.selectedAuthor == null && uiState.selectedTopic == null -> {
                        DiscoveryDashboard(
                            viewModel = viewModel,
                            browserPagerState = browserPagerState,
                            globalTags = globalTags,
                            loadingTags = loadingTags
                        )
                    }
                    uiState.selectedTopic != null -> {
                        TopicDetailView(
                            quotes = topQuotes,
                            isLoading = loadingTopQ,
                            onQuoteSelected = { item ->
                                clipboard.setText(AnnotatedString("“${item.quote}” — ${item.author}"))
                                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    uiState.selectedAuthor != null -> {
                        AuthorDetailView(
                            author = uiState.selectedAuthor!!,
                            curatedQuotes = (uiState.quoteState as? com.example.copy_pastewisdom.data.QuoteState.Success)?.quotes?.filter { 
                                QuoteRepository.normalizeAccents(it.author) == QuoteRepository.normalizeAccents(uiState.selectedAuthor!!) && it.quote.isNotBlank()
                            } ?: emptyList(),
                            globalQuotes = authorGlobalQuotes,
                            isLoadingGlobal = loadingAuthorGlobalQuotes,
                            isDiscoverMode = uiState.isDiscoverMode,
                            onToggleDiscover = { viewModel.toggleDiscoverMode(context) },
                            onShowAbout = { showAbout = true },
                            onQuoteSelected = { item ->
                                viewModel.selectBrowseItem(item)
                            },
                            onCopyQuote = { item ->
                                clipboard.setText(AnnotatedString("“${item.quote}” — ${item.author}"))
                                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BrowserHeader(
    selectedAuthor: String?,
    selectedTopic: String?,
    onBackClick: () -> Unit,
    onClose: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { 
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selectedAuthor != null || selectedTopic != null) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                text = when { 
                    selectedAuthor != null -> "Quotes by $selectedAuthor"
                    selectedTopic != null -> "Topic: #$selectedTopic"
                    else -> "Discover" 
                }, 
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        TextButton(onClick = onClose) { 
            Text("Close", color = MaterialTheme.colorScheme.onBackground) 
        } 
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryDashboard(
    viewModel: MainViewModel,
    browserPagerState: androidx.compose.foundation.pager.PagerState,
    globalTags: List<Pair<String, Int>>,
    loadingTags: Boolean
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    
    val authState = rememberLazyListState()
    val topState = rememberLazyListState()
    
    val filteredAuthors = remember(uiState.displayAuthors, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) uiState.displayAuthors 
        else uiState.displayAuthors.filter { it.name.contains(uiState.searchQuery, true) }
    }
    
    val filteredTags = remember(globalTags, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) globalTags 
        else globalTags.filter { it.first.contains(uiState.searchQuery, true) }
    }

    Column(Modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = browserPagerState.currentPage, containerColor = Color.Transparent, divider = {}) { 
            Tab(selected = browserPagerState.currentPage == 0, onClick = { scope.launch { browserPagerState.animateScrollToPage(0) } }) { Text("Authors", Modifier.padding(12.dp)) }
            Tab(selected = browserPagerState.currentPage == 1, onClick = { scope.launch { browserPagerState.animateScrollToPage(1) } }) { Text("Topics", Modifier.padding(12.dp)) } 
        }
        
        OutlinedTextField(
            value = uiState.searchQuery, 
            onValueChange = { viewModel.setSearchQuery(it) }, 
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("browser_search_field"), 
            placeholder = { Text(if (browserPagerState.currentPage == 0) "Search authors..." else "Search topics...") }, 
            leadingIcon = { Icon(Icons.Default.Search, null) }, 
            singleLine = true, 
            shape = RoundedCornerShape(12.dp)
        )
        
        HorizontalPager(state = browserPagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { pageIndex ->
            if (pageIndex == 0) {
                AuthorTabContent(
                    viewModel = viewModel,
                    authors = filteredAuthors,
                    listState = authState,
                    isLuckyEnabled = !uiState.isFetchingLucky,
                    luckyQuote = uiState.luckyQuote,
                    isDiscoverMode = uiState.isDiscoverMode,
                    onLuckyClick = { viewModel.fetchLuckyQuote(context) },
                    onCopyLucky = { item ->
                        clipboard.setText(AnnotatedString("“${item.quote}” — ${item.author}"))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    },
                    onAuthorClick = { viewModel.selectAuthor(it) },
                    onToggleDiscover = { viewModel.toggleDiscoverMode(context) }
                )
            } else {
                TopicTabContent(
                    tags = filteredTags,
                    listState = topState,
                    isLoading = loadingTags,
                    onTopicClick = { viewModel.selectTopic(it) }
                )
            }
        }
    }
}

@Composable
fun AuthorTabContent(
    viewModel: MainViewModel,
    authors: List<com.example.copy_pastewisdom.ui.viewmodels.AuthorDisplayItem>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isLuckyEnabled: Boolean,
    luckyQuote: QuoteItem?,
    isDiscoverMode: Boolean,
    onLuckyClick: () -> Unit,
    onCopyLucky: (QuoteItem) -> Unit,
    onAuthorClick: (String) -> Unit,
    onToggleDiscover: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Discover New Wisdom", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Button(onClick = onLuckyClick, enabled = isLuckyEnabled) { 
                if (!isLuckyEnabled) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp) 
                else Text("I'm Feeling Lucky") 
            }
        }
        
        luckyQuote?.let { item -> 
            Surface(
                onClick = { onCopyLucky(item) }, 
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), 
                shape = RoundedCornerShape(12.dp), 
                color = MaterialTheme.colorScheme.surfaceVariant
            ) { 
                Column(Modifier.padding(16.dp)) { 
                    Text("“${item.quote}”", style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic))
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) { 
                        AuthorAvatar(item.author, null, 24.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("— ${item.author}", style = MaterialTheme.typography.labelLarge) 
                    } 
                } 
            } 
        }
        
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) { 
            Text(
                text = if (isDiscoverMode) "The Expanded Library" else "Curated Anthology", 
                style = MaterialTheme.typography.titleSmall
            )
            Switch(
                checked = isDiscoverMode, 
                onCheckedChange = { onToggleDiscover() },
                modifier = Modifier.testTag("browser_discovery_switch")
            ) 
        }
        
        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState, 
                modifier = Modifier.fillMaxSize().testTag("authors_list"), 
                contentPadding = PaddingValues(end = if (isDiscoverMode && uiState.searchQuery.isBlank()) 32.dp else 0.dp)
            ) { 
                items(authors, key = { it.name }) { a -> 
                    DiscoveryListItem(a.name, "${a.quoteCount} quotes", a.imageUrl) { onAuthorClick(a.name) }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)) 
                } 
            }
            if (isDiscoverMode && uiState.searchQuery.isBlank() && authors.isNotEmpty()) {
                FastScrollBar(
                    listState = listState,
                    totalItems = authors.size,
                    modifier = Modifier.align(Alignment.CenterEnd).testTag("authors_scrollbar")
                ) { 
                    authors.getOrNull(it)?.name?.firstOrNull()?.uppercase() ?: "" 
                }
            }
        }
    }
}

@Composable
fun TopicTabContent(
    tags: List<Pair<String, Int>>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isLoading: Boolean,
    onTopicClick: (String) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else { 
            LazyColumn(
                state = listState, 
                modifier = Modifier.fillMaxSize(), 
                contentPadding = PaddingValues(end = 32.dp)
            ) { 
                items(tags, key = { it.first }) { (t, c) -> 
                    DiscoveryListItem("#$t", "$c quotes", showAvatar = false) { onTopicClick(t) }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)) 
                } 
            }
            if (tags.isNotEmpty()) {
                FastScrollBar(
                    listState = listState,
                    totalItems = tags.size,
                    modifier = Modifier.align(Alignment.CenterEnd).testTag("topics_scrollbar")
                ) { 
                    tags.getOrNull(it)?.first?.firstOrNull()?.uppercase() ?: "" 
                }
            }
        }
    }
}

@Composable
fun TopicDetailView(
    quotes: List<QuoteItem>,
    isLoading: Boolean,
    onQuoteSelected: (QuoteItem) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(Modifier.fillMaxSize()) { 
                items(quotes) { item -> 
                    QuoteTrayItem(item = item) { onQuoteSelected(item) } 
                } 
            }
        }
    }
}

@Composable
fun AuthorDetailView(
    author: String,
    curatedQuotes: List<QuoteItem>,
    globalQuotes: List<QuoteItem>,
    isLoadingGlobal: Boolean,
    isDiscoverMode: Boolean,
    onToggleDiscover: () -> Unit,
    onShowAbout: () -> Unit,
    onQuoteSelected: (QuoteItem) -> Unit,
    onCopyQuote: (QuoteItem) -> Unit
) {
    val prioritizedCurated = remember(curatedQuotes) { curatedQuotes.filter { it.priority == 3 } }
    val expandedLocal = remember(curatedQuotes) { curatedQuotes.filter { it.priority != 3 } }
    
    LazyColumn(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Column(modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.clip(CircleShape).clickable { onShowAbout() }) {
                    AuthorAvatar(author = author, imageUrl = QuoteRepository.findAuthorImage(author), size = 120.dp)
                }
                Spacer(Modifier.height(12.dp))
                Text(text = author, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(text = "Tap portrait for details & photos", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Text(
                    text = if (isDiscoverMode) "The Expanded Library" else "Curated Anthology",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isDiscoverMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
                Switch(
                    checked = isDiscoverMode,
                    onCheckedChange = { onToggleDiscover() }
                )
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        }
        
        if (prioritizedCurated.isNotEmpty()) { 
            item { Text("CURATED ANTHOLOGY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) }
            items(prioritizedCurated) { item ->
                QuoteTrayItem(
                    item = item, 
                    showAvatar = false,
                    showAuthor = false
                ) { onQuoteSelected(item) }
            } 
        }

        if (isDiscoverMode) {
            if (expandedLocal.isNotEmpty()) {
                item { Text("ARCHIVE COLLECTION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) }
                items(expandedLocal) { item ->
                    QuoteTrayItem(
                        item = item,
                        showAvatar = false,
                        showAuthor = false
                    ) { onQuoteSelected(item) }
                }
            }

            if (isLoadingGlobal) {
                item { Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) } }
            } else if (globalQuotes.isNotEmpty()) {
                item { Text("THE EXPANDED LIBRARY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) }
                items(globalQuotes) { item ->
                    QuoteTrayItem(
                        item = item,
                        showAvatar = false,
                        showAuthor = false
                    ) { onCopyQuote(item) }
                }
            }
        }
    }
}
