# Copy-Paste Wisdom 📖✨

A personal daily wisdom app built with **Kotlin** and **Jetpack Compose**. This app serves as a clean, minimal interface for receiving daily inspiration directly from a Google Sheets-powered backend.

## 🚀 Features

- **Dynamic Source**: Fetches quotes in real-time from a published Google Sheets CSV URL.
- **Offline First**: Automatically caches the latest quotes to ensure the app works perfectly without an internet connection.
- **Daily Notifications**: Receive a random dose of wisdom every morning.
- **Customizable Schedule**: Integrated `TimePickerDialog` allows you to choose exactly when you want to be notified.
- **Discovery Tools**:
    - **Random Shuffle**: Instantly jump to a new random quote.
    - **Author Browser**: Explore the entire collection grouped by author using a high-performance `LazyColumn` interface.
- **Material 3 Design**: Clean, responsive UI with independent typography styling for quotes and authors.

## 🛠️ Technical Stack

- **UI**: Jetpack Compose (Material 3)
- **Language**: Kotlin
- **Architecture**: Modular functional approach with a centralized `QuoteRepository`
- **Background Tasks**: Android WorkManager for reliable daily notification scheduling
- **Networking**: Coroutines with `Dispatchers.IO` for non-blocking network calls
- **Storage**: `SharedPreferences` with Android KTX for efficient local caching
- **Permissions**: Graceful handling of Android 13+ POST_NOTIFICATIONS permissions

## 📦 How to Use Your Own Data

The app is currently configured to read from a Google Sheet with two columns:
- **Column A**: Author Name
- **Column B**: Quote Text

To use your own quotes, simply update the `csvUrl` variable in `MainActivity.kt` with your published CSV link from Google Sheets.

## 📸 Screenshots

*(Add your screenshots here!)*

## 📝 License

This project is intended for personal use and learning. feel free to fork and customize your own version of wisdom!
