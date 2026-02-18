# ChessPredictor

A Kotlin Multiplatform chess application featuring an AI opponent powered by Stockfish engine with human-like playing behavior.

## Features

- **Adjustable Difficulty** - Play against AI with ELO ratings from 800 to 2800
- **Human-Like Mode** - AI makes realistic mistakes, thinks at natural pace, and shows emotional responses
- **Fast Mode** - Instant engine responses for quick games
- **Opening Detection** - Recognizes chess openings as you play
- **Game Management** - Save, load, import/export games in PGN format
- **Move Animations** - Smooth piece movement with board highlighting
- **Undo Support** - Take back moves when needed
- **Dark Theme** - Modern UI design

## Platforms

| Platform | Status |
|----------|--------|
| Android  | Production Ready |
| iOS      | Production Ready |
| Web      | Production Ready |

## Tech Stack

- **Kotlin Multiplatform** - Shared codebase across platforms
- **Jetpack Compose** - Modern Android UI
- **SwiftUI** - Native iOS UI with responsive iPad layout
- **Stockfish 17.1** - World's strongest open-source chess engine
- **Coroutines & StateFlow** - Reactive state management
- **MVVM Architecture** - Clean separation of concerns

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- Xcode 15+ (for iOS)
- JDK 11+
- Android SDK 24+
- CocoaPods (for iOS)

### Build Android

```bash
./gradlew :androidApp:assembleDebug
```

APK will be in `androidApp/build/outputs/apk/debug/`

### Build iOS

```bash
cd iosApp
pod install
open iosApp.xcworkspace
```

Build and run from Xcode on a simulator or device.

### Build Web

```bash
./build-web.sh
```

Output in `shared/build/dist/js/productionExecutable/`

## Project Structure

```
ChessPredictor/
├── androidApp/                 # Android app module
│   └── src/main/
│       └── MainActivity.kt
├── iosApp/                     # iOS app module (SwiftUI)
│   └── iosApp/
│       ├── ContentView.swift
│       ├── Views/             # SwiftUI views
│       └── ViewModels/        # ViewModel wrappers
├── shared/                     # Shared KMP module
│   ├── commonMain/            # Cross-platform code
│   │   ├── domain/            # Business logic
│   │   ├── data/              # Repositories & data sources
│   │   └── presentation/      # ViewModels & UI state
│   ├── androidMain/           # Android implementations
│   ├── iosMain/               # iOS implementations
│   └── jsMain/                # Web implementations
├── build.gradle.kts
└── settings.gradle.kts
```

## Architecture

The app follows MVVM with clean architecture principles:

- **Domain Layer** - Chess rules engine, move validation, game state
- **Data Layer** - Stockfish integration, game persistence
- **Presentation Layer** - ViewModels, UI state management

## License

Apache License 2.0
