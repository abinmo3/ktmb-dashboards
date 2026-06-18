# KTMB Pulse — Current UI/UX State (June 2026)

## HTML Prototype (`docs/ktmb-pulse-upgraded.html`)
This is the most complete visual prototype. It's a **dark premium theme** with a full 5-tab bottom navigation:

| Screen | Content |
|-----|---------|
| **Network** (Home) | Hero "The KTM network across Peninsular Malaysia" · 153 stations · 4+2 services · 8.2M journeys · Malaysia SVG map with service layer toggles · YoY ridership trends (Selatan ↓15.8%, Utara ↑22.1%) · **KPI row (stations, live trains, ridership)** · **Quick actions (Find Route, Live Map, Alarms)** · **Trending stations strip** · **Last-route resume** |
| **Forecast** | Origin/destination route selectors · 24h crowd heatmap with Latest/Typical toggle · Best/Avoid windows |
| **Live** | GTFS-realtime vehicle feed with vehicle list |
| **Stations** | Search by name/state · grouped by state · click for detail card with "Use as origin/destination" |
| **Alarms** 🔥 | **Station search + picker** · **Configurable radius (500m/1km/2km)** · **Foreground GPS service** · **Proximity alarm notification** · **Enable/disable/delete alarms** |
| **Info** | Settings · Data freshness · Attribution |

Service filter: All / Komuter / Utara / ETS / Intercity / Shuttle / BRT — scrollable pills.

### Design System
- **Palette**: `#080D14` bg · glass-morphism cards `rgba(22,34,49,0.85)` with 24px blur · white text
- **Accents**: Teal `#0D9488` · Amber `#F59E0B` · Indigo `#6366F1` · Purple `#8B5CF6` · Pink `#EC4899` · Cyan `#06B6D4`
- **Malaysia Map**: SVG path with real coords (40+ point path) + station markers
- **Typo**: Inter, 300–900 weight

## Android Compose App (`android/`)
Currently has **4 screens** via bottom nav (Forecast / Live / Stations / Info/Setting):
- **Forecast** ✓ — Route picker with service switch · heatmap · Best/Avoid windows · data freshness
- **Live** ✓ — GTFS vehicle list · freshness indicator · stats row · 30s auto-poll
- **Stations** ✓ — Search · grouped list · detail card with origin/destination navigation
- **Settings** ✓ — Service switch · data freshness · attribution

## Gaps
| Screen | Spec says | Have? |
|--------|-----------|-------|
| Home Dashboard | Hero card, journey search, service highlights, trending stations | ❌ (empty ui/home/) |
| Alerts & Reports | Report form, crowd chips, saved reports | ❌ (is "Info" not "Alerts") |
| Live Map | Map with route lines, station markers, journey card | ❌ (vehicle list only, no map) |
| 5-tab nav | Home + Forecast + Live + Stations + Alerts | ❌ (4 tabs, starts on Forecast) |

## Screenshot captured
I just captured a browser screenshot — MEDIA:C:\Users\abinm\AppData\Local\hermes\cache\screenshots\browser_screenshot_de6fc458cd11401fa7caea52f6c4c362.png
