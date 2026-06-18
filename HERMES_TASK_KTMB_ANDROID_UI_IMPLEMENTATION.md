# Hermes Desktop Agent Task File

## Project
KTMB Crowd & Trend — Android App UI/UX implementation

## Mission
Build a clean Android-style app prototype from the approved Figma/mobile preview direction. The target is a phone-first Android UI, not a desktop dashboard.

## Figma Design Reference
Figma file:
https://www.figma.com/design/9h2i4ZGdJd3rxjmZnBmI8x

Use this Figma file as the visual source of truth for spacing, hierarchy, colors, cards, and screen structure.

## Supporting Preview Files
Use the attached/source files if available:
- `ktmb-actual-android-phone-preview.html`
- `ktmb_actual_android_phone_preview_source.zip`
- `ANDROID_APP_PLAN.md`

## Build Target
Create an Android-style prototype using one of the following, depending on the existing project stack:

### Preferred if building native Android
- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose

### Acceptable if only producing preview/prototype first
- HTML
- CSS
- JavaScript
- Mobile-first responsive preview

Do not create a desktop dashboard layout. The output must look and behave like an Android phone app.

## Screens To Build
Create these five mobile screens:

1. Home Dashboard
2. Crowd Forecast
3. Live Map
4. Stations
5. Alerts & Reports

Use bottom navigation for these screens.

## Required UI/UX Rules
- Use a phone-first 390px Android preview frame or native Android dimensions.
- Use Android-style status bar, app bar, scrollable content, and bottom navigation.
- Use Material-style cards, chips, rounded fields, and clear visual hierarchy.
- Keep the KTMB teal/emerald identity.
- Keep strong contrast and readable typography.
- No desktop top navigation as the primary structure.
- No wide multi-column dashboard layout for mobile screens.
- Avoid copying proprietary KTM/KTM Berhad assets unless user supplies licensed assets.
- Use original icons/shapes or simple placeholder vector icons.

## Visual Direction
- Primary color: deep teal / emerald.
- Background: warm light surface.
- Cards: white with subtle border/shadow.
- Accent colors:
  - Green: low / on time
  - Yellow/orange: moderate / delayed
  - Red: high crowd / severe delay
  - Blue: live route information
- Rounded corners: 16–28px equivalent.
- Typography: Material-like, preferably Inter/Roboto.

## Screen Details

### 1. Home Dashboard
Purpose: quick overview and entry point.

Required sections:
- Hero card with value proposition: “Plan better. Travel smarter.”
- Journey search card: From, To, Date, Search button.
- Service highlights: Crowd Forecast, Live Map, Stations, Alerts.
- Trending stations cards with crowd percentages.
- Bottom navigation with Home active.

### 2. Crowd Forecast
Purpose: help rider choose the best travel time.

Required sections:
- Origin and destination selectors.
- Swap route control.
- Time window summary.
- Best travel window card.
- Avoid travel window card.
- 24-hour crowd heatmap.
- Latest vs typical pattern mini cards.
- Bottom navigation with Forecast active.

### 3. Live Map
Purpose: compact live route context.

Required sections:
- Mobile map-style canvas or placeholder.
- Route line with station markers.
- Journey overview card.
- Live train update cards.
- Line crowd summary card.
- Bottom navigation with Live active.

### 4. Stations
Purpose: quick station search and browsing.

Required sections:
- Search field.
- Selected station detail card.
- Buttons for Open Forecast and Report.
- Grouped station list by state.
- Bottom navigation with Stations active.

### 5. Alerts & Reports
Purpose: report crowd and view saved reports.

Required sections:
- Crowd report form.
- Station field.
- Crowd level chips: Low, Moderate, Busy, Packed.
- Save report button.
- Recent saved reports.
- Quick guide / methodology note.
- Bottom navigation with Alerts active.

## Interaction Requirements
For prototype:
- Bottom navigation switches screens.
- Service switch updates labels where practical.
- Forecast route swap button swaps origin and destination.
- Station search filters station list.
- Crowd level chips update selected state.
- Save report stores a local report in browser/local state or Android Room/DataStore if native.

## Data Handling For MVP
Use mock/sample data first unless existing project data is already available.

Sample stations:
- KL Sentral
- Shah Alam
- Seremban
- Ipoh
- Butterworth
- Rawang
- Tanjung Malim
- Tapah Road

Sample crowd levels:
- Low
- Moderate
- Busy
- Packed

Sample services:
- Komuter Klang Valley
- Komuter Utara

## Deliverables
Produce these files or equivalent project changes:

### If HTML prototype
- `index.html`
- `styles.css`
- `app.js`
- Any assets placed in `assets/`

### If Kotlin/Compose app
- Compose screen files for Home, Forecast, Live, Stations, Alerts.
- Shared components for cards, chips, app bar, bottom navigation, heatmap.
- Theme file with colors and typography.
- Navigation graph.
- Mock repository/data model.

## Quality Bar
The result is acceptable only if:
- It clearly looks like an Android mobile app.
- It has all five screens.
- It is usable on a phone-sized viewport without horizontal scrolling.
- Navigation works.
- Cards, spacing, colors, and typography match the Figma direction.
- The design does not revert into desktop dashboard style.

## Recommended Hermes Model Assignment
Use token-saving model routing:

- DeepSeek V4 Flash: inspect existing files, make small edits, implement repeated UI components, clean CSS/Compose components.
- DeepSeek V4 Pro: create or refactor the full screen architecture, fix layout problems, implement navigation and state management, resolve difficult bugs.

Suggested first run:
Use DeepSeek V4 Pro for the first implementation pass because this is a visual architecture task across five screens.

## First Prompt To Run In Hermes
You are building the KTMB Crowd & Trend Android app UI/UX prototype. Read this task file carefully. Use the Figma URL and attached preview files as the visual reference. Build a phone-first Android-style UI with five screens: Home Dashboard, Crowd Forecast, Live Map, Stations, and Alerts & Reports. Do not create a desktop dashboard. Implement working bottom navigation and basic prototype interactions. Keep the teal/emerald KTMB visual identity, Material-style cards, chips, heatmap, mobile map preview, station search, and report form. After implementation, provide a concise summary of files changed and how to preview the result.
