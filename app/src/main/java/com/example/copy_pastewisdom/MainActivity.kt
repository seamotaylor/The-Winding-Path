package com.example.copy_pastewisdom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.copy_pastewisdom.data.QuoteRepository
import com.example.copy_pastewisdom.ui.screens.MainScreen
import com.example.copy_pastewisdom.ui.theme.WisdomAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var theme by remember { mutableStateOf(QuoteRepository.getTheme(this)) }
            WisdomAppTheme(theme = theme) {
                MainScreen(
                    context = this, 
                    currentTheme = theme, 
                    onThemeChange = { new ->
                        theme = new
                        QuoteRepository.setTheme(this, new)
                    }
                )
            }
        }
    }
}
