# KTMB Crowd & Trend — Data Opportunities & Extraordinary Features

**Date:** 2026-06-15
**Source:** [data.gov.my](https://data.gov.my/data-catalogue) — Malaysia Open Data Portal

---

## The Hidden Goldmine: What We Discovered

The current app (MVP) only uses **2 of 6 available datasets** (Komuter and Komuter Utara). Here's what else exists:

| Dataset | Rows | Size | Coverage | Used? |
|---------|------|------|----------|-------|
| `komuter_2024.parquet` | 2,686,243 | 5.2 MB | Jan–Dec 2024 | ❌ |
| `komuter_2025.parquet` | 2,115,822 | 2.6 MB | Jan–Dec 2025 | ✅ (forecast) |
| `komuter_utara_2024.parquet` | 1,320,398 | 1.4 MB | Jan–Dec 2024 | ❌ |
| `komuter_utara_2025.parquet` | 1,417,899 | 1.4 MB | Jan–Dec 2025 | ✅ (forecast) |
| **`ets_2025.parquet`** | **496,689** | **691 KB** | **Jan–Dec 2025** | **❌ UNUSED** |
| **`intercity_2025.parquet`** | **242,509** | **390 KB** | **Jan–Dec 2025** | **❌ UNUSED** |
| GTFS-realtime (live) | — | — | Real-time | ✅ |

**Total untapped: 739,198 records + 2 full years of historical comparison.**

---

## Shocking Discoveries in the Data

### 1. Komuter RIDERSHIP IS DECLINING

```
Komuter 2024: 12,182,679 rides
Komuter 2025: 10,045,207 rides
Change: -17.5%
```

**But Komuter Utara grew 9.2%** — suggesting ridership is shifting north.

### 2. Peak Hours Reveal Surprising Behaviour

| Service | Peak Hours | Busiest Hour | Implication |
|---------|------------|-------------|------------|
| **Komuter** | **19:00, 08:00, 07:00** | **19:00** | Evening peak > morning — leisure/return riders dominate |
| **Utara** | 07:00, 06:00, 17:00 | 07:00 | Traditional commuter pattern (AM > PM) |
| **ETS** | 08:00, 15:00, 18:00 | 08:00 | Morning peak, then spread (inter-city) |
| **Intercity** | 16:00, 20:00, 10:00 | 16:00 | Afternoon departure pattern |

### 3. The Hidden Network — 153 Stations Across 4 Services

The current app covers 81 stations (58 Komuter + 23 Utara).

**72 stations are invisible** — ETS and Intercity routes that connect the entire country.

### 4. Transfer Hubs — Stations Where Networks Meet

These stations connect **multiple services**, making them prime for journey planning:

| Station | Services | Role |
|---------|----------|------|
| **KL Sentral** | Komuter, ETS, Intercity | 🇲🇾 National hub — 3.4M+ annual boardings |
| **Seremban** | Komuter, ETS, Intercity | Gateway to Negeri Sembilan |
| **Kajang** | Komuter, ETS, Intercity | Selangor connection hub |
| **Tanjong Malim** | Komuter, ETS, Intercity | Perak gateway |
| **Sungai Buloh** | Komuter, ETS | MRT/Putra connection point |
| **Butterworth** | Utara, ETS | Northern transport hub |
| **Gemas** | ETS, Intercity | 🇲🇾 Peninsula split point (West → East Coast) |
| **Padang Besar** | Utara, ETS | 🇹🇭 Thai border gateway |
| **Ipoh** | ETS, Utara | 🇲🇾 Perak capital |
| **JB Sentral** | ETS, Intercity | 🇸🇬 Singapore border gateway |

### 5. The Jungle Railway — Malaysia's Most Scenic Train

The **Intercity (Gemas→Tumpat)** route traverses Malaysia's interior — through Gua Musang, Kuala Lipis, Dabong, and the Kelantan heartland. These stations have **zero digital presence** in any transit app. This is an unexplored feature.

### 6. Year-over-Year Comparison Available

We now have **2024 data** for Komuter and Utara. We can build year-over-year trend charts showing:
- How ridership patterns shifted
- Which stations grew or declined
- Seasonal patterns across holidays

---

## Extraordinary Feature Concepts

These go beyond standard "show a heatmap" — they're designed to be **remarkable, shareable, and genuinely useful**.

---

### 🔥 CONCEPT 1: "CrowdFlow" — Network Live Visualization

**What it is:** A force-directed graph of the entire KTM network (all 153 stations) that shows crowd flow dynamics in real-time or simulation.

**How it works:**
- Nodes = stations, sized and colored by current crowd level (green→yellow→red→purple)
- Edges = routes between stations, colored by congestion
- Built-in 24-hour animation slider: watch crowds pulse through the network
- "Heat Seeker" mode: tap any station → show where its crowds flow to across the network

**Why it's genius:**
- No Malaysian transit app shows the **entire network** in one interactive view
- The 24-hour animation is inherently shareable (social media ready)
- Reveals patterns that static heatmaps hide
- Works offline with forecast data + real-time with GTFS

**Complexity:** Medium-High (Canvas-based graph rendering in Compose)
**Data needed:** All 6 datasets + GTFS routes
**Dependencies:** Graph layout engine (custom or library)

```
Demo: Imagine a video of 153 dots pulsing through the day.
       At 8AM, KL Sentral glows red as trains flow out.
       At 12PM, everything is blue/green.
       At 7PM, Komuter stations turn orange as riders return home.
```

---

### 🔥 CONCEPT 2: "My KTMB Year" — Personal Commute Chronicle

**What it is:** A personalized "Spotify Wrapped"-style annual report for your commute.

**How it works:**
- Based on the stations you search most (DataStore tracking) or your saved routes
- Generates an animated infographic:
  - "You checked KL Sentral → BTS 147 times this year"
  - "Your best travel window was 10:00 AM — 47% less crowded"
  - "This year, the crowd at 8 AM was 22% higher than last year"
  - "You avoided peak hour 63 times — saving an estimated 12 hours of standing"
  - "Rarest station you searched: YYY"
  - "KTM Ridership this year: 10 million journeys"

**Why it's genius:**
- Viral sharing potential (screenshot → social media)
- Builds emotional connection to data
- Rewards frequent usage
- Spotify Wrapped proved this format works (people love self-data)

**Complexity:** Low (static composable with personalized stats)
**Data needed:** Local usage + 2024–2025 datasets
**Dependencies:** None

---

### 🔥 CONCEPT 3: "Golden Hour Finder" — Network-Wide Best Travel Time

**What it is:** Instead of per-route best windows, find the **network-wide** golden hour.

**How it works:**
- Analyze the entire network (all 4 services) and find the hour where **total system-wide crowding is lowest**
- Present as a "Today's Sweet Spot" card:
  - "The KTM network is most relaxed at **10:00 AM**. Crowds are 63% below peak."
  - Color-coded timeline of the day
  - Comparison: "If everyone shifted 1 hour, the system could handle 18% more passengers"

**Why it's genius:**
- Shifts thinking from "my route" to "our network"
- Educational — helps users understand system-wide dynamics
- Could influence actual commuting behaviour
- Simple enough for any user to understand

**Complexity:** Low (just a network-wide aggregation)
**Data needed:** All 6 datasets

---

### 🔥 CONCEPT 4: "Ripple" — What-If Departure Time Engine

**What it is:** See the crowd **RIPPLE** effect across the entire network when you change your departure time by 1 hour.

**How it works:**
1. User selects origin + destination + current time
2. App shows crowd prediction at current time
3. User drags a slider to "7:00 PM" instead
4. The ENTIRE network animates — stations change colour, routes adjust
5. "If you leave at 7 PM instead of 6 PM: BTS platform crowd drops 60%, KL Sentral platform crowd stays same, BUT Seremban→KL crowd increases 12%"
6. **Trade-off meter**: shows total system-wide impact

**Why it's genius:**
- Novel visualization — no transit app shows system-level consequences of individual decisions
- Gamifies crowd avoidance
- Genuinely useful — helps riders understand trade-offs
- The "ripple" metaphor is intuitive and visual

**Complexity:** High (requires pre-computing all origin×destination×hour combinations)
**Data needed:** All 6 datasets

---

### 🔥 CONCEPT 5: "Network Weather" — 24-Hour Crowd Forecast

**What it is:** A weather-forecast-style presentation of the entire KTM network.

**How it works:**
- Map of Peninsular Malaysia with stations overlaid
- 24-hour slider showing animated crowd "isobars" across the network
- Legend: Blue (smooth) → Green (light) → Yellow (moderate) → Orange (busy) → Red (packed) → Purple (critical)
- Text summary generated by LLM:
  - "**Klang Valley**: Scattered crowds, heaviest at KL Sentral from 7:30–9:00 AM and 5:30–8:00 PM"
  - "**North Line**: Isolated heavy loading at Ipoh from 8–9 AM"
  - "**East Coast (Jungle Line)**: Clear sky — mild ridership throughout the day"
  - "**ETS**: Moderate pressure in the morning corridor. Afternoon easing."

**Why it's genius:**
- Familiar format (everyone understands weather forecasts)
- Abstract → accessible (no need to understand heatmap scales)
- Shareable — tweet this as "Today's KTM Crowd Forecast"
- Generates natural-language description automatically

**Complexity:** Medium (map render + LLM summary generation)
**Data needed:** All 6 datasets

---

### 🔥 CONCEPT 6: "The Jungle Line" — Rediscovering Malaysia's Interior

**What it is:** A dedicated feature promoting the **Intercity (Jungle Railway)** — Gemas → Tumpat through Malaysia's interior.

**Why this matters:**
- 83 unique stations only reachable by Intercity
- Passes through **Gua Musang, Kuala Lipis, Dabong** — places with no other rail access
- One of Malaysia's most scenic train rides (through rainforest, mountains, rivers)
- **Zero digital presence** — no transit app covers these routes

**How it works:**
- "Explore Malaysia" screen showing the Jungle Railway route on a scenic map
- Station profiles with photos (crowd-sourced or Wikipedia)
- Trip planner: "Gemas → Tumpat in 8 hours, 23 stops, RM 34"
- "Ridership by station" — show which stops are busiest
- "Hidden gem" spotlights: stations worth getting off at

**Why it's genius:**
- Differentiates from every other Malaysian transit app (none cover Intercity)
- Educational — most Malaysians don't know about the Jungle Railway
- Tourism value — could promote domestic travel
- The data is already available, just not used

**Complexity:** Medium (needs route map + station detail pages)
**Data needed:** Intercity 2025 parquet

---

### 🔥 CONCEPT 7: "Ridership DNA" — Visual Fingerprint for Every Station

**What it is:** Each station gets a unique **24-hour ridership fingerprint** visualization.

**How it works:**
- For each of the 153 stations, compute the 24-hour normalized ridership profile
- Render as a circular "DNA helix" — hours 0–23 wrapped in a ring
- Color: Low=blue, Mid=green, High=yellow, Peak=red
- Compare two stations side by side: "KL Sentral vs Ipoh — see the difference"

**Why it's genius:**
- Beautiful, unique visual language
- Station comparison becomes intuitive
- Shareable — "This is KL Sentral's crowd DNA" (screenshot viral material)
- Enables clustering: which stations have similar patterns? (commuter hubs vs residential vs mixed)

**Complexity:** Medium (Canvas circular chart)
**Data needed:** All 6 datasets

---

### 🔥 CONCEPT 8: "Route Explorer" — The Complete KTM Route Map

**What it is:** Replace the station search with a full interactive route map showing ALL KTM train routes across Malaysia.

**How it works:**
- SVG/Compose Canvas map of Peninsular Malaysia
- 4 route layers you can toggle: Komuter, Komuter Utara, ETS, Intercity
- Each station is a dot — tap for crowd data, routes, schedules
- Route lines curve along actual geography
- Zoom in for KL Sentral area, zoom out for full network
- "Services at this station" badges (e.g., KL Sentral = 🟢Komuter 🟠ETS 🟣Intercity)

**Why it's genius:**
- First time ALL KTM services are shown in one view
- Reveals the true scale of the network
- Educational — helps riders discover routes they didn't know existed
- Beautiful as a standalone feature

**Complexity:** Medium-High (route rendering + Compose Canvas)
**Data needed:** 6 datasets + GTFS routes.json

---

## Implementation Roadmap

### Phase: Immediate (low effort, high impact)

| Feature | Effort | Impact | Why |
|---------|--------|--------|-----|
| **Year-over-Year trends** | 1 day | ★★★★ | Show +9.2% Utara vs -17.5% Komuter — this is newsworthy |
| **Network-Wide Golden Hour** | 1 day | ★★★★★ | Simple calculation, immediately useful |
| **My KTMB Year (mini version)** | 2 days | ★★★★★ | Personalization, viral potential |
| **Add ETS data to app** | 3 days | ★★★★ | 45 new stations, at least (ETS forecasts) |
| **Network Weather text summary** | 2 days | ★★★★ | Novel presentation, shareable |

### Phase: Differentiator (medium effort)

| Feature | Effort | Impact | Why |
|---------|--------|--------|-----|
| **CrowdFlow network graph** | 1-2 weeks | ★★★★★ | Most visually impressive feature |
| **Route Explorer map** | 2 weeks | ★★★★★ | Complete network visualization |
| **The Jungle Line feature** | 1 week | ★★★★ | Unique differentiator |
| **Ridership DNA fingerprints** | 1 week | ★★★★ | Beautiful visual language |

### Phase: Visionary (high effort, long-term)

| Feature | Effort | Impact | Why |
|---------|--------|--------|-----|
| **Ripple what-if engine** | 3-4 weeks | ★★★★★ | Most technically impressive |
| **Full My KTMB Year** | 2 weeks | ★★★★ | Requires long user history |
| **Interactive network animation** | 3 weeks | ★★★★★ | Viral potential |

---

## The "Genius Pitch" — One-Page Summary

```
┌─────────────────────────────────────────────────────────────┐
│              KTMB Crowd & Trend — Beyond MVP                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Current: 2 services, 81 stations, per-route heatmaps       │
│                                                             │
│  Available: 4 services, 153 stations, 8M+ ridership records │
│             2 years of history, live GTFS positions           │
│                                                             │
│  ❌ What no one does:                                        │
│     • Show the entire KTM network in a single interactive    │
│       visualization                                          │
│     • Cover the Jungle Railway (Gemas→Tumpat)               │
│     • Present crowd as "network weather"                     │
│     • Let users see the system-wide ripple of their choices  │
│                                                             │
│  ✅ What we can build:                                       │
│     • CrowdFlow — animated 153-station network graph         │
│     • Network Weather — 24h forecast like a weather report   │
│     • Ridership DNA — visual fingerprint per station         │
│     • My KTMB Year — personal Wrapped-style commute report  │
│     • The Jungle Line — rediscover Malaysia's interior       │
│     • Ripple — what-if departure time engine                 │
│     • Route Explorer — complete KTM route map                │
│     • Year-over-Year trends — full 2024 vs 2025 analysis     │
│                                                             │
│  Data volume: ALL 6 parquets = 11.6 MB (small for modern    │
│  phones). Could bundle everything offline.                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Data Inventory Summary

| Source | Rows | What It Covers | Currently Used |
|--------|------|----------------|---------------|
| `komuter_2024.parquet` | 2.7M | Klang Valley, 2024 | ❌ |
| `komuter_2025.parquet` | 2.1M | Klang Valley, 2025 | ✅ |
| `komuter_utara_2024.parquet` | 1.3M | Northern line, 2024 | ❌ |
| `komuter_utara_2025.parquet` | 1.4M | Northern line, 2025 | ✅ |
| `ets_2025.parquet` | 497K | ETS KL–Padang Besar–JB, 2025 | ❌ |
| `intercity_2025.parquet` | 243K | Jungle Railway + Intercity, 2025 | ❌ |
| GTFS-realtime | Real-time | Live vehicle positions | ✅ |

**Absolute total: 8,279,561 ridership records, 153 unique stations, 4 services.**

---

## Key Technical Decisions

1. **Bundle all parquets as processed JSON** (same pattern as current app): ~12 MB total
2. **2024 vs 2025 comparison** means every screen can show trend arrows (↑↓ year-over-year)
3. **ETS and Intercity forecasts** can use the same `by_origin/{slug}.json` pattern
4. **GTFS routes already have polyline coordinates** — can render on a Compose Canvas map
5. **153 stations** is manageable for a force-directed graph (performance is fine on mid-range phones)

---

*End of Data Opportunities document.*
