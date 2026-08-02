# Paper Bird

*A quiet place for your thoughts.*

Kotlin + Jetpack Compose 

---

## Package

`com.ishaan.paperBird`

---

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| Database | Room (SQLite) |
| DI | Hilt |
| Preferences | DataStore |
| Images | Coil |
| Architecture | MVVM (ViewModel + StateFlow) |

---

## Setup

### Requirements
- Android Studio Ladybug (2024.2) or newer
- JDK 17
- minSdk 26 (Android 8.0)

### Steps

```bash
# 1. Open the project in Android Studio
# 2. Let Gradle sync complete
# 3. Run on device or emulator (API 26+)
```

---

## Features

### Write Without Distractions
- Full-screen letter editor with adjustable font size
- Title + body with placeholder text
- Autosave (3-second debounce on existing letters)
- Word count & reading time in the editor footer

### View & Edit Modes
- Existing letters open in **view mode** — clean, read-only, with Markdown rendered
- View mode header shows Back, Favorite (indicator), Category (indicator), and Edit
- Tapping **Edit** switches to edit mode with the full toolbar (Favorite, Category, Image, Save)
- Keyboard scrolls content correctly in edit mode; cursor stays visible

### Markdown Rendering (View Mode)
Rendered inline using `AnnotatedString` — no external library required:

| Syntax | Output |
|---|---|
| `**text**` | **bold** |
| `*text*` | *italic* |
| `~~text~~` | ~~strikethrough~~ |
| `` `text` `` | `inline code` |
| `# / ## / ###` | Headings (H1–H3) |
| `> text` | Blockquote |
| `- / * / 1.` | Bullet / numbered list |

### Built Around Letters
- Favorites — heart any letter to keep it close
- Categories — Love, Gratitude, Achievement, Grief, Memory, Dream, Today (+ custom)
- Custom categories with a chosen color, visible in the category list
- Duplicate letters
- Image attachments (stored by URI, completely local)

### Find Memories Again
- Instant search with match highlighting
- Library screen — sort by Newest / Oldest / A→Z / Z→A
- Category filter in Library
- Relative date grouping (Today / Yesterday / This Week / This Month / Month / Year)

### Memories
- Calendar view — see which days have letters, tap to reveal them
- Timeline — chronological scroll through your full story
- On This Day — letters from the same date in past years (shown on Home)
- Random Letter — a letter to revisit (shown on Home)

### Personalization
- Dark / Light / System theme toggle
- 6 accent colors: Rose, Lavender, Sage, Sky, Amber, Slate
- Adjustable editor font size (12–24sp)
- Default writing category
- Custom categories with color picker

### Privacy
- All data stored in local Room database
- **App Lock** — enable with a 4-digit PIN (entered twice to confirm); disabling requires verifying the current PIN
- **Biometric** — unlock with Fingerprint or Face ID (requires App Lock)
- **Instant Lock** — re-locks the app the moment it goes to background (requires App Lock)

---

## Project Structure

```
app/src/main/java/com/ishaan/paperBird/
├── data/
│   ├── local/
│   │   ├── dao/          # LetterDao, AttachmentDao
│   │   ├── entities/     # LetterEntity, AttachmentEntity
│   │   └── PaperBirdDatabase.kt
│   └── repository/
│       ├── LetterRepository.kt
│       └── SettingsRepository.kt
├── di/
│   └── AppModule.kt      # Hilt module
├── domain/
│   └── model/
│       └── Letter.kt     # Domain models + constants
├── ui/
│   ├── components/       # LetterCard, CategoryBadge, PinSetupDialog, PinVerifyDialog, etc.
│   ├── navigation/       # Screen routes + BottomNavItem
│   ├── screens/
│   │   ├── LetterViewModel.kt   # Shared ViewModel
│   │   ├── home/
│   │   ├── editor/
│   │   ├── library/
│   │   ├── favorites/
│   │   ├── calendar/
│   │   ├── timeline/
│   │   └── settings/
│   └── theme/            # PaperBirdTheme, Typography, AccentColors
├── MainActivity.kt
├── PaperBirdApp.kt           # Nav host + Scaffold
└── PaperBirdApplication.kt   # @HiltAndroidApp
```

---

## License

MIT
