# Copy-Paste Wisdom 📖✨

A personal daily wisdom app built with **Kotlin** and **Jetpack Compose**. This app serves as a clean, minimal interface for receiving daily inspiration directly from a curated Google Sheets backend, enhanced by a massive 30,000+ quote global library.

## 🚀 Key Features

### 🏮 Content & Curation
- **Massive Discovery Engine**: Features an integrated **Extended Archive** of over 30,000 quotes. The engine uses a smart multi-stage search that prioritizes your curated collection while offering instant local search across history's greatest thinkers.
- **Dynamic Google Sheets Sync**: Real-time synchronization with a published CSV backend, featuring manual refresh and intelligent cache-busting.
- **Smart Deduplication**: Advanced accent normalization automatically merges authors with different diacritics (e.g., "Niccolo" and "Niccolò"), prioritizing your curated names and portraits.
- **Handpicked Order**: The app intelligently handles multiple rows for the same author, ensuring that your **first** curated image in the sheet becomes the default portrait.
- **Offline-First Architecture**: Caches the entire 30k archive and curated list for a 100% functional experience without an internet connection.

### 🎡 Navigation & Experience
- **Swipeable Browser**: A high-performance navigation system allowing you to swipe between **Authors** and **Topics** effortlessly.
- **Fast-Scroll Browser**: A dedicated scroll bar with a dynamic **Letter Bubble** that moves with your thumb, allowing you to zip through thousands of authors in seconds.
- **Endless Quote Pager**: An infinitely looping shuffled deck of wisdom with haptic feedback and smooth scale/alpha transformations.
- **Tray Visuals**: Quotes are displayed in premium "Visual Trays" (inspired by editorial card designs) with italics and integrated author signatures.

### 🔔 Smart Notifications
- **Exact 24-Hour Timing**: High-precision delivery using `AlarmManager`, bypassing system "Doze" delays to hit your target time exactly.
- **Rich Visuals**: Notifications feature author portraits and `BigTextStyle` for full-quote expansion.

## 🏗️ Architecture & Stability

The app follows a modern, decoupled **Clean Architecture** to ensure stability and ease of growth:

- **Data Layer**: Specialized repositories and models for quote processing, CSV parsing, and cross-source deduplication.
- **Logic Layer**: Independent scheduling logic for alarms and system events.
- **UI Components**: A library of small, atomic Compose components (`QuoteTray`, `AuthorAvatar`, etc.) for consistent styling.
- **Reactive UI (ViewModel)**: Powered by `StateFlow`, ensuring the app survives screen rotations and system events without losing data or re-fetching.

## 🧪 Testing Suite

Stability is guarded by a dual-layer automated testing strategy:
- **Unit Tests**: 7+ tests verifying the "fragile" core logic, including CSV parsing robustness (handling commas inside quotes) and name normalization.
- **UI Tests (AndroidTest)**: Automated journey testing that verifies the "Golden Path"—launching, loading, and browser navigation—on real devices.

## 🛠️ Technical Stack

- **UI**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (ViewModel + StateFlow)
- **Networking**: Coroutines with `HttpURLConnection`
- **Image Loading**: **Coil** with custom headers for secure archive access.
- **Scheduling**: `AlarmManager` for precision triggers.
- **Testing**: JUnit 4 & Compose Test Rule.

## 📦 Data Schema

The app expects a published Google Sheets CSV with the following columns:
1. **Author**: Name of the individual.
2. **About**: A biography or description.
3. **Quote**: The text of the wisdom.
4. **Image URL (Optional)**: A direct link to an image. Add multiple rows with the same author to create a gallery.

## 📝 License

This project is intended for personal use and learning. Feel free to fork and customize your own version of wisdom!
