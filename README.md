# iPhone-Inspired Android Dialer

A native Android Phone and Dialer application crafted with modern Android architecture, Jetpack Compose, and Material Design 3, inspired by the clean visual structure and ergonomic interaction patterns of the iOS Phone app.

## Features in Phase 1 (Foundation & Core UI)

- **Five Core Sections with Persistent Bottom Navigation**:
  - **Favourites**: Quick-dial favorite contacts with customizable list and empty states.
  - **Recents**: Call log history with "All" vs "Missed" segmented filtering, contact avatars, direction indicators, and date/time info.
  - **Contacts**: Alphabetically indexed contacts with right-edge alphabet scrubber, search field with mic action, and "My Card" profile card.
  - **Keypad**: Authentic 3x4 circular dial pad with bold primary digits, secondary alphabetical labels (`ABC`, `DEF`, etc.), formatted number display with animated backspace, call action, and incoming call simulation triggers.
  - **Voicemail**: Voicemail list with unread indicator badges, duration tags, transcription previews, and deleted message count.
- **Call Overlays & Lifecycle**:
  - **Incoming Call Screen**: Full-screen dark interface with caller identity, green "Accept" button, red "Decline" button, and quick message replies.
  - **Active Call Screen**: Active in-call dashboard showing live duration counter, caller metadata, in-call DTMF keypad toggle, mute toggle, and audio routing.
  - **Mini Call Banner**: Top persistent floating pill allowing uninterrupted navigation across dialer tabs during an active call.
- **Dual Theme Support**:
  - Dynamic adaptation to system Light and Dark themes with authentic iOS color palettes, subtle surface borders, and high contrast typography.
- **Automated CI/CD**:
  - GitHub Actions workflow (`.github/workflows/build-apk.yml`) to automatically compile and export debug APKs.

## Tech Stack

- **Language**: Kotlin 2.x
- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM (Model-View-ViewModel) + StateFlow
- **Coroutines & Flow**: Asynchronous call timer & state management
- **Build System**: Gradle (Kotlin DSL - `.gradle.kts`)

## Build & Run Instructions

```bash
# Build the debug APK
gradle assembleDebug

# Output APK path
# app/build/outputs/apk/debug/app-debug.apk
```
