import os, json, re
from datetime import datetime, timedelta
import pandas as pd

RIDERSHIP_URL = "https://storage.data.gov.my/transportation/ktmb/komuter_2025.parquet"
# (Optional) GTFS Static ZIP endpoint:
# https://api.data.gov.my/gtfs-static/ktmb  (ZIP)  :contentReference[oaicite:5]{index=5}

OUT_DIR = "docs/data"
BY_ORIGIN_DIR = os.path.join(OUT_DIR, "by_origin")

def slugify(s: str) -> str:
    s = s.lower().strip()
    s = re.sub(r"[^a-z0-9]+", "-", s)
    return re.sub(r"(^-|-$)", "", s)

def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    os.makedirs(BY_ORIGIN_DIR, exist_ok=True)

    # Load parquet (recommended by dataset page) :contentReference[oaicite:6]{index=6}
    df = pd.read_parquet(RIDERSHIP_URL)

    # Expect columns: date, time, origin, destination, ridership :contentReference[oaicite:7]{index=7}
    df["date"] = pd.to_datetime(df["date"])
    df["hour"] = df["time"].str.slice(0,2).astype(int)

    latest_date = df["date"].max().date()

    # Keep last 8 weeks (adjust if you want faster builds)
    start_date = pd.Timestamp(latest_date) - pd.Timedelta(days=56)
    df_recent = df[df["date"] >= start_date].copy()

    # "today" = latest_date
    df_today = df_recent[df_recent["date"].dt.date == latest_date]
    # baseline = mean of same weekday (excluding latest day)
    weekday = pd.Timestamp(latest_date).weekday()
    df_base = df_recent[
        (df_recent["date"].dt.weekday == weekday) &
        (df_recent["date"].dt.date != latest_date)
    ]

    # Aggregate
    today_agg = (df_today.groupby(["origin","destination","hour"])["ridership"]
                 .sum().reset_index())
    base_agg = (df_base.groupby(["origin","destination","hour"])["ridership"]
                .mean().round(2).reset_index())

    # stations list (from ridership station names)
    stations = sorted(set(df["origin"].unique()) | set(df["destination"].unique()))
    with open(os.path.join(OUT_DIR, "stations.json"), "w", encoding="utf-8") as f:
        json.dump([{"name": s} for s in stations], f, ensure_ascii=False)

    # Build per-origin JSON files (lazy-loaded by the web card)
    # Format:
    # { origin, latest_date, destinations: { dest: {today:[24], baseline:[24]} } }
    # Make baseline dict for quick lookup
    base_key = {}
    for r in base_agg.itertuples(index=False):
        base_key[(r.origin, r.destination, int(r.hour))] = float(r.ridership)

    # Group today's data by origin/destination
    for origin, g0 in today_agg.groupby("origin"):
        out = {
            "origin": origin,
            "latest_date": str(latest_date),
            "destinations": {}
        }
        # for each destination under this origin
        for dest, g1 in g0.groupby("destination"):
            today_arr = [None]*24
            base_arr = [None]*24

            for r in g1.itertuples(index=False):
                today_arr[int(r.hour)] = int(r.ridership)

            # baseline fill
            for h in range(24):
                v = base_key.get((origin, dest, h))
                base_arr[h] = v if v is not None else None

            out["destinations"][dest] = {"today": today_arr, "baseline": base_arr}

        fn = os.path.join(BY_ORIGIN_DIR, f"{slugify(origin)}.json")
        with open(fn, "w", encoding="utf-8") as f:
            json.dump(out, f, ensure_ascii=False)

    # meta
    with open(os.path.join(OUT_DIR, "meta.json"), "w", encoding="utf-8") as f:
        json.dump({
            "latest_date": str(latest_date),
            "generated_at": datetime.utcnow().strftime("%Y-%m-%d %H:%M UTC"),
            "source": RIDERSHIP_URL
        }, f, ensure_ascii=False)

if __name__ == "__main__":
    main()

