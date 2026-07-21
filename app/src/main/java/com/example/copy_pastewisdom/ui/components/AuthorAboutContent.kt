package com.example.copy_pastewisdom.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

@Composable
fun AuthorAboutContent(author: String, about: String, imageUrls: List<String>, onClose: () -> Unit) {
    var showFull by remember { mutableStateOf(false) }; var rect by remember { mutableStateOf<Rect?>(null) }
    var isGalleryLightMode by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    if (showFull && imageUrls.isNotEmpty()) {
        val inf = 1000000; val pager = rememberPagerState((inf / 2) - ((inf / 2) % imageUrls.size)) { if (imageUrls.size > 1) inf else imageUrls.size }
        AnimatedZoomDialog({ showFull = false }, rect) { s, a, o, d ->
            Box(modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = s, scaleY = s, alpha = a, translationX = o.x, translationY = o.y), Alignment.Center) {
                HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), userScrollEnabled = imageUrls.size > 1) { p ->
                    val actual = p % imageUrls.size
                    var scale by remember { mutableFloatStateOf(1f) }
                    var offset by remember { mutableStateOf(Offset.Zero) }
                    var isError by remember { mutableStateOf(false) }
                    var isLoading by remember { mutableStateOf(true) }
                    
                    Box(modifier = Modifier.fillMaxSize().clip(RectangleShape).pointerInput(Unit) { awaitEachGesture { do { val ev = awaitPointerEvent(); val z = ev.calculateZoom(); val pan = ev.calculatePan(); if (scale > 1f || z != 1f) { ev.changes.forEach { it.consume() }; scale = (scale * z).coerceIn(1f, 5f); if (scale > 1f) offset += pan else offset = Offset.Zero } } while (ev.changes.any { it.pressed }) } }.pointerInput(Unit) { detectTapGestures(onTap = { 
                        if (scale > 1f) { scope.launch { launch { animate(scale, 1f) { v, _ -> scale = v } }; launch { animate(offset.x, 0f) { v, _ -> offset = offset.copy(x = v) } }; launch { animate(offset.y, 0f) { v, _ -> offset = offset.copy(y = v) } } } } else d() 
                    }) }, contentAlignment = Alignment.Center) {
                        
                        // "Dark Studio" Gradient Background for premium feel and contrast support
                        val bgBrush = if (isGalleryLightMode) {
                            Brush.verticalGradient(listOf(Color(0xFFF5F5F5), Color(0xFFE0E0E0)))
                        } else {
                            Brush.radialGradient(listOf(Color(0xFF333333), Color(0xFF000000)))
                        }
                        
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp).background(bgBrush, RoundedCornerShape(8.dp)))

                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrls[actual])
                                .setHeader("User-Agent", "Mozilla/5.0")
                                .crossfade(true)
                                .build(),
                            contentDescription = author, 
                            modifier = Modifier.fillMaxSize().padding(16.dp).graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y), 
                            contentScale = ContentScale.Fit,
                            onLoading = { isLoading = true; isError = false },
                            onError = { isError = true; isLoading = false },
                            onSuccess = { isError = false; isLoading = false }
                        )

                        if (isLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        }
                        
                        if (isError) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.ImageNotSupported, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                                Spacer(Modifier.height(8.dp))
                                Text("Image failed to load", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                if (imageUrls.size > 1) Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp), Arrangement.spacedBy(8.dp)) { repeat(imageUrls.size) { i -> Box(Modifier.padding(2.dp).background(if ((pager.currentPage % imageUrls.size) == i) (if (isGalleryLightMode) Color.DarkGray else Color.White) else (if (isGalleryLightMode) Color.LightGray else Color.White.copy(0.5f)), CircleShape).size(8.dp)) } }
                
                // Controls Overlay
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { isGalleryLightMode = !isGalleryLightMode }) {
                        Icon(Icons.Default.Brightness4, "Toggle Contrast", tint = if (isGalleryLightMode) Color.Black else Color.White)
                    }
                    IconButton(d) { Icon(Icons.Default.Close, null, tint = if (isGalleryLightMode) Color.Black else Color.White) }
                }
            }
        }
    }
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { 
            Text("About $author", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = onClose, modifier = Modifier.semantics { contentDescription = "Close About" }) { 
                Icon(Icons.Default.Close, contentDescription = null) 
            } 
        }
        Spacer(Modifier.height(16.dp)); Box(modifier = Modifier.onGloballyPositioned { rect = Rect(it.positionInWindow(), it.size.toSize()) }.clickable(imageUrls.isNotEmpty()) { showFull = true }, contentAlignment = Alignment.BottomEnd) {
            AuthorAvatar(author, imageUrls.firstOrNull(), 120.dp); if (imageUrls.isNotEmpty()) Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape, modifier = Modifier.size(32.dp).offset((-4).dp, (-4).dp), tonalElevation = 4.dp) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.ZoomIn, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimary) } }
        }
        Spacer(Modifier.height(24.dp)); val paras = remember(about) { if (about.isBlank()) listOf("Mystery thinker.") else about.split(Regex("(?<=[.!?])\\s+(?=[A-Z])")) }
        LazyColumn(Modifier.fillMaxWidth().testTag("author_bio_list"), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(paras) { Text(it, style = MaterialTheme.typography.bodyLarge, lineHeight = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    }
}
