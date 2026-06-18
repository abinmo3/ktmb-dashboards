# KTMB Crowd & Trend Android App Plan

## Goal
Convert the current web dashboard into a native Android app that helps KTM Komuter riders plan trips with crowd forecasts, live train context, station browsing, and crowd reporting.

This plan follows the current product direction from the existing site and the Android preview prototype in `docs/android-preview.html`.

## Product Principles
- Mobile first, one-handed use.
- Fast answers before deep exploration.
- Trustworthy data presentation.
- Clean Android-native interactions with strong visual hierarchy.
- Keep the current product identity: teal brand accents, route/network imagery, and practical commuter-focused copy.

## Core User Jobs
1. Check whether a trip will be crowded.
2. Compare origin and destination options.
3. See live train positions and service coverage.
4. Find a station quickly.
5. Submit a crowd report from the station or platform.
6. Read short guides for travel planning.

## MVP Scope
The first Android release should include these screens:

1. Home
2. Forecast
3. Live
4. Stations
5. More

These screens map directly to the current web product and the Android preview.

## Screen Plan

### 1) Home
Purpose: give a quick overview and entry points.

Content:
- Service switch for `Komuter` and `Komuter Utara`
- Hero section with the main value proposition
- Quick actions for Forecast, Live Map, and Stations
- KPI cards for latest data date, live train count, and best window
- Popular stations strip

Primary actions:
- Check crowd forecast
- Open live map
- Open station directory

### 2) Forecast
Purpose: help riders choose the best time to travel.

Content:
- Service selector
- Origin selector
- Destination selector
- Swap route control
- Route summary
- 24-hour crowd heatmap
- Best boarding windows
- Avoid if possible windows
- Clear disclaimer about estimates

Primary actions:
- Change route
- Compare latest-day vs typical 24-month patterns
- Jump to Live or Stations for the selected station

### 3) Live
Purpose: show active vehicle context in a compact mobile format.

Content:
- Live route/map card
- Active vehicle count
- Coverage summary
- Last update time
- Recent train snapshot list
- Service status fallback state

Primary actions:
- Open forecast for the same route
- Refresh live feed automatically

### 4) Stations
Purpose: let the user search and browse stations quickly.

Content:
- Search field
- Station detail card for the selected station
- Grouped station list by state
- Actions to open forecast, live map, or report crowd

Primary actions:
- Search by station or state
- Open a station-specific forecast
- Jump into the live view

### 5) More
Purpose: capture the supporting features and secondary content.

Content:
- Crowd report form
- Recent saved reports
- Guides list
- Data / methodology note
- Disclaimer / planning-use note

Primary actions:
- Save a report locally or sync later
- Read a guide
- Review disclaimer and methodology

## Information Architecture
Recommended bottom navigation:
- Home
- Forecast
- Live
- Stations
- More

This keeps the most-used trip planning flows within one thumb reach and matches the current Android prototype.

## Visual Direction
- Material 3 foundation
- Teal / emerald primary palette
- Light surfaces with subtle depth
- Rounded cards and chips
- Strong typography hierarchy
- Transit-network background motifs used sparingly
- Compact but roomy spacing for mobile usability

## Data Sources

### Existing project data
- Static station data from `docs/data/stations.json`
- State grouping from `docs/data/state_map.json`
- Guide content from `docs/data/guides.json`
- Forecast data from `docs/data/meta.json` and `docs/data/by_origin/*.json`
- North line data from `docs/data/komuter_utara/*`

### Live data
- GTFS-realtime vehicle feed via the existing proxy used by the web app

### User-generated data
- Crowd reports saved locally at first
- Optional sync layer later if a backend is introduced

## Android Architecture
Suggested implementation stack:
- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- MVVM
- Repository pattern
- Room for cached data and saved reports
- DataStore for app preferences and selected service
- WorkManager for periodic refreshes
- Retrofit or Ktor client for JSON and live feed requests
- Protobuf support for GTFS-realtime parsing

## Suggested Package Structure
- `ui/`
  - `home/`
  - `forecast/`
  - `live/`
  - `stations/`
  - `more/`
- `data/`
  - `remote/`
  - `local/`
  - `repository/`
- `domain/`
  - models and use cases
- `core/`
  - theme, utils, and shared components

## Delivery Phases

### Phase 0: Confirm the product direction
- Approve the Android preview look and screen set
- Confirm the bottom navigation
- Confirm which features are MVP and which are later

### Phase 1: App foundation
- Create the Compose project shell
- Set up Material 3 theme
- Build navigation and bottom nav
- Create shared cards, chips, forms, and loading states

### Phase 2: Core planning flows
- Build Home
- Build Forecast
- Wire station and route selection
- Render the hourly crowd chart

### Phase 3: Live and stations
- Build the Live screen
- Integrate the live feed summary
- Build Stations search and grouped browsing
- Add station detail shortcuts

### Phase 4: More and persistence
- Build crowd report form
- Store reports locally
- Add guides and methodology screens
- Add disclaimer and feedback content

### Phase 5: Polish and release readiness
- Accessibility pass
- Offline and low-network states
- Performance checks on mid-range Android devices
- Visual QA on multiple screen sizes
- Internal beta release

## Success Criteria
The Android app is ready for the first approved build when:
- A rider can open the app and immediately understand the current service state.
- Forecasts are easy to read on a phone without horizontal scrolling.
- Live train status is visible in a compact and stable layout.
- Stations are searchable in under a few taps.
- Crowd reports can be submitted or saved without friction.
- The app still feels like the same product, not a generic transit template.

## Risks And Decisions To Resolve
- Map provider choice for the live view
- Whether crowd reports stay local-only or sync to a backend
- Whether the live feed should poll or use push-style updates later
- Whether favorites or pinned stations should be in MVP
- Whether a widget or notification should be included in the first release

## Suggested Next Step
After the UI is approved, turn this plan into:
1. A screen-by-screen Android specification, or
2. A Kotlin/Jetpack Compose starter structure.
