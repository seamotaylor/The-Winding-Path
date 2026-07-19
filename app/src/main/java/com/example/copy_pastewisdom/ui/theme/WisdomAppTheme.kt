package com.example.copy_pastewisdom.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.copy_pastewisdom.data.WisdomTheme

val DarkBackground = Color(0xFF121212)
val PrimaryText = Color(0xFFEDEDED)
val SecondaryText = Color(0xFFBDBDBD)
val CardSurface = Color(0xFF1E1E1E)

@Composable
fun WisdomAppTheme(theme: WisdomTheme = WisdomTheme.Neutral, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = theme.primaryColor,
            onPrimary = Color.Black,
            secondary = theme.primaryColor.copy(alpha = 0.8f),
            onSecondary = Color.Black,
            background = DarkBackground,
            onBackground = PrimaryText,
            surface = CardSurface,
            onSurface = PrimaryText,
            surfaceVariant = Color(0xFF2C2C2C),
            onSurfaceVariant = SecondaryText
        ),
        content = content
    )
}
