package com.example.copy_pastewisdom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

@Composable
fun FastScrollBar(
    listState: LazyListState,
    totalItems: Int,
    modifier: Modifier = Modifier,
    onScrub: (Int) -> String
) {
    var scrubbing by remember { mutableStateOf(false) }
    var char by remember { mutableStateOf("") }
    var h by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    
    val pct by remember(totalItems) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsCount = layoutInfo.visibleItemsInfo.size
            if (totalItems <= visibleItemsCount || totalItems == 0) 0f
            else {
                val scrollOffset = listState.firstVisibleItemIndex.toFloat()
                (scrollOffset / (totalItems - visibleItemsCount)).coerceIn(0f, 1f)
            }
        }
    }
    
    val handleHeight = 80.dp
    val hh = with(LocalDensity.current) { handleHeight.toPx() }
    
    Box(
        modifier
            .fillMaxHeight()
            .width(32.dp)
            .zIndex(1f)
            .onGloballyPositioned { h = it.size.height.toFloat() }
            .pointerInput(totalItems) {
                detectVerticalDragGestures(
                    onDragStart = { scrubbing = true },
                    onDragEnd = { scrubbing = false },
                    onDragCancel = { scrubbing = false }
                ) { change, _ ->
                    val dragPct = (change.position.y / h).coerceIn(0f, 1f)
                    val idx = (dragPct * totalItems).toInt().coerceIn(0, totalItems - 1)
                    char = onScrub(idx)
                    scope.launch { listState.scrollToItem(idx) }
                }
            }
    ) {
        // Track
        Box(
            Modifier
                .fillMaxHeight()
                .width(2.dp)
                .align(Alignment.Center)
                .background(MaterialTheme.colorScheme.onSurface.copy(0.1f), CircleShape)
        )
        
        // Handle
        Box(
            Modifier
                .fillMaxWidth()
                .height(handleHeight)
                .offset { IntOffset(0, (pct * (h - hh)).toInt()) }
                .padding(horizontal = 8.dp)
                .background(
                    if (scrubbing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    RoundedCornerShape(4.dp)
                )
        )
        
        // Scrub Bubble
        if (scrubbing) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .testTag("scrub_bubble")
                    .offset {
                        IntOffset(
                            -64.dp.toPx().toInt(),
                            (pct * (h - hh)).toInt() // Align top with handle top for simplicity
                        )
                    }
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                Alignment.Center
            ) {
                Text(
                    char,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
