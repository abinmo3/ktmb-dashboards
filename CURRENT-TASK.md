# Current Task — June 2026

## Status: Established MVP — Phase 0–4 Complete

The Android app has 4 working screens (Forecast, Live, Stations, Settings). The HTML prototype has a 5-tab design with a full Malaysia SVG map.

## What's Done
- ✅ Android app builds + runs on device
- ✅ Forecast screen with heatmap, best/avoid windows, service switch
- ✅ Live screen with GTFS vehicle list, 30s auto-poll, freshness indicator
- ✅ Stations screen with search, grouped-by-state directory, detail cards
- ✅ Settings screen with service switch, data freshness, attribution
- ✅ Data pipeline (Python) for Komuter + Utara forecasts
- ✅ GTFS routes + bbox generation for Komuter + Utara
- ✅ Cloudflare Worker for GTFS-RT CORS proxy
- ✅ Data feasibility report (all 6 parquet sources inventoried)
- ✅ Data opportunities doc (8 extraordinary feature concepts)
- ✅ Post-MVP roadmap (11 features assessed)

## Next Up (Post-MVP Phase)

**Priority: Next batch**
1. **Home Dashboard** — Hero card, KPI row, quick actions, popular stations (5th tab)
2. **Favorites / Pinned Stations** — Star toggle, DataStore persistence
3. **Multi-language (Malay + English)** — string externalization
4. **ETS data integration** — Add 496K rows, 45 new stations
5. **Intercity (Jungle Railway) integration** — Add 243K rows, 83 unique stations

## Key Constraints
- Data is ~6 months historical lag — forecasts show "typical" not "predicted"
- No CI/CD for Android — manual APK builds
- No automated tests on Android
- No git history initialized in repo root
- Project lives at `C:\Users\abinm\OneDrive\Documents\01_Active_Work\Business_Projects\ktmb_dashboards`

## Open Questions
- How to handle data staleness UX (6-month lag currently)
- Whether to ship data updates as app updates or pull from a server
- Priority between ETS vs Intercity integration
