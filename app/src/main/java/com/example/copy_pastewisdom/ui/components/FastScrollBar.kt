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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

@Composable
fun FastScrollBar(listState: LazyListState, totalItems: Int, onScrub: (Int) -> String) {
    var scrubbing by remember { mutableStateOf(false) }; var char by remember { mutableStateOf("") }; var h by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope(); val pct by remember(totalItems) { derivedStateOf { if (totalItems <= 1) 0f else (listState.firstVisibleItemIndex.toFloat() / totalItems).coerceIn(0f, 1f) } }
    val hh = with(LocalDensity.current) { 80.dp.toPx() }
    Box(Modifier.fillMaxHeight().width(32.dp).zIndex(1f).onGloballyPositioned { h = it.size.height.toFloat() }.pointerInput(totalItems) { detectVerticalDragGestures(onDragStart = { scrubbing = true }, onDragEnd = { scrubbing = false }, onDragCancel = { scrubbing = false }) { ch, _ -> val idx = ((ch.position.y / h).coerceIn(0f, 1f) * totalItems).toInt().coerceIn(0, totalItems - 1); char = onScrub(idx); scope.launch { listState.scrollToItem(idx) } } }) {
        Box(Modifier.fillMaxHeight().width(2.dp).align(Alignment.Center).background(MaterialTheme.colorScheme.onSurface.copy(0.1f), CircleShape))
        Box(Modifier.fillMaxWidth().height(80.dp).offset { IntOffset(0, (pct * (h - hh)).toInt()) }.padding(horizontal = 8.dp).background(if (scrubbing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp)))
        if (scrubbing) Box(Modifier.align(Alignment.TopEnd).offset { IntOffset(-64.dp.toPx().toInt(), (pct * (h - hh)).toInt() - (80.dp.toPx() / 2).toInt() + (hh / 2).toInt()) }.size(80.dp).background(MaterialTheme.colorScheme.primary, CircleShape), Alignment.Center) { Text(char, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.onPrimary) }
    }
}
