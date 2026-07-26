# PaperBird

*A quiet place for your thoughts.*

PaperBird is a minimalist journaling app built with Kotlin and Jetpack Compose. It helps you capture memories, thoughts, emotions, and everyday moments in a calm, distraction-free writing experience.

Everything stays on your device. No accounts, no cloud, no ads—just your words.

---

## Features

### ️ Write Freely
- Clean, distraction-free editor
- Adjustable editor font size
- Autosave with debounce
- Word count & estimated reading time

### Organize Your Thoughts
- Categories with custom category support
- Favorites
- Duplicate letters
- Image attachments
- Instant search

### Revisit Memories
- Timeline view
- Calendar view
- Random Letter
- On This Day
- Smart date grouping

### Personalize
- Light & Dark themes
- Six accent colors
- Adjustable editor preferences
- Default writing category

### Private by Design
- Local Room database
- No accounts
- Your data never leaves your device

---

## 🛠 Tech Stack

| Layer | Technology |
|--------|------------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM |
| Navigation | Navigation Compose |
| Database | Room (SQLite) |
| Dependency Injection | Hilt |
| Preferences | DataStore |
| Images | Coil |

---

## Project Structure

```text
app/
└── src/
    └── main/
        └── java/
            └── com/ishaan/paperBird/
                ├── data/
                │   ├── local/
                │   │   ├── dao/
                │   │   ├── entities/
                │   │   └── PaperBirdDatabase.kt
                │   └── repository/
                │
                ├── di/
                │
                ├── domain/
                │   └── model/
                │
                ├── ui/
                │   ├── components/
                │   ├── navigation/
                │   ├── screens/
                │   │   ├── calendar/
                │   │   ├── editor/
                │   │   ├── favorites/
                │   │   ├── home/
                │   │   ├── library/
                │   │   ├── settings/
                │   │   └── timeline/
                │   ├── LetterViewModel.kt
                │   └── theme/
                │
                ├── util/
                │
                ├── MainActivity.kt
                ├── PaperBird.kt
                └── PaperBirdApplication.kt
```

---

## Getting Started

### Requirements

- Android Studio Ladybug (2024.2) or newer
- JDK 17
- Android 8.0 (API 26+) or higher

### Run

```bash
git clone https://github.com/akasajal/PaperBird.git
```

Open the project in Android Studio, allow Gradle to sync, then run on an emulator or physical device.

---

## Roadmap

- Export letters (TXT / PDF)
- Import letters
- Encrypted backups
- Markdown preview
- Rich text formatting

---

## License

Licensed under the MIT License.

---

Made with ❤️ by **Sajal**.