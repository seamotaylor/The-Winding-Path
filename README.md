# The Winding Path 📖✨

A personal daily wisdom app built with **Kotlin** and **Jetpack Compose**. This app serves as a clean, minimal interface for receiving daily inspiration directly from a curated Google Sheets backend, enhanced by a massive 30,000+ quote global library.

## 🚀 Key Features

### 🏮 Content & Curation
- **The Expanded Library**: Seamlessly switch between your **Curated Anthology** and the massive 30,000+ quote global library with a dynamic mode indicator.
- **Dynamic Google Sheets Sync**: Real-time synchronization with a published CSV backend, featuring manual refresh and intelligent cache-busting.
- **Smart Deduplication & Priority**: Advanced accent normalization automatically merges authors with different diacritics (e.g., \"Niccolo\" and \"Niccolò\"). A robust multi-tier priority system (Main Sheet > Archive > Global Library) ensures that curated data always takes precedence.
- **Handpicked Order**: The app intelligently handles multiple rows for the same author, ensuring that the **first** appearance in your curated sheet becomes the default portrait, preserving your editorial intent.
- **ZenQuotes & Lucky Discovery**: Integration with the ZenQuotes API for "I'm Feeling Lucky" surprises, with a fallback to the local archive.
- **Offline-First Architecture**: Caches the entire 30k archive and curated list for a 100% functional experience without an internet connection.

### 🎡 Navigation & Experience
- **Author Identity Drawer**: An enhanced Modal Bottom Sheet that provides deep dives into author biographies, featuring smart paragraph formatting and a multi-image gallery with a high-contrast "Dark Studio" mode.
- **Animated Zoom Gallery**: Tap any author portrait to enter an immersive, animated full-screen view with pinch-to-zoom, pan support, and a dedicated contrast toggle for optimal legibility across different source images.
- **Swipeable Browser**: A high-performance navigation system allowing you to swipe between **Authors** and **Topics** effortlessly.
- **Fast-Scroll Browser**: A dedicated scroll bar with a dynamic **Letter Bubble** that moves with your thumb, allowing you to zip through thousands of authors in seconds.
- **Endless Quote Pager**: An infinitely looping shuffled deck of wisdom with haptic feedback and smooth scale/alpha transformations.
- **Tray Visuals**: Quotes are displayed in premium "Visual Trays" (inspired by editorial card designs) with italics, integrated author signatures, and "Learn More" links.

### 🎨 Personalization & Settings
- **Dynamic Theming**: Choose from a variety of curated color palettes to match your philosophical mood, updating the app's primary and accent colors instantly.
- **Reminders Hub**: A dedicated control center to schedule your daily wisdom delivery. Adjust the notification time and filter the content source (Curated vs. Global Library).
- **Interactive About Section**: Learn more about the app's mission with an integrated animated icon zoom and updated philosophical identity descriptions.

### 🔔 Smart Notifications
- **Exact 24-Hour Timing**: High-precision delivery using `AlarmManager`, bypassing system "Doze" delays to hit your target time exactly.
- **Rich Visuals**: Notifications feature author portraits and `BigTextStyle` for full-quote expansion.
- **Customizable Source**: Toggle between receiving only **Curated Wisdom** or wisdom from the **Full Library** in your daily reminders.

## 🏗️ Architecture & Stability

The app follows a modern, decoupled **Clean Architecture** to ensure stability and ease of growth:

- **Data Layer**: Specialized repositories and models for quote processing, CSV parsing, and cross-source deduplication.
- **Logic Layer**: Independent scheduling logic for alarms and system events.
- **UI Components**: A library of small, atomic Compose components (`QuoteTray`, `AuthorAvatar`, etc.) for consistent styling.
- **Reactive UI (ViewModel)**: Powered by `StateFlow`, ensuring the app survives screen rotations and system events without losing data or re-fetching.

## 🧪 Testing Suite

Stability is guarded by a dual-layer automated testing strategy using the latest **AndroidX Test** and **Espresso** stacks:
- **Comprehensive Coverage**: 25+ automated tests verifying the \"fragile\" core logic, UI integrity, and end-to-end journeys.
- **Unit Tests**: Verifying CSV parsing robustness (handling commas inside quotes), name normalization, and the **Daily Wisdom Determinism**—ensuring the daily quote remains stable throughout the day.
- **Identity Integrity Tests**: Specialized UI tests that verify the correct rendering of biographies and metadata for key figures (e.g., Confucius, Lao Tzu) within the Author Identity Drawer.
- **End-to-End UI Tests**: Automated journey testing that verifies the "Golden Path"—launching, loading, and browser navigation. Optimized with increased timeouts and robust semantic matching for reliable CI/CD execution.

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
