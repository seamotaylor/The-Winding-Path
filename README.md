# Copy-Paste Wisdom 📖✨

A personal daily wisdom app built with **Kotlin** and **Jetpack Compose**. This app serves as a clean, minimal interface for receiving daily inspiration directly from a curated Google Sheets backend, enhanced by global wisdom discovery engines.

## 🚀 Key Features

### 🏮 Content & Curation
- **Dynamic Google Sheets Sync**: Fetches quotes in real-time from a published CSV URL with manual refresh and cache-busting logic.
- **Offline-First Architecture**: Automatically caches the latest quotes to ensure 100% functionality without an internet connection.
- **Rich Biographies**: Detailed "About the Author" sections with intelligent paragraph formatting and portrait scanning across the entire dataset.

### 🎡 Navigation & Experience
- **Endless Quote Pager**: An infinitely looping shuffled deck of wisdom with haptic feedback and smooth scale/alpha transformations.
- **Multi-Source Discovery Engine**:
    - **Extended Archive**: Instant local search across ~1,500 famous quotes (via DummyJSON).
    - **Global Discovery**: Deep-web fallback to the ZenQuotes API for niche authors like Baruch Spinoza.
    - **"I'm Feeling Lucky"**: A randomizer for discovering new authors beyond your curated list.
- **Quick Share**: Tap any discovered quote to instantly copy it to your clipboard in a professional format.

### 🔔 Smart Notifications
- **Exact 24-Hour Timing**: Migrated to `AlarmManager` for precise delivery, bypassing Android's "Doze" delays to hit your target (e.g., 05:00 AM) exactly.
- **Rich Visuals**: Notifications include author portraits and a clean monochromatic "Book" silhouette for the status bar.
- **Full-Text Expansion**: Uses `BigTextStyle` to allow reading the entire quote directly from the notification tray.
- **Resilience**: Features boot-survival logic and an "Improve Timing" settings helper for Android 14+ permissions.

### 🎨 Premium UI/UX
- **Burst Animation**: Custom "Shared Element" effect where full-screen images physically grow out of the thumbnails you tap and shrink back on dismissal.
- **Bouncy Interaction**: Uses Spring physics for organic, responsive feeling UI transitions.
- **Portrait Gallery**: High-resolution viewer with support for multiple images per author, pinch-to-zoom, and panning gestures.
- **Zen Themes**: Four custom themes (Neutral, Scholarly, Peaceful, Intellectual) that persist across sessions.

## 🛠️ Technical Stack

- **UI**: Jetpack Compose (Material 3)
- **Networking**: `HttpURLConnection` with Coroutines and robust retry logic.
- **Image Loading**: **Coil** with custom User-Agent headers for Wikimedia and Google Drive compatibility.
- **Scheduling**: `AlarmManager` for high-precision time-based triggers.
- **Storage**: `SharedPreferences` with Android KTX.
- **Parsing**: `org.json` for lightweight, reliable API interaction.

## 📦 Data Schema

The app expects a published Google Sheets CSV with the following columns:
1. **Author**: Name of the individual.
2. **About**: A biography or description of the author.
3. **Quote**: The text of the wisdom.
4. **Image URL (Optional)**: A direct link to an author image (supports Wikimedia direct links and Google Drive share links). Add multiple rows with the same author to provide multiple images for the gallery.

## 📝 License

This project is intended for personal use and learning. Feel free to fork and customize your own version of wisdom!
