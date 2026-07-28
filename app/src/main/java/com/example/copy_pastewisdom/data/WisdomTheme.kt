package com.example.copy_pastewisdom.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

enum class WisdomTheme(val label: String, val primaryColor: Color) {
    Neutral("Neutral", Color(0xFFE0E0E0)),
    Gold("Scholarly", Color(0xFFD4AF37)),
    Sage("Peaceful", Color(0xFF8FBC8F)),
    Blue("Intellectual", Color(0xFF78909C));

    fun toColorInt(): Int = primaryColor.toArgb()
}
