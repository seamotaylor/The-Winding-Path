# Copy-Paste Wisdom 📖✨

A personal daily wisdom app built with **Kotlin** and **Jetpack Compose**. This app serves as a clean, minimal interface for receiving daily inspiration directly from a Google Sheets-powered backend.

## 🚀 Features

- **Dynamic Source**: Fetches quotes in real-time from a published Google Sheets CSV URL.
- **Customizable Appearance**: Choose from four zen-inspired themes (Neutral, Scholarly Gold, Peaceful Sage, and Intellectual Blue) that persist across sessions.
- **Manual Refresh & Cache Busting**: Instantly sync with the latest spreadsheet changes using the manual refresh button.
- **Offline First**: Automatically caches the latest quotes to ensure the app works perfectly without an internet connection.
- **Daily Notifications**: Receive a random dose of wisdom every morning via Android **WorkManager**. Tapping a notification launches the app directly.
- **Discovery Tools**:
    - **Interactive Pager**: Swipe through a shuffled deck of wisdom with haptic feedback and smooth scale/alpha transformations.
    - **Author Browser**: Explore the entire collection grouped by author with a high-performance `LazyColumn` interface.
    - **Smart Biographies**: Detailed "About the Author" sections with intelligent paragraph formatting that handles abbreviations correctly.
    - **Portrait Gallery**: High-resolution, full-screen image viewer with support for multiple portraits per author.
    - **Interactive Zoom**: Advanced gesture support including pinch-to-zoom, panning, and smooth animated resets for portraits.
- **Visual Identity**:
    - **Philosophical Symbolism**: Integrated "About" section in settings explaining the labyrinth as a metaphor for the pursuit of wisdom. Click on the labyrinth icon in settings to view it in full screen.
    - **Consistent Author Branding**: Every quote card automatically finds and displays the author's portrait by scanning the entire dataset.
    - **Dynamic Avatars**: Fallback to color-coded circular avatars for authors without images.
    - **Premium UI**: Refined typography (26sp quotes), balanced negative space, and modern Material 3 components with high-contrast accessibility (WCAG AA compliant).

## 🛠️ Technical Stack

- **UI**: Jetpack Compose (Material 3)
- **Image Loading**: **Coil** with custom User-Agent headers for reliable sourcing from Wikimedia and Google Drive.
- **Architecture**: Modular functional approach with a centralized `QuoteRepository`.
- **Background Tasks**: WorkManager for reliable daily notification scheduling.
- **Networking**: Kotlin Coroutines with cache-busting timestamping.
- **Storage**: `SharedPreferences` with Android KTX for efficient local caching of quotes and theme preferences.
- **Permissions**: Fully compatible with Android 13+ (TIRAMISU) notification permission flows.

## 📦 Data Schema

The app expects a published Google Sheets CSV with the following columns:
1. **Author**: Name of the individual.
2. **About**: A biography or description of the author.
3. **Quote**: The text of the wisdom.
4. **Image URL (Optional)**: A direct link to an author image (supports Wikimedia direct links and Google Drive share links). Add multiple rows with the same author to provide multiple images for the gallery.

## 📸 Screenshots

*(Add your screenshots here!)*

## 📝 License

This project is intended for personal use and learning. Feel free to fork and customize your own version of wisdom!
