# Project Map

## Entry points
- `docs/index.html` — Main crowd heatmap + live map page.
- `docs/reco.html` — Recommendation-focused page (best/avoid windows).

## Data locations
- `docs/data/*` — Generated JSON used by the frontend (stations, meta, per-origin data, routes, bbox).

## Data generation
- `scripts/build_data.py` — Builds `docs/data/*.json` from parquet ridership sources.
- `scripts/build_gtfs_routes.py` — Builds `docs/data/gtfs_routes.json` + `docs/data/bbox.json` from GTFS static data.
- `.github/workflows/build-data.yml` — Scheduled workflow that runs the scripts and commits updates to `docs/data`.

## Live feed
- `src/index.js` — Cloudflare Worker proxy for GTFS-Realtime vehicle positions (adds CORS + no-store).
- `wrangler.jsonc` — Worker configuration used to deploy the proxy.
