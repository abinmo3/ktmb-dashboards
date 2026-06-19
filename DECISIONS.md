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

## 10. Git History Initialized

**Decision:** Git repository initialized with full history on GitHub at `abinmo3/ktmb-dashboards`.
**Date:** 2026-06-18
**Context:** Project was previously in OneDrive without version control (see original Decision #10 below).
**Rationale:**
- Enables proper version control, branching, and collaboration
- Allows CI/CD via GitHub Actions
- Public visibility encourages code quality
- Protects against unrecoverable changes
**Trade-off:** Public repo requires hygiene discipline (no secrets, clean commit history).
**Previous decision (superseded 2026-06-18):** Project lived in OneDrive without git init. This was acceptable during exploratory phase but became a risk as the project matured.

## 11. This Repo Is Now a Serious Public Project

**Decision:** Treat `ktmb-dashboards` as a production-grade public repository with professional standards.
**Date:** 2026-06-19
**Rationale:**
- Repository is public on GitHub — code and history visible to anyone
- Project has real users and real data pipeline
- Professional standards prevent embarrassing mistakes (secrets in commits, junk files, stale docs)
**Impact:**
- Control files (`.hermes.md`, `AGENTS.md`, `CURRENT-TASK.md`, `DECISIONS.md`) must be maintained
- AI agents must read control files before making broad changes
- Secrets must never be committed (checked in pre-commit review)
- Generated/build artifacts must stay gitignored
- Documentation must stay up-to-date
**Status:** Active

## 12. AI Agents Must Use Control Files Before Broad Changes

**Decision:** Any AI coding agent (Hermes, Codex, Copilot, etc.) must read `AGENTS.md` and `.hermes.md` before making repo-wide changes.
**Date:** 2026-06-19
**Rationale:** Control files encode safety boundaries, conventions, and project knowledge that prevent agents from making destructive or incorrect changes.
**Impact:** Reduces risk of accidental deletions, build breakage, and style violations.
**Status:** Active

## 13. Deletion Must Be Conservative

**Decision:** Files are only deleted after categorization into safe (A), needs-confirmation (B), keep (C), and security-concern (D) groups. Only Group A gets deleted without human review.
**Date:** 2026-06-19
**Rationale:** Over-aggressive cleanup can break builds, remove valuable historical context, or delete files needed by other team members. A systematic approach prevents mistakes.
**Impact:** Cleanup takes slightly longer but is safer.
**Status:** Active

## 14. Secrets Must Not Be Committed

**Decision:** All commits are reviewed for secrets (API keys, tokens, passwords, credentials) before being pushed. Files that extract or contain credentials are never tracked.
**Date:** 2026-06-19
**Rationale:** Public repo means any committed secret is immediately exposed. `.gitignore` already covers common patterns; scripts that query credential managers (not store secrets) are acceptable.
**Impact:** `.ps1` helper scripts that use `git credential-manager` must not be tracked unless they are confirmed free of hardcoded secrets.
**Status:** Active

## 15. Generated/Build Files Should Not Be Tracked

**Decision:** All generated outputs (build artifacts, data derived from parquet, AI-generated mockups, screenshots, ZIP archives) are gitignored and must not be committed.
**Date:** 2026-06-19
**Rationale:**
- Generated files bloat the repo and create merge conflicts
- They can be regenerated from sources
- Tracking them creates a false sense of version control (changes are to the generator, not the artifact)
**Impact:** `.gitignore` covers: `*.apk`, `*.aab`, build dirs, screenshots, ZIPs, Firefly images, extracted archives, root HTML mockups, temp files. `docs/data/` and `android/app/src/main/assets/data/` are the exception — they ARE tracked because they are the app's bundled data.
**Status:** Active

## 16. The Android App Must Remain Buildable

**Decision:** No change that breaks the Android Gradle build is acceptable. After any change touching `android/`, `assembleDebug` must be verified.
**Date:** 2026-06-19
**Rationale:** The Android app is the primary deliverable. A broken build blocks all other work.
**Impact:** Pre-commit checklist includes build verification; deletions of files referenced by Gradle are prohibited.
**Status:** Active
