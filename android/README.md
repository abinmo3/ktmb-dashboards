# KTMB Crowd & Trend — Android App

Native Android (Kotlin + Jetpack Compose) app for KTM Komuter riders.

**MVP screens:** Forecast · Live Feed · Stations · Info

---

## Quick Start

### Prerequisites

- **Android Studio** Meerkat (2025.1+) or newer
- **JDK 17** (bundled with Android Studio)
- **Android SDK 35** with build-tools

### Build

```bash
cd android

# First run generates the Gradle wrapper jar automatically:
./gradlew assembleDebug

# Install on connected device/emulator:
./gradlew installDebug
```

### Run tests

```bash
# Unit tests (pure Kotlin, no Android SDK needed for domain tests):
./gradlew testDebug

# Instrumented tests (requires emulator):
./gradlew connectedAndroidTest
```

---

## Architecture

```
┌──────────────────────────────────────────┐
│  Screens (Compose)                        │
│  Forecast · Live · Stations · Info        │
├──────────────────────────────────────────┤
│  ViewModels (StateFlow)                   │
│  ForecastVM  LiveVM  StationsVM  InfoVM   │
├──────────────────────────────────────────┤
│  KtmbRepository                           │
│  ├─ Assets: stations, state_map, meta     │
│  ├─ Retrofit: forecast JSON on demand     │
│  ├─ Protobuf: GTFS-RT poll (30s)          │
│  └─ DataStore: service + last route       │
└──────────────────────────────────────────┘
```

### Package structure

```
com.ktmb.crowdtrend/
├── CrowdTrendApp.kt           Application
├── MainActivity.kt            Single-activity entry
├── domain/model/              Station, Forecast, LiveVehicle, etc.
├── data/
│   ├── remote/                Retrofit DTOs + API service
│   ├── local/                 DataStore preferences
│   └── repository/            KtmbRepository (StateFlow)
├── navigation/                Bottom nav + NavHost (4 screens)
├── ui/
│   ├── theme/                 Material 3 teal/emerald palette
│   ├── components/            Shared: ServiceSwitch, Heatmap, etc.
│   ├── forecast/              Screen + ViewModel
│   ├── live/                  Screen + ViewModel
│   ├── stations/              Screen + ViewModel
│   └── settings/              Screen + ViewModel
└── (test)                     Unit tests
```

### Key decisions

| Decision | Choice |
|----------|--------|
| Database | **None** — data lives in StateFlow for the session |
| JSON parsing | **kotlinx.serialization** (type-safe, no reflection) |
| HTTP | **Retrofit** with kotlinx.serialization converter |
| GTFS live feed | **Protobuf-lite** — parsed from raw bytes every 30s |
| Preferences | **DataStore** (service type + last route) |
| State management | **StateFlow** in ViewModels, consumed via `collectAsState()` |
| DI | **None** — AndroidViewModel gets Application context directly |

---

## Data Sources

| Source | Location | Format |
|--------|----------|--------|
| Station list | Bundled assets | JSON |
| State map | Bundled assets | JSON |
| Service metadata | Bundled assets | JSON |
| Forecast files | Remote (configurable `DATA_BASE_URL`) | JSON |
| GTFS live feed | `ktmb-gtfs-proxy.abinmo3.workers.dev` | Protobuf |

Core reference data (~5 KB total) is bundled as Android assets for instant offline access. Forecast files (82 files, ~2 MB total) are fetched on demand when the user selects a route.

### Data freshness

- `meta.json` contains `latest_date` (most recent ridership date)
- App compares `latest_date` to system clock
- If > 14 days behind: stale-data warning banner appears
- GTFS live feed shows freshness state: **Fresh** (< 60s), **Stale** (60–300s), **Expired** (> 300s), **Unavailable**

---

## Project validation

```bash
# Data schema + coverage validation (run from repo root):
python3 scripts/validate_data.py

# End-to-end integration test (asset bundling + GTFS proxy + forecast resolution):
python3 scripts/integration_test.py
```

---

## Attribution

- Ridership data: **data.gov.my** (Malaysia Open Data Portal)
- Live positions: **GTFS-realtime** via data.gov.my
- This app is **not affiliated with KTM Berhad**

---

## License

Project code: MIT (or as decided by the project owner)
Data: subject to data.gov.my open data terms
