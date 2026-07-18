# Copy-Paste Wisdom 📖✨

A personal daily wisdom app built with **Kotlin** and **Jetpack Compose**. This app serves as a clean, minimal interface for receiving daily inspiration directly from a curated Google Sheets backend, enhanced by a massive 30,000+ quote global library.

## 🚀 Key Features

### 🏮 Content & Curation
- **Massive Discovery Engine**: Features an integrated **Extended Archive** of over 30,000 quotes. The engine uses a smart multi-stage search that prioritizes your curated collection while offering instant local search across history's greatest thinkers.
- **Dynamic Google Sheets Sync**: Real-time synchronization with a published CSV backend, featuring manual refresh and intelligent cache-busting.
- **Smart Deduplication**: Advanced accent normalization automatically merges authors with different diacritics (e.g., "Niccolo" and "Niccolò"), prioritizing your curated names and portraits.
- **Offline-First Architecture**: Caches the entire 30k archive and curated list for a 100% functional experience without an internet connection.

### 🎡 Navigation & Experience
- **Fast-Scroll Browser**: A high-performance navigation bar with a dynamic **Letter Bubble** that moves with your thumb, allowing you to zip through thousands of authors in seconds.
- **Real-Time Search**: Instant filtering in the author browser with context-aware auto-reset logic.
- **Endless Quote Pager**: An infinitely looping shuffled deck of wisdom with haptic feedback and smooth scale/alpha transformations.
- **Fluid Resets**: The "Return to Today" feature includes deterministic reshuffling and a smooth, fast-scroll animation back to "Today's Wisdom."

### 🔔 Smart Notifications
- **Exact 24-Hour Timing**: High-precision delivery using `AlarmManager`, bypassing system "Doze" delays to hit your target time (e.g., 05:00) exactly.
- **Rich Visuals**: Notifications feature author portraits, a clean monochromatic "Book" silhouette for the status bar, and `BigTextStyle` for full-quote expansion.
- **Resilience**: Integrated boot-survival logic and an "Improve Timing" settings helper for Android 14+ permissions.

### 🎨 Premium UI/UX
- **Hero Portraits**: Large **150dp** author portraits on the home screen paired with bold **headline typography** for a high-end editorial feel.
- **Scrollable Wisdom**: Quote cards automatically handle long text with internal scrolling, ensuring author details are never clipped.
- **Burst Animation**: Custom "Shared Element" effect where portraits physically grow out of thumbnails and shrink back on dismissal.
- **Zen Themes**: Four custom themes (Neutral, Scholarly, Peaceful, Intellectual) that persist across sessions.

## 🛠️ Technical Stack

- **UI**: Jetpack Compose (Material 3) with Spring physics and bouncy interactions.
- **Networking**: `HttpURLConnection` with Coroutines and a robust multi-stage discovery fallback.
- **Image Loading**: **Coil** with custom headers and large icon notification support.
- **Scheduling**: `AlarmManager` for precise time-based triggers.
- **Data**: `Normalizer` API for accent-insensitive author matching.
- **Storage**: `SharedPreferences` with Android KTX.

## 📦 Data Schema

The app expects a published Google Sheets CSV with the following columns:
1. **Author**: Name of the individual.
2. **About**: A biography or description of the author.
3. **Quote**: The text of the wisdom.
4. **Image URL (Optional)**: A direct link to an author image (supports Wikimedia and Google Drive). Add multiple rows with the same author for a gallery.

## 📝 License

This project is intended for personal use and learning. Feel free to fork and customize your own version of wisdom!
