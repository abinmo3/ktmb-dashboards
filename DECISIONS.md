# KTMB Pulse — Architectural & Design Decisions

## 1. Data as Bundled JSON (not live API)

**Decision:** Process parquet → structured JSON → bundle in Android assets.
**Date:** 2026-06-14
**Context:** The KTMB ridership data from data.gov.my is historical parquet (updated ~quarterly).
**Rationale:**
- No server costs — forecasts are computed offline and shipped with the app
- Instant load — no network dependency for forecast data
- Works offline completely for forecast and stations
- Simple data pipeline (Python → JSON)
**Trade-off:** Data is always behind (6+ months). No real-time forecast updates without an app update.
**Alternatives considered:** Server-side API (cost, complexity), live parquet parsing on device (heavy).

## 2. No DI Framework on Android

**Decision:** Manual dependency injection via `AppContainer` pattern.
**Date:** 2026-06-14
**Context:** Small app with ~4 screens and limited dependencies.
**Rationale:**
- Avoids Hilt/Dagger/Koin learning curve and build time overhead
- Transitive dependencies (Hilt + kapt) add APK size and compile time
- App is small enough that manual DI is manageable (~5-6 ViewModels)
- Can migrate to Hilt later if complexity grows
**Trade-off:** More boilerplate when adding new dependencies; no compile-time graph validation.

## 3. HTML Prototype in `docs/` (GitHub Pages)

**Decision:** Maintain an HTML prototype alongside the Android app.
**Date:** 2026-06-14
**Context:** Rapid UI exploration before committing to Android Compose implementation.
**Rationale:**
- Faster iteration on visual design and UX patterns
- Shareable as a GitHub Pages link without installing Android tooling
- Serves as reference implementation for Compose screens
- All data processing already produces JSON consumable by both platforms
**Trade-off:** Divergence risk — HTML features that don't make it to Android; maintenance burden of two UIs.

## 4. Cloudflare Worker for GTFS-RT Proxy

**Decision:** Deploy a Cloudflare Worker to add CORS headers to the GTFS-Realtime feed.
**Date:** 2026-06-14
**Context:** The data.gov.my GTFS-RT endpoint doesn't include CORS headers, blocking browser requests.
**Rationale:**
- Free tier (no cost for current usage level)
- Adds `Access-Control-Allow-Origin: *` and caching headers
- Minimal code (~30 lines JS)
- Can be extended later for transformations or filtering
**Trade-off:** Additional dependency on Cloudflare; potential downtime if worker is removed.

## 5. Compose Canvas over Google Maps for Station Visualization

**Decision:** Use custom Compose Canvas/Path rendering instead of Google Maps SDK.
**Date:** 2026-06-14 (tentative — not yet implemented for route map)
**Context:** Need to display station locations and route lines on a map.
**Rationale:**
- No API key required (security + no Google dependency)
- Offline-capable (no map tile downloads)
- Full design control over styling (dark theme, custom markers, animations)
- Route polylines already available as coordinate arrays in `gtfs_routes.json`
- Station positions available as lat/lon
**Trade-off:** No street-level detail; no turn-by-turn navigation; manual coordinate projection math needed.
**Update (2026-06-15):** MapLibre evaluated as an alternative for interactive maps. Static route card preferred for MVP+1, MapLibre for later if interactive map becomes a priority.

## 6. Dark Theme Only (No Light Mode)

**Decision:** App uses a single dark theme (`#080D14` background, glass-morphism cards).
**Date:** 2026-06-14
**Context:** Crowd-forecast app primarily used in transit environments (low light, on-the-go).
**Rationale:**
- Simpler to maintain (one color system)
- Heatmap colors (green→yellow→red→purple) pop better on dark backgrounds
- Battery-efficient on AMOLED screens
- Consistent with modern Malaysian transit apps
**Trade-off:** Users who prefer light mode cannot switch.

## 7. Local-Only Crowd Reports (No Cloud Backend)

**Decision:** If crowd reports are added, they will be local-only (Room/DataStore → no server).
**Date:** 2026-06-15
**Context:** Feature request for users to submit crowd-level reports at stations.
**Rationale:**
- Zero backend cost
- No privacy/data protection compliance needed (data never leaves device)
- No moderation overhead
- Works offline
- Can be upgraded to cloud if adoption justifies it (>1,000 users)
**Trade-off:** No community-aggregated data benefit; single-user value only.

## 8. Komuter Utara as Separate Module (not integrated with Komuter)

**Decision:** Komuter Utara treated as a separate service with its own data pipeline, stations, and forecast files.
**Date:** 2026-06-14
**Context:** The two services have separate parquet data sources, station sets, and GTFS route files.
**Rationale:**
- Data sources are published separately by data.gov.my
- Station sets are distinct (58 Komuter vs 23 Utara)
- Service switching UI already handles per-service data loading
- Avoids complexity of merging parallel-but-separate datasets
**Trade-off:** Code duplication in data loading; user must switch services to see different lines.

## 9. ETS and Intercity Integration Deferred

**Decision:** ETS (496K rows) and Intercity (243K rows) data will not be integrated in the MVP.
**Date:** 2026-06-15
**Context:** Data exists and is processed, but not consumed by the app.
**Rationale:**
- Current app handles only Komuter + Utara
- Adding ETS/Intercity requires updating data pipeline, stations list, forecast files, and Android UI
- Focus on polishing existing experience before expanding scope
- The data opportunities doc identifies this as high-impact deferred work
**Trade-off:** 72 stations invisible; 739K ridership records unused; Jungle Railway feature deferred.

## 10. No Git History Initialized

**Decision:** Project lives in OneDrive without git init.
**Date:** 2026-06-14
**Context:** Initial development was exploratory/experimental.
**Rationale:**
- Started as data exploration notebooks and HTML prototypes
- OneDrive syncing provides basic version history
- Project wasn't originally planned as a multi-developer project
**Trade-off:** No branching, no PRs, no CI/CD beyond the data workflow; risk of unrecoverable changes.
**Recommendation:** Initialize git when adding major features (Home Dashboard, ETS integration).
