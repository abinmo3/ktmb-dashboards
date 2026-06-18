# KTMB Crowd & Trend — Post-MVP Roadmap

**Date:** 2026-06-15
**Status:** Planning only — no implementation

---

## Current MVP Capabilities (Stage 0–4 complete)

| Screen | What it does |
|--------|-------------|
| **Forecast** | Origin/destination crowd heatmap, best/avoid windows, Latest vs Typical toggle, service switching, route persistence |
| **Live** | GTFS-realtime vehicle positions, freshness bar, 30s polling, coverage labels, manual refresh |
| **Stations** | Offline search by name/state, grouped-by-state directory, station detail with "Use as origin/destination" actions |
| **Settings** | Service switch, data freshness + staleness warning, source attribution, advisory disclaimers |

---

## Feature Roadmap

Each feature is assessed on: user value, implementation complexity, dependencies, data requirements, and security/privacy concerns.

---

### 1. Home Dashboard

| Dimension | Assessment |
|-----------|------------|
| **User value** | ★★★★☆ — Quick overview reduces taps for returning users |
| **Complexity** | Low — aggregating existing StateFlows into summary cards |
| **Dependencies** | ForecastViewModel (best window), LiveViewModel (vehicle count), StationsViewModel (popular stations), SettingsViewModel (service state) |
| **Data requirements** | None new — all data already loaded in other ViewModels. Could add a "popular stations" hardcoded list or derive from forecast query frequency |
| **Security/privacy** | None |

**Recommendation:** **NEXT (first post-MVP feature).** Sits naturally as an aggregator of existing screens. A single scrollable composable with: hero card, KPI row (latest data date, live train count, best window), quick-action shortcuts, and popular stations strip. Reuse all existing shared components. ~200 lines of Compose. Add as 5th bottom nav tab.

---

### 2. More Screen (Reports + Guides + Methodology)

| Dimension | Assessment |
|-----------|------------|
| **User value** | ★★★☆☆ — Secondary features; most users won't visit daily |
| **Complexity** | Medium — combines three distinct features into one screen |
| **Dependencies** | Crowd report form (see #3), guides content (already in bundled assets), methodology text |
| **Data requirements** | Guides already in assets. Reports would need local storage (see #3). Methodology is static text |
| **Security/privacy** | Low if reports are local-only; see #3/#4 for cloud concerns |

**Recommendation:** **LATER.** Gate on crowd report decision. The guides and methodology can ship independently but are low priority. Can be added as the 5th tab when Home is ready, or kept as a secondary entry point from Settings.

---

### 3. Local Crowd Reports

| Dimension | Assessment |
|-----------|------------|
| **User value** | ★★★☆☆ — Community validation of forecast data; limited value if single-user |
| **Complexity** | Medium — form UI + local persistence + display list |
| **Dependencies** | Room (SQLite) for structured storage, or simple JSON file in app-private storage. Station data (StationRepository) for dropdown |
| **Data requirements** | New entity: `CrowdReport(station, level, notes, timestamp)`. CrowdLevel enum already exists |
| **Security/privacy** | Low — data never leaves device. No PII unless user types it in notes |

**Recommendation:** **LATER (local-first).** If reports are added, start local-only. This avoids backend costs, moderation, and privacy policy. If adoption grows and users request community sharing, evaluate cloud option (see #4).

---

### 4. Cloud/Community Crowd Reports

| Dimension | Assessment |
|-----------|------------|
| **User value** | ★★★★☆ — Shared crowd data could improve forecast accuracy for everyone |
| **Complexity** | High — requires backend server, database, API, authentication, moderation |
| **Dependencies** | Backend (Firebase, Supabase, or custom), auth system, user identity, abuse prevention, privacy policy, data retention policy |
| **Data requirements** | Server-side database. User accounts or anonymous IDs. Submission rate limiting. Geo-fencing to station locations |
| **Security/privacy** | **Significant.** User-generated content with location context. Requires: privacy policy, data deletion mechanism, moderation queue, abuse reporting, compliance with Malaysian data protection laws |

#### Comparison: Local vs Cloud Reports

| Factor | Local-only | Cloud/Community |
|--------|-----------|-----------------|
| Implementation | Days | Weeks to months |
| Backend cost | $0 | $20–100+/mo (hosting, DB, auth) |
| Moderation | None needed | Required — abuse, spam, false reports |
| Privacy | Inherent — data stays on device | Needs privacy policy, data handling |
| Forecast improvement | No — single data point | Yes — aggregate community reports |
| Offline support | Full | Partial — needs sync |
| User trust | High — no data collection | Medium — requires transparency |

**Recommendation:** **REJECT for near-term.** The complexity-to-value ratio is unfavorable for an MVP+1 app. Revisit if the app reaches >1,000 active users and community reports show clear potential to improve forecast accuracy. If built, prefer a minimalist approach: anonymous submissions, no accounts, rate-limited by device fingerprint, with clear "this is community-sourced, not verified" labeling.

---

### 5. Guides Page

| Dimension | Assessment |
|-----------|------------|
| **User value** | ★★☆☆☆ — Helpful for new users; rarely revisited |
| **Complexity** | Low — guides content already bundled as `guides.json` in assets |
| **Dependencies** | None — data already present. Needs a simple detail screen with section rendering |
| **Data requirements** | Already satisfied — 3 guides with title, summary, sections |
| **Security/privacy** | None |

**Recommendation:** **LATER.** Bundle with the More screen or add as a standalone entry from Settings. Low effort but low urgency. ~100 lines of Compose for a guide detail view.

---

### 6. Full Methodology Page

| Dimension | Assessment |
|-----------|------------|
| **User value** | ★☆☆☆☆ — Important for transparency but rarely read |
| **Complexity** | Low — static markdown or composable text |
| **Dependencies** | None — content is static |
| **Data requirements** | Needs written methodology content explaining: how ridership ratios are computed, what baseline/baseline730 mean, data.gov.my source, GTFS-RT protocol, limitations |
| **Security/privacy** | None |

**Recommendation:** **LATER.** Low priority but valuable for credibility. Include as a text screen accessible from Settings or the advisory disclaimer. Should be written in plain Malay + English since the target audience is Malaysian commuters.

---

### 7. Interactive Live Map

| Dimension | Assessment |
|-----------|------------|
| **User value** | ★★★★☆ — Seeing train positions on a map is intuitive |
| **Complexity** | Medium to High — depends on map provider choice |
| **Dependencies** | Map SDK (see comparison below), GeoJSON route data (already in `gtfs_routes.json`), vehicle coordinates (already in LiveViewModel) |
| **Data requirements** | Route polyline data already available. Vehicle lat/lon already available. Map tiles require network access |
| **Security/privacy** | Map SDK may require API key. MapLibre/OSM avoids this. User location is NOT needed — only vehicle positions |

#### Comparison: Map Options

| Option | Effort | Cost | Offline | Quality | API Key Required |
|--------|--------|------|---------|---------|-----------------|
| **No map** (current) | 0 | $0 | Yes | — | No |
| **Static route card** | Low | $0 | Yes | Schematic only | No |
| **Google Maps** | Medium | Free tier (unlimited for native Android) | Limited | Excellent | **Yes** — must be in APK |
| **MapLibre** (open-source) | Medium | $0 (self-host tiles) or free tier | Yes (with MBTiles) | Good | No |

**Recommendation:** **LATER — prefer MapLibre.** Google Maps is easier to implement (Maps Compose library) but requires an API key in the APK (security concern) and locks the app to Google's ecosystem. MapLibre with OpenStreetMap tiles is fully open, requires no API key, supports offline MBTiles, and has a Compose-compatible library. If the static route card is sufficient for MVP+1, do that first — it's ~100 lines and uses existing data.

---

### 8. Favorites / Pinned Stations

| Dimension | Assessment |
|-----------|------------|
| **User value** | ★★★★☆ — Frequent commuters save taps |
| **Complexity** | Low — DataStore persistence of a string list |
| **Dependencies** | DataStore (already present). StationsViewModel to filter/show favorites |
| **Data requirements** | Store list of station names in DataStore. No new data sources |
| **Security/privacy** | None — local preferences |

**Recommendation:** **NEXT.** Very high value-to-effort ratio. Add a "☆" toggle on station detail cards, persist a `Set<String>` in DataStore, and show a "Favorites" section at the top of the Stations screen. ~50 lines of code.

---

### 9. Notifications

| Dimension | Assessment |
|-----------|------------|
| **User value** | ★★★☆☆ — Proactive alerts vs. pull-based checking |
| **Complexity** | Medium — WorkManager + NotificationChannel |
| **Dependencies** | WorkManager (already in deps). Needs a notification intent to open the Forecast screen. Needs a user-configurable trigger |
| **Data requirements** | User must set: origin, destination, preferred travel hour, crowd threshold |
| **Security/privacy** | Notification content may reveal commuting patterns if device is unlocked |

**Recommendation:** **LATER.** Notifications without accurate data are worse than no notifications. Since forecast data is ~6 months behind (historical only, not predictive), a notification saying "KL Sentral → BTS is typically crowded at 08:00" is only marginally useful. Revisit when: (a) data freshness improves or (b) live feed can trigger service-disruption alerts.

---

### 10. Widgets (Android Home Screen)

| Dimension | Assessment |
|-----------|------------|
| **User value** | ★★★☆☆ — Glanceable info without opening app |
| **Complexity** | Medium — Glance API (Jetpack) or RemoteViews |
| **Dependencies** | Glance composable library. Data must be available without launching the app |
| **Data requirements** | Must cache last-known forecast and live feed state in DataStore or a periodic worker |
| **Security/privacy** | Widget content visible on lock screen — avoid showing sensitive locations |

**Recommendation:** **LATER.** A simple widget showing "KL Sentral → BTS: Best at 10:00, Crowd: Low" would be useful but requires the app to have a reliable data refresh mechanism. Build after Favorites (so the widget knows which route to show) and after the data pipeline stabilizes.

---

### 11. Multi-Language Support (Malay + English)

| Dimension | Assessment |
|-----------|------------|
| **User value** | ★★★★★ — Essential for a Malaysian transit app |
| **Complexity** | Low to Medium — string externalization + locale-aware formatting |
| **Dependencies** | Android resource system (`values/strings.xml`, `values-ms/strings.xml`). Compose `stringResource()` |
| **Data requirements** | All user-facing strings must be extracted to resources. Station names remain in original language. State names, crowd levels, coverage labels need translation |
| **Security/privacy** | None |

**Recommendation:** **NEXT (parallel with other features).** This is not a "feature" — it's a correctness issue. A Malaysian transit app without Malay is incomplete. Start by externalizing all hardcoded English strings to `strings.xml`, then add Malay translations. Use `stringResource()` throughout Compose. This is mechanical work that can proceed in parallel with feature development.

---

## Priority Summary

### NEXT (post-MVP, high priority)

| # | Feature | Effort | Rationale |
|---|---------|--------|-----------|
| 11 | Multi-language (Malay + English) | Low-Medium | Essential for Malaysian users |
| 8 | Favorites / Pinned Stations | Low | High value/effort ratio |
| 1 | Home Dashboard | Low | Aggregator of existing data |

### LATER (valuable but not urgent)

| # | Feature | Effort | Rationale |
|---|---------|--------|-----------|
| 7 | Static route card (no interactive map) | Low | Schematic view using existing route data |
| 3 | Local crowd reports | Medium | Adds community data; local-first is safe |
| 2 | More screen | Medium | Bundles reports + guides + methodology |
| 9 | Notifications | Medium | Gate on data freshness improvements |
| 5 | Guides page | Low | Already have content in assets |
| 6 | Methodology page | Low | Static text; important for credibility |
| 10 | Widgets | Medium | Gate on favorites + data pipeline |
| 7 | Interactive map (MapLibre) | Medium-High | Better UX than static card, but more work |

### REJECT (or distant future)

| # | Feature | Effort | Rationale |
|---|---------|--------|-----------|
| 4 | Cloud crowd reports | High | Backend + moderation + privacy overhead too high for current scale |
| 7 | Google Maps | Medium | API key in APK is a security concern; MapLibre is a better fit |

---

## Implementation Sequence (Recommended)

```
Phase 6: Multi-language (Malay + English)
    ↓
Phase 7: Favorites + Home Dashboard (together — Favorites feeds Home)
    ↓
Phase 8: Static route card on Live screen
    ↓
Phase 9: Local crowd reports + More screen
    ↓
Phase 10: Guides + Methodology + Polish
    ↓
Future:  Notifications, Widgets, Interactive Map (MapLibre)
    ↓
Distant: Cloud reports (revisit at 1,000+ users)
```

---

## Risks Not Yet Addressed

| Risk | Impact | Mitigation |
|------|--------|------------|
| Data staleness (6-month lag) | Users dismiss forecasts as outdated | Add "last updated" prominently; explore more frequent data.gov.my parquet updates |
| GTFS proxy dependency (Cloudflare Worker) | Live feed goes down if worker is removed | Document fallback; consider direct data.gov.my call with CORS handling |
| No automated testing on device | Bugs surface in production | Set up GitHub Actions with Android emulator for CI |
| APK size growth | 2.8 MB assets already; MapLibre tiles would add more | Use Android App Bundles; lazy-load map tiles |
| Malaysian data privacy laws (PDPA) | If cloud reports are added, compliance required | Local-only reports avoid this entirely; cloud reports need legal review |

---

*End of Post-MVP Roadmap. No code, dependencies, or screens were added as part of this stage.*
