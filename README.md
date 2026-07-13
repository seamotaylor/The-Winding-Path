# Copy-Paste Wisdom 📖✨

A personal daily wisdom app built with **Kotlin** and **Jetpack Compose**. This app serves as a clean, minimal interface for receiving daily inspiration directly from a Google Sheets-powered backend.

## 🚀 Features

- **Dynamic Source**: Fetches quotes in real-time from a published Google Sheets CSV URL.
- **Offline First**: Automatically caches the latest quotes to ensure the app works perfectly without an internet connection.
- **Daily Notifications**: Receive a random dose of wisdom every morning via Android **WorkManager**.
- **Discovery Tools**:
    - **Interactive Pager**: Swipe through a shuffled deck of wisdom.
    - **Author Browser**: Explore the entire collection grouped by author with a high-performance `LazyColumn` interface.
    - **Author Biographies**: Detailed "About the Author" sections presented in beautiful **Material 3 Bottom Sheets** with intelligent paragraph formatting for readability.
- **Visual Identity**:
    - **Dynamic Avatars**: Every author is assigned a unique, color-coded circular avatar based on their name for instant recognition.
    - **Premium UI**: Refined typography, balanced negative space, and modern Material 3 components like `FilledTonalButton` and `ElevatedCard`.

## 🛠️ Technical Stack

- **UI**: Jetpack Compose (Material 3)
- **Architecture**: Modular functional approach with a centralized `QuoteRepository`
- **Background Tasks**: WorkManager for reliable daily notification scheduling with custom time offsets.
- **Networking**: Kotlin Coroutines for non-blocking fetch and parse logic.
- **Storage**: `SharedPreferences` with Android KTX for efficient local caching.
- **Permissions**: Fully compatible with Android 13+ (TIRAMISU) notification permission flows.

## 📦 Data Schema

The app expects a published Google Sheets CSV with the following columns:
1. **Author**: Name of the individual.
2. **About**: A biography or description of the author.
3. **Quote**: The text of the wisdom.
4. **Image URL (Optional)**: A direct link to an author image.

## 📸 Screenshots

*(Add your screenshots here!)*

## 📝 License

This project is intended for personal use and learning. Feel free to fork and customize your own version of wisdom!
