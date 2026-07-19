package com.example.copy_pastewisdom.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun AnimatedZoomDialog(onDismissRequest: () -> Unit, startRect: Rect? = null, content: @Composable (Float, Float, Offset, () -> Unit) -> Unit) {
    var visible by remember { mutableStateOf(false) }; var exiting by remember { mutableStateOf(false) }
    val windowSize = LocalWindowInfo.current.containerSize
    val sw = windowSize.width.toFloat(); val sh = windowSize.height.toFloat()
    val initScale = if (startRect != null) (startRect.width / sw).coerceAtLeast(startRect.height / sh) else 0.4f
    val initOffset = if (startRect != null) Offset(startRect.center.x - (sw / 2), startRect.center.y - (sh / 2)) else Offset.Zero
    val scale by animateFloatAsState(if (visible && !exiting) 1f else initScale, if (!exiting) spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow) else tween(200), finishedListener = { if (exiting) onDismissRequest() }, label = "")
    val offset by animateOffsetAsState(if (visible && !exiting) Offset.Zero else initOffset, if (!exiting) spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow) else tween(200), label = "")
    val alpha by animateFloatAsState(if (visible && !exiting) 1f else 0f, tween(if (exiting) 150 else 250), label = "")
    LaunchedEffect(Unit) { visible = true }
    Dialog(onDismissRequest = { exiting = true }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = alpha)).clickable { exiting = true }, contentAlignment = Alignment.Center) { content(scale, alpha, offset) { exiting = true } }
    }
}
