# KTMB Dashboards (GitHub Pages)

## Enable GitHub Pages for `/docs`
1. In the GitHub repo, go to **Settings → Pages**.
2. Under **Build and deployment**, select **Deploy from a branch**.
3. Choose the branch (e.g., `main`) and set the folder to **/docs**.
4. Save. GitHub will publish the site from the `/docs` folder.

## How the data updates
- The workflow `.github/workflows/build-data.yml` runs daily and on manual trigger.
- It executes:
  - `scripts/build_data.py` → regenerates `docs/data/*` from the parquet sources.
  - `scripts/build_gtfs_routes.py` → regenerates `docs/data/gtfs_routes.json` and `docs/data/bbox.json` from GTFS static data.
- The workflow commits and pushes updated files in `docs/data/`.

## Local build (manual)
To run the data build locally, install Python dependencies first:
1. `python -m pip install -r requirements.txt`
2. `python scripts/build_data.py`
3. `python scripts/build_gtfs_routes.py`

## Embedding in Notion
1. Ensure GitHub Pages is enabled for `/docs` (see above).
2. In Notion, add an **Embed** block.
3. Paste the GitHub Pages URL for the page you want to embed, for example:
   - `https://<org>.github.io/<repo>/embed.html`
   - `https://<org>.github.io/<repo>/reco.html`

### Optional embed query params
`embed.html` supports a few URL parameters to preselect values:
- `origin` — station name (matches the dropdown label).
- `destination` — station name (matches the dropdown label).
- `window` — free-text label shown under the meta line (e.g., `07:00–08:00`).
- `embed=1` — removes padding/border for tighter iframe fits.

## Recommended embed page
- **Recommended:** `embed.html`
  - Purpose-built for Notion: minimal layout, no extra navigation, and supports query params for preselecting origin/destination.
- `reco.html` remains available for a fuller view with more context.
