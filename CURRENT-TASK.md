# Current Task — June 2026

## Repository Cleanup — COMPLETED (2026-06-19)

✅ Control files created/updated (`.hermes.md`, `AGENTS.md`, `CURRENT-TASK.md`, `DECISIONS.md`)
✅ Deleted 11 gitignored artifacts (~22 MB: screenshots, mockups, ZIPs, duplicates, Firefly images)
✅ Root `gtfs-realtime.proto` removed (duplicate of `docs/vendor/gtfs-realtime.proto`)
✅ `.gitignore` updated — `.hermes/` now covers all runtime files (was only `desktop-attachments/`); `_build_apk.ps1` and `_get_token.ps1` now gitignored
✅ Secret scan passed — no secrets found, all hits are false positives
✅ Data validation runs (7/9 pass; 2 pre-existing state-map/orphan issues)
✅ Android Gradle build not verified (Java unavailable in this shell); no Android files were touched

## What's Done (Historical — Android App)
- ✅ Android app builds + runs on device (6 screens)
- ✅ **Home/Network dashboard** — KPI row, quick actions, trending stations, last-route resume, service switcher
- ✅ **Transit Alarms** — GPS proximity alarm system with foreground service, configurable radius, notifications
- ✅ **6-tab bottom navigation** (Network, Forecast, Live, Stations, Alarms, Info)
- ✅ Forecast screen with heatmap, best/avoid windows, service switch
- ✅ Live screen with GTFS vehicle list, 30s auto-poll, freshness indicator
- ✅ Stations screen with search, grouped-by-state directory, detail cards
- ✅ Settings/Info screen with service switch, data freshness, attribution
- ✅ Data pipeline (Python) for Komuter + Utara forecasts
- ✅ GTFS routes + bbox generation for Komuter + Utara
- ✅ Cloudflare Worker for GTFS-RT CORS proxy
- ✅ Data feasibility report (all 6 parquet sources inventoried)
- ✅ Data opportunities doc (8 extraordinary feature concepts)
- ✅ Post-MVP roadmap (11 features assessed)
- ✅ Control files created (AGENTS.md, .hermes.md, DECISIONS.md, CURRENT-TASK.md)
- ✅ Repository cleanup & public hygiene pass completed

## Next Up (Post-Cleanup Features)
1. **ETS data integration** — Add 496K rows, 45 new stations to the app
2. **Intercity (Jungle Railway) integration** — Add 243K rows, 83 unique stations
3. **Favorites / Pinned Stations** — Star toggle, DataStore persistence
4. **Multi-language (Malay + English)** — string externalization
5. **Automated tests on Android** — JUnit + Compose testing

## Known Issues / Next Maintenance
| # | Issue | Priority | Notes |
|---|-------|----------|-------|
| 1 | `validate_data.py`: 9 Komuter stations missing from `state_map` | Low | Segambut Utara, Kulai, Layang-Layang, Tapah Road, Sungkai, Kampar, Kempas Baru, Pasir Gudang, Segambut 2 need state_map entries |
| 2 | `validate_data.py`: orphan forecast file `rengam.json` | Low | Forecast file exists but no matching station — may be a retired/renamed station |
| 3 | No Android CI in GitHub Actions | Medium | Manual builds only; add Gradle build step to CI |
| 4 | `_build_apk.ps1` and `_get_token.ps1` untracked + gitignored | Low | Local scripts with machine-specific paths; not suitable for public repo |
| 5 | `.hermes/` directory gitignored | Low | Hermes runtime attachments — never commit |

## Key Constraints
- Data is ~6 months historical lag — forecasts show "typical" not "predicted"
- No CI/CD for Android — manual APK builds
- No automated tests on Android
- Project lives on GitHub at `abinmo3/ktmb-dashboards` (public)

## Open Questions
- How to handle data staleness UX (6-month lag currently)
- Whether to ship data updates as app updates or pull from a server
- Priority between ETS vs Intercity integration
