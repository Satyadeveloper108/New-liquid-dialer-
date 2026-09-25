# iPhone-Inspired Android Dialer (Phase 1: Foundation & Core UI)

Build a native Android phone dialer with a premium iPhone-inspired aesthetic, featuring a 5-tab persistent bottom navigation bar, full keypad layout with DTMF feedback styling, contacts, recents, favourites, voicemail UI, and preview call sheets.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed during clarification:
> - **Theme Mode**: Follow system theme with authentic iOS light and dark palettes (subtle borders, translucent blur feel, high-contrast typography, and iOS system accents).
> - **Preview Data Strategy**: Include rich, realistic sample data for instant visual verification across all five tabs in Phase 1 before live Android Telecom and Contacts/Call Log system integrations in subsequent phases.
> - **Call Screen Previews**: Provide interactive test trigger buttons on Keypad and Recents to preview the full-screen Incoming Call, Active Call, and Mini Floating Call Banner interfaces.

---

## 1. Overview & Core Concept

- **What It Does**: A native Android Phone & Dialer app styled after the iconic iOS Phone interface. It provides five core tabs (Favourites, Recents, Contacts, Keypad, Voicemail) with circular keypad buttons, alphabetical contacts index, missed/all call history filtering, voicemail management UI, and full-screen incoming/active call simulation interfaces.
- **Target Audience / Persona**: Users seeking a clean, minimalist, high-usability dialer with the ergonomics, spacious typography, and fluid visual hierarchy of iOS on Android.
- **Key Value**: Delivers a fluid, visually refined dialer experience while laying a solid Clean Architecture and MVVM foundation ready for Android Telecom, Contacts Provider, and Call Log integrations in Phase 2+.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Dialing & Keypad**:
   - User opens the Keypad tab; sees the centered 3x4 grid of circular dial buttons (`1` to `9`, `*`, `0 +`, `#`).
   - Tapping keys appends digits with responsive touch ripples.
   - Digits appear above with formatted spacing and an animated backspace button.
   - Green circular call button initiates the call preview flow or places outgoing calls.
   - Quick action button allows previewing the incoming call screen directly.

2. **Browsing Contacts & Details**:
   - Contacts tab displays a search bar with microphone icon, "My Card" profile header, and alphabetically grouped contacts (`A`, `B`, `C`...).
   - A right-edge alphabet index scrubber enables quick jumping.
   - Tapping a contact reveals quick communication actions (Call, Message, Info).

3. **Recents & History**:
   - Segmented pill control toggles between **All** and **Missed** calls.
   - Each item shows contact avatar/initials, call direction icon, formatted timestamp, and right-hand info button. Missed calls stand out in bold red text.

4. **Favourites & Voicemail**:
   - Favourites shows frequent contacts with one-tap calling and a `+` button to add new favourites.
   - Voicemail displays audio voicemails with duration, date, transcription previews, and a "Deleted Voicemails" count.

5. **Call Lifecycle Overlays**:
   - **Incoming Call Screen**: Dark full-screen presentation with large caller text, "Decline" (red) and "Accept" (green) circular buttons, plus "Type to Reply" and "More" actions.
   - **Active Call Screen**: Displays call timer, caller name, 6-button control grid (Audio routing, Video placeholder, Mute, Keypad, More, End Call).
   - **Mini Call Banner**: Top persistent floating pill when navigating away during an active call, showing live timer, caller name, and end call button.

### Visual Identity & Theme

- **Design Philosophy**: Minimal, soft-radii, glass-like elevation, and precision spacing inspired by iOS Human Interface Guidelines recreated natively in Jetpack Compose.
- **Light Mode Palette**:
  - Background: `#FFFFFF` (Primary Surface), `#F2F2F7` (Secondary grouped background)
  - Text: `#000000` (Primary), `#8E8E93` (Secondary label)
  - Accents: `#007AFF` (iOS System Blue), `#34C759` (Call Green), `#FF3B30` (End/Missed Call Red)
  - Keypad Surface: `#E5E5EA` with subtle border and elevation
- **Dark Mode Palette**:
  - Background: `#000000` (Primary Surface), `#1C1C1E` (Elevated grouped cards)
  - Text: `#FFFFFF` (Primary), `#98989D` (Secondary)
  - Keypad Surface: `#2C2C2E`
- **Typography**: Clean San Francisco-style metrics using high-legibility sans-serif with bold number keys (36sp) and sub-letter labels (10sp).
- **Navigation Bar**: Translucent blur/pill bottom bar with 5 items, blue icon + text highlight on active tab, and soft rounded indicator.

---

## 3. Key Product Decisions & Trade-Offs

- **Phase 1 Modular Scope**:
  - *Decision*: Focus strictly on the complete UI foundation, navigation, theme system, responsive layouts, and call screen mocks as specified in the Phase 1 prompt.
  - *Why*: Establishes rock-solid compilation, proper package architecture, and UI fidelity before adding complex Telecom framework and Android permissions in Phase 2.
- **State Management via MVVM & StateFlow**:
  - *Decision*: Centralize UI state in `DialerViewModel` exposing immutable `DialerUiState` with `StateFlow`.
  - *Why*: Keeps the 5 tabs responsive and synchronized (e.g. initiating a call from Contacts, Keypad, or Recents transitions seamlessly into the active call overlay).
- **Zero Heavy External Dependencies**:
  - *Decision*: Rely on standard AndroidX, Jetpack Compose Material 3, and Coroutines already configured in the project.
  - *Why*: Ensures rapid Gradle build times and clean GitHub Actions compilation without version mismatch risks.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                          MainActivity                            │
│           (Edge-to-edge, Theme Container, Root Scaffold)         │
└─────────────────────────────────┬────────────────────────────────┘
                                  │
                                  ▼
┌──────────────────────────────────────────────────────────────────┐
│                         DialerApp                                │
│       ┌─────────────────────────┴────────────────────────┐       │
│       ▼                                                  ▼       │
│ ┌───────────────────────────┐              ┌───────────────────┐ │
│ │   Call Overlays / Sheets  │              │  Active Call Bar  │ │
│ │ - IncomingCallScreen      │              │ (Mini Call Banner)│ │
│ │ - ActiveCallScreen        │              └───────────────────┘ │
│ └───────────────────────────┘                                    │
│                                                                  │
│  Five Core Screen Router:                                        │
│  ┌──────────────┬──────────────┬──────────────┬───────────────┐  │
│  │  Favourites  │   Recents    │   Contacts   │    Keypad     │  │
│  │    Screen    │    Screen    │    Screen    │    Screen     │  │
│  └──────────────┴──────────────┴──────────────┴───────────────┘  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                      Voicemail Screen                      │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                  │                               │
│                                  ▼                               │
│             ┌────────────────────────────────────────┐           │
│             │       iOS-Style Bottom Navigation      │           │
│             │  [Favourites][Recents][Contacts][Keypad][Voicemail]│
│             └────────────────────────────────────────┘           │
└─────────────────────────────────┬────────────────────────────────┘
                                  │
                                  ▼
┌──────────────────────────────────────────────────────────────────┐
│                         DialerViewModel                          │
│  - Tab navigation state (active tab, badge counts)               │
│  - Keypad buffer (digits, formatting, backspace)                 │
│  - Active/Incoming Call state & timer ticker                     │
│  - Contacts, Recents, Favourites, Voicemails (Domain models)     │
└──────────────────────────────────────────────────────────────────┘
```

### Key Data Entities

- `ContactItem`: ID, display name, phone number, label (mobile/home), avatar URL or initials, isFavorite, photoUri.
- `CallLogItem`: ID, caller name/number, call type (INCOMING, OUTGOING, MISSED), timestamp, duration, repeatCount.
- `VoicemailItem`: ID, caller name/number, timestamp, duration, transcription, isRead, isDeleted.
- `CallState`: IDLE, INCOMING, ACTIVE, ENDED, with associated `CallSession` (caller name, number, durationSeconds, isMuted, isSpeakerOn).

### Interactive Component & State Mapping

1. **KeypadScreen**:
   - `onDigitClick(char)`: Appends digit, formats string, triggers haptic feedback.
   - `onDeleteClick()`: Drops trailing character; long press clears entire buffer.
   - `onCallClick()`: Launches active call session for dialed number.
   - `onSimulateIncoming()`: Triggers incoming call simulation modal.
2. **RecentsScreen**:
   - `onToggleFilter(RecentsFilter.ALL / MISSED)`: Filters call history in place.
   - `onCallItemClick(CallLogItem)`: Initiates instant call to log entry.
3. **ContactsScreen**:
   - `onSearchQueryChange(query)`: Live filter of contacts list.
   - `onScrubAlphabet(letter)`: Scrolls LazyColumn to target alphabet header.
4. **ActiveCallScreen & IncomingCallScreen**:
   - Full-screen overlays responding to `CallState`.
   - `onAccept()`, `onDecline()`, `onToggleMute()`, `onToggleSpeaker()`, `onEndCall()`, `onMinimize()`.
5. **MiniCallBanner**:
   - Displays when `callState == ACTIVE` and user minimizes to browse other tabs.
