package com.example.copy_pastewisdom.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.copy_pastewisdom.data.QuoteItem

@Composable
fun QuoteTrayItem(
    item: QuoteItem,
    showAvatar: Boolean = true,
    showAuthor: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "“${item.quote}”",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontStyle = FontStyle.Italic,
                    lineHeight = 24.sp
                )
            )
            if (showAuthor || showAvatar) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showAvatar) {
                        AuthorAvatar(item.author, null, 24.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    if (showAuthor) {
                        Text(
                            text = "— ${item.author}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
