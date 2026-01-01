from pathlib import Path
import json, io, zipfile
from datetime import datetime, timezone

import pandas as pd
import requests

GTFS_ZIP_URL = "https://api.data.gov.my/gtfs-static/ktmb"

SERVICES = {
    # komuter stays at docs/data
    "komuter": Path("docs/data"),
    # north goes here
    "komuter_utara": Path("docs/data/komuter_utara"),
}

def load_station_names(service_dir: Path):
    p = service_dir / "stations.json"
    data = json.loads(p.read_text(encoding="utf-8"))
    return set(x["name"] for x in data)

def main():
    r = requests.get(GTFS_ZIP_URL, timeout=60)
    r.raise_for_status()
    z = zipfile.ZipFile(io.BytesIO(r.content))

    stops = pd.read_csv(z.open("stops.txt"))[["stop_id","stop_name","stop_lat","stop_lon"]].dropna()
    stop_times = pd.read_csv(z.open("stop_times.txt"))[["trip_id","stop_id","stop_sequence"]].dropna()
    trips = pd.read_csv(z.open("trips.txt"))[["trip_id","route_id"]].dropna()

    st = stop_times.merge(trips, on="trip_id", how="inner")

    for service_key, out_dir in SERVICES.items():
        out_dir.mkdir(parents=True, exist_ok=True)

        station_names = load_station_names(out_dir)
        svc_stops = stops[stops["stop_name"].isin(station_names)].copy()

        # bbox for filtering trains + fitBounds
        bbox = {
            "min_lat": float(svc_stops["stop_lat"].min()),
            "max_lat": float(svc_stops["stop_lat"].max()),
            "min_lon": float(svc_stops["stop_lon"].min()),
            "max_lon": float(svc_stops["stop_lon"].max()),
        }
        (out_dir / "bbox.json").write_text(json.dumps(bbox), encoding="utf-8")

        svc_stop_ids = set(svc_stops["stop_id"].unique())
        svc_st = st[st["stop_id"].isin(svc_stop_ids)].copy()

        # pick one representative trip per route (the trip with most stops)
        route_lines = []
        for route_id, g in svc_st.groupby("route_id"):
            trip_id = g.groupby("trip_id").size().sort_values(ascending=False).index[0]
            trip_rows = g[g["trip_id"] == trip_id].sort_values("stop_sequence")
            trip_rows = trip_rows.merge(svc_stops, on="stop_id", how="left").dropna()

            coords = [[float(a), float(b)] for a,b in zip(trip_rows["stop_lat"], trip_rows["stop_lon"])]

            # remove consecutive dupes
            cleaned = []
            for c in coords:
                if not cleaned or cleaned[-1] != c:
                    cleaned.append(c)

            if len(cleaned) >= 2:
                route_lines.append({"route_id": str(route_id), "coords": cleaned})

        (out_dir / "gtfs_routes.json").write_text(json.dumps(route_lines, ensure_ascii=False), encoding="utf-8")

if __name__ == "__main__":
    main()
