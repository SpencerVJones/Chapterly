<div align="center">
  <h2 align="center">Chapterly</h2>
  <div align="left">

![Repo Views](https://visitor-badge.laobi.icu/badge?page_id=SpencerVJones/Google-Books-Listing-App)
</div>

  <p align="center">
    A fullstack portfolio project: Android app (Kotlin + Compose) plus backend API (Spring Boot + Postgres + Flyway) with Firebase Auth and Firestore sync.
    <br />
    <br />
    <a href="https://github.com/SpencerVJones/Google-Books-Listing-App/issues">Report Bug</a>
    ·
    <a href="https://github.com/SpencerVJones/Google-Books-Listing-App/issues">Request Feature</a>
  </p>
</div>

<!-- PROJECT SHIELDS -->
<div align="center">

![License](https://img.shields.io/github/license/SpencerVJones/Google-Books-Listing-App?style=for-the-badge)
![Contributors](https://img.shields.io/github/contributors/SpencerVJones/Google-Books-Listing-App?style=for-the-badge)
![Forks](https://img.shields.io/github/forks/SpencerVJones/Google-Books-Listing-App?style=for-the-badge)
![Stargazers](https://img.shields.io/github/stars/SpencerVJones/Google-Books-Listing-App?style=for-the-badge)
![Issues](https://img.shields.io/github/issues/SpencerVJones/Google-Books-Listing-App?style=for-the-badge)
![Last Commit](https://img.shields.io/github/last-commit/SpencerVJones/Google-Books-Listing-App?style=for-the-badge)
![Repo Size](https://img.shields.io/github/repo-size/SpencerVJones/Google-Books-Listing-App?style=for-the-badge)
![Platform](https://img.shields.io/badge/platform-Android-3ddc84.svg?style=for-the-badge)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF.svg?style=for-the-badge)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4.svg?style=for-the-badge)
![Material 3](https://img.shields.io/badge/Material%203-Design-1f2937.svg?style=for-the-badge)
![Room](https://img.shields.io/badge/Room-Offline%20Cache-0ea5e9.svg?style=for-the-badge)
![Paging 3](https://img.shields.io/badge/Paging%203-Infinite%20Scroll-2563eb.svg?style=for-the-badge)
![Retrofit](https://img.shields.io/badge/Retrofit-Moshi-f59e0b.svg?style=for-the-badge)
![Hilt](https://img.shields.io/badge/Hilt-DI-16a34a.svg?style=for-the-badge)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-Backend-6DB33F.svg?style=for-the-badge)
![Postgres](https://img.shields.io/badge/Postgres-Database-336791.svg?style=for-the-badge)
![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200.svg?style=for-the-badge)
![Firebase Auth](https://img.shields.io/badge/Firebase-Auth-FFCA28.svg?style=for-the-badge)
![Firestore](https://img.shields.io/badge/Firestore-Cloud%20Sync-FFA000.svg?style=for-the-badge)
![CI](https://img.shields.io/badge/GitHub%20Actions-CI-111827.svg?style=for-the-badge)
</div>

## Table of Contents
- [Overview](#overview)
- [Technologies Used](#technologies-used)
- [Features](#features)
- [Demo](#demo)
- [Project Structure](#project-structure)
- [API Endpoints](#api-endpoints)
- [API Examples](#api-examples)
- [Backend API](#backend-api)
- [Firebase Auth and Firestore](#firebase-auth-and-firestore)
- [Performance and Baseline Profile](#performance-and-baseline-profile)
- [Testing](#testing)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Open in Android Studio from VS Code](#open-in-android-studio-from-vs-code)
  - [Run on Emulator or Device](#run-on-emulator-or-device)
  - [Run Backend Locally](#run-backend-locally)
- [CI](#ci)
- [Troubleshooting](#troubleshooting)
- [Usage](#usage)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
  - [Contributors](#contributors)
- [License](#license)
- [Contact](#contact)

## Overview
**Chapterly** is a fullstack project that demonstrates production-style Android + backend engineering:
- Kotlin + Jetpack Compose UI
- Offline-first data architecture with Room + Paging 3 `RemoteMediator`
- Retrofit + Moshi API integration with Google Books
- MVVM + Repository + Hilt dependency injection
- Spring Boot backend with PostgreSQL and Flyway migrations
- Firebase Auth for mobile sign-in and Firestore cloud sync hooks
- Search history, favorites, sorting/filtering, detail screen navigation
- CI quality gates for both app and backend

Current Android package/runtime target:
- `namespace` and `applicationId`: `com.example.chapterly`
- Manifest app class: `com.example.chapterly.ChapterlyApplication`
- Manifest launcher activity: `com.example.chapterly.MainActivity`

## Technologies Used
- Kotlin
- Jetpack Compose (Material 3)
- Navigation Compose
- Retrofit
- Moshi
- Room
- Paging 3
- Hilt
- Coil
- JUnit + Robolectric + Turbine
- Compose UI testing
- Detekt + Ktlint + JaCoCo
- GitHub Actions CI
- Macrobenchmark + Baseline Profiles
- Spring Boot (Kotlin backend)
- PostgreSQL
- Flyway
- Firebase Auth
- Firestore

## Features
- Search books by keyword using Google Books API
- Infinite list loading with Paging 3
- Offline-first caching (read cached books without network)
- Real favorites/bookmarks persisted locally
- Search history with quick re-use
- Sort options: relevance, newest, rating
- Explore and Favorites tabs
- Book detail screen with:
  - title/author/metadata
  - expandable description
  - share action
  - preview link in Custom Tabs
- Pull-to-refresh and polished loading/empty/error states
- Accessibility labels for key actions
- Account tab with Firebase Auth sign-in/sign-up/sign-out
- Cloud sync hooks (Firestore) for favorites and search history
- Backend REST API with persisted entities:
  - users
  - favorites
  - history
  - lists
  - list_items
  - reviews

## Demo
Run locally on Android Emulator or device:
- Explore tab for discovery and search
- Favorites tab for saved books
- Detail screen for rich metadata and preview

If you want, add screenshots/GIFs under `docs/images/` and link them here.

## Project Structure
```bash
Google-Books-Listing-App/
├── README.md
├── LICENSE
├── .github/
│   └── workflows/
│       └── android-ci.yml
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── kotlin/com/example/chapterly/backend/
│       │   └── resources/db/migration/
│       └── test/
└── Chapterly/
    ├── app/
    │   ├── build.gradle.kts
    │   └── src/
    │       ├── main/
    │       │   ├── AndroidManifest.xml
    │       │   ├── baseline-prof.txt
    │       │   └── java/com/example/chapterly/
    │       │       ├── data/
    │       │       │   ├── local/
    │       │       │   ├── remote/
    │       │       │   └── repository/
    │       │       ├── di/
    │       │       ├── model/
    │       │       └── ui/
    │       ├── test/
    │       └── androidTest/
    ├── benchmark/
    │   └── src/
    │       ├── main/
    │       └── androidTest/
    ├── config/
    │   └── detekt/
    ├── gradle/
    ├── settings.gradle.kts
    └── gradlew
```

## API Endpoints
External Google Books API consumed by mobile app:
- `GET https://www.googleapis.com/books/v1/volumes?q={query}&startIndex={start}&maxResults={size}&orderBy={relevance|newest}`

Common query parameters used:
- `q` search text
- `startIndex` for pagination offset
- `maxResults` page size
- `orderBy` sorting mode

Backend API endpoints (Spring Boot):
- `GET /api/backend/health`
- `GET /api/backend/me`
- `GET|POST|DELETE /api/backend/favorites`
- `GET|POST|DELETE /api/backend/history`
- `GET|POST /api/backend/lists`
- `GET|POST /api/backend/lists/{listId}/items`
- `GET /api/backend/reviews?bookId={bookId}`
- `GET /api/backend/reviews/mine`
- `POST /api/backend/reviews`

## API Examples
Search Android books:
```bash
curl "https://www.googleapis.com/books/v1/volumes?q=android&startIndex=0&maxResults=20&orderBy=relevance"
```

Newest Kotlin books:
```bash
curl "https://www.googleapis.com/books/v1/volumes?q=kotlin&startIndex=0&maxResults=20&orderBy=newest"
```

Backend example (dev mode using debug header auth):
```bash
curl -X POST http://localhost:8080/api/backend/favorites \
  -H "Content-Type: application/json" \
  -H "X-Debug-Uid: local-dev-user" \
  -d '{"bookId":"book-123"}'
```

## Backend API
The backend uses:
- PostgreSQL as source of truth
- Flyway migrations (`backend/src/main/resources/db/migration/V1__init_schema.sql`)
- Spring Data JPA repositories
- Firebase Admin token verification when service-account env vars are provided
- `X-Debug-Uid` fallback for local development/tests

Data model includes:
- `users`
- `favorites`
- `history`
- `lists`
- `list_items`
- `reviews`

## Firebase Auth and Firestore
Android app includes:
- Firebase Auth (email/password sign-in + sign-up + sign-out)
- Firestore cloud sync hooks for:
  - favorites
  - search history

Setup notes:
- Add `google-services.json` to `Chapterly/app/` for full Firebase runtime support.
- Without it, the Account tab shows a clear “Firebase not configured” state instead of crashing.

## Performance and Baseline Profile
This project includes a `benchmark` module for startup and scroll macrobenchmarks.

Build benchmark APKs:
```bash
cd Chapterly
./gradlew :app:assembleBenchmark :benchmark:assembleBenchmark
```

Run benchmark instrumentation:
```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest
```

Generate baseline profile from the dedicated test:
```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.chapterly.benchmark.BaselineProfileGenerator
```

## Testing
Run all quality gates and unit tests:
```bash
cd Chapterly
./gradlew detekt ktlintCheck lint testDebugUnitTest jacocoTestCoverageVerification assembleDebug
```

Run Compose instrumented tests:
```bash
./gradlew connectedDebugAndroidTest
```

Run full check task:
```bash
./gradlew check
```

Run backend tests:
```bash
cd backend
mvn -B test
```

## Getting Started
### Prerequisites
- Android Studio (latest stable recommended)
- Android SDK Platform 34 installed
- JDK 17
- Android emulator or physical Android device
- Maven 3.9+
- Docker Desktop (optional, for local Postgres)

### Quick Run (Android)
```bash
cd Chapterly
chmod +x gradlew
./gradlew clean :app:assembleDebug
./gradlew :app:installDebug
```

Launch on connected emulator/device:
```bash
"$HOME/Library/Android/sdk/platform-tools/adb" shell monkey -p com.example.chapterly -c android.intent.category.LAUNCHER 1
```

### Installation
1. Clone the repository:
```bash
git clone https://github.com/SpencerVJones/Google-Books-Listing-App.git
```
2. Move into the repo:
```bash
cd Google-Books-Listing-App
```
3. Make Gradle wrapper executable:
```bash
chmod +x Chapterly/gradlew
```

### Open in Android Studio from VS Code
From repo root:
```bash
open -a "Android Studio" "$PWD/Chapterly"
```

### Run on Emulator or Device
1. Start an emulator in Android Studio (or connect a phone with USB debugging).
2. Verify device connection:
```bash
"$HOME/Library/Android/sdk/platform-tools/adb" devices
```
3. Build and install:
```bash
cd Chapterly
./gradlew installDebug
```
4. Launch app:
```bash
"$HOME/Library/Android/sdk/platform-tools/adb" shell monkey -p com.example.chapterly -c android.intent.category.LAUNCHER 1
```

### Run Backend Locally
1. Start Postgres:
```bash
cd backend
docker compose up -d
```
2. Run backend API:
```bash
mvn spring-boot:run
```
3. Verify:
```bash
curl http://localhost:8080/api/backend/health
```

## CI
GitHub Actions workflow: `.github/workflows/android-ci.yml`

Runs on pushes and PRs:
- Backend tests (`mvn -B test` in `backend/`)
- Android quality/build pipeline:
  - `detekt`
  - `ktlintCheck`
  - `lint`
  - `testDebugUnitTest`
  - `jacocoTestCoverageVerification`
  - `assembleDebug`
- Android instrumented tests on emulator (`connectedDebugAndroidTest`)

## Troubleshooting
### `Execution failed for task ':app:parseDebugLocalResources'` with `ic_launcher_background 2.xml`
Cause:
- A copied resource file has an invalid name (spaces are not allowed in Android resource filenames).

Fix:
1. Check `Chapterly/app/src/main/res/drawable/` and remove or rename any file like `* 2.xml`, `* copy.xml`.
2. Keep only valid names (`[a-z0-9_]`), for example:
   - `ic_launcher_background.xml`
3. Rebuild:
```bash
cd Chapterly
./gradlew clean :app:assembleDebug
```

### `Duplicate resources` for `ic_launcher` (`.png` + `.webp`)
Cause:
- Both `.png` and `.webp` launcher assets exist with the same resource name in `mipmap-*`.

Fix:
1. Keep one format only (recommended: `.png` set).
2. Remove duplicate format files from all `mipmap-*` directories.
3. Rebuild:
```bash
cd Chapterly
./gradlew clean :app:assembleDebug
```

### `Unresolved class 'ChapterlyApplication'` or `MainActivity` in manifest
This project package is:
- `com.example.chapterly`

Verify these values:
- `Chapterly/app/build.gradle.kts`
  - `namespace = "com.example.chapterly"`
  - `applicationId = "com.example.chapterly"`
- `Chapterly/app/src/main/AndroidManifest.xml`
  - `android:name="com.example.chapterly.ChapterlyApplication"`
  - `android:name="com.example.chapterly.MainActivity"`

Then refresh Android Studio:
1. Open `Google-Books-Listing-App/Chapterly` (not only repo root).
2. `File > Sync Project with Gradle Files`
3. `Build > Rebuild Project`
4. If still red: `File > Invalidate Caches / Restart`

### `SDK location not found`
Create/update `Chapterly/local.properties`:
```bash
cd Chapterly
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
```

### `No connected devices` on `installDebug`
Start emulator first, then wait for full boot:
```bash
"$HOME/Library/Android/sdk/platform-tools/adb" wait-for-device
until [ "$("$HOME/Library/Android/sdk/platform-tools/adb" shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
"$HOME/Library/Android/sdk/platform-tools/adb" devices
```

### `Activity class ... does not exist`
Usually means app was not installed or wrong package/activity name was used.
Use:
```bash
cd Chapterly
./gradlew installDebug
"$HOME/Library/Android/sdk/platform-tools/adb" shell monkey -p com.example.chapterly -c android.intent.category.LAUNCHER 1
```

### Firebase Auth shows unavailable
Cause:
- `google-services.json` is missing from `Chapterly/app/`.

Fix:
1. Add your Firebase project config file to `Chapterly/app/google-services.json`.
2. Rebuild app.
3. Reopen Account tab.

### Backend cannot connect to database
Check Postgres container:
```bash
cd backend
docker compose ps
docker compose logs postgres
```

If needed, override connection env vars:
```bash
export DB_URL=jdbc:postgresql://localhost:5432/chapterly
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
```

### App crashes on launch
Capture crash logs:
```bash
"$HOME/Library/Android/sdk/platform-tools/adb" logcat -c
"$HOME/Library/Android/sdk/platform-tools/adb" shell monkey -p com.example.chapterly -c android.intent.category.LAUNCHER 1
sleep 3
"$HOME/Library/Android/sdk/platform-tools/adb" logcat -d | rg -n "FATAL EXCEPTION|AndroidRuntime|Caused by|com.example.chapterly"
```

### Hilt duplicate binding errors (`DuplicateBindings` / multiple app roots)
Cause:
- Mixed legacy and current package trees being compiled together can register duplicate DI modules or app roots.

Fix:
1. Build only the Chapterly package tree (already configured in `Chapterly/app/build.gradle.kts` source sets).
2. Run a clean build:
```bash
cd Chapterly
./gradlew clean :app:assembleDebug
```

## Usage
1. Open the app and enter a search query.
2. Scroll to load more books.
3. Change sorting or filters.
4. Open details for metadata/description/preview.
5. Favorite books and view them in the Favorites tab.
6. Relaunch offline to verify cached content and favorites persistence.

## Roadmap
- [ ] Add screenshot and GIF assets to README
- [ ] Add richer filter controls (language, print type, free ebooks)
- [ ] Add analytics-friendly event logging
- [ ] Raise JaCoCo coverage thresholds
- [ ] Add release signing and Play Store-ready build profile

See open issues for proposed features and bug fixes.

## Contributing
Contributions are welcome.
- Fork the project
- Create your feature branch (`git checkout -b feature/amazing-feature`)
- Commit your changes (`git commit -m "Add amazing feature"`)
- Push to your branch (`git push origin feature/amazing-feature`)
- Open a pull request

### Contributors
<a href="https://github.com/SpencerVJones/Google-Books-Listing-App/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=SpencerVJones/Google-Books-Listing-App"/>
</a>

## License
Distributed under the MIT License. See `LICENSE` for details.

## Contact
Spencer Jones  
Email: [jonesspencer99@icloud.com](mailto:jonesspencer99@icloud.com)  
GitHub: [SpencerVJones](https://github.com/SpencerVJones)  
Repository: [Google-Books-Listing-App](https://github.com/SpencerVJones/Google-Books-Listing-App)
