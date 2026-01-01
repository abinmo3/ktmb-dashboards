from pathlib import Path
from datetime import datetime, timezone
import json
import pandas as pd

SERVICES = {
    # keep existing Komuter output at docs/data (so your current URLs still work)
    "komuter": {
        "url": "https://storage.data.gov.my/transportation/ktmb/komuter_2025.parquet",
        "out_dir": Path("docs/data"),
    },
    # new service goes into a subfolder
    "komuter_utara": {
        "url": "https://storage.data.gov.my/transportation/ktmb/komuter_utara_2025.parquet",
        "out_dir": Path("docs/data/komuter_utara"),
    },
}

def slugify(s: str) -> str:
    import re
    s = s.lower()
    s = re.sub(r"[^a-z0-9]+", "-", s)
    s = re.sub(r"(^-|-$)", "", s)
    return s

def series_to_24(series_by_hour):
    # series index = hour (0..23)
    arr = [None] * 24
    for h in range(24):
        v = series_by_hour.get(h)
        arr[h] = float(v) if v is not None else None
    return arr

def build_service(service_key: str, url: str, out_dir: Path):
    out_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / "by_origin").mkdir(parents=True, exist_ok=True)

    # Expected schema: date, time (HH:MM), origin, destination, ridership
    df = pd.read_parquet(url, columns=["date", "time", "origin", "destination", "ridership"])
    df["date"] = pd.to_datetime(df["date"])
    df["hour"] = df["time"].astype(str).str.slice(0, 2).astype(int)

    latest_dt = df["date"].max().normalize()
    latest_date = latest_dt.date().isoformat()



    # optional visibility: how much history exists in the parquet
    earliest_dt = df["date"].min().normalize()
    earliest_date = earliest_dt.date().isoformat()
    days_available = int((latest_dt - earliest_dt).days) + 1

    today_df = df[df["date"] == latest_dt]
    base_df = df[df["date"] < latest_dt]
    if base_df.empty:
        base_df = df  # fallback

    # 730-day baseline (≈ 24 months) excluding latest day
    cutoff_730 = latest_dt - pd.Timedelta(days=730)
    base_730_df = df[(df["date"] < latest_dt) & (df["date"] >= cutoff_730)]
    if base_730_df.empty:
        base_730_df = base_df  # fallback to whatever history exists

    # Aggregate
    today_g = today_df.groupby(["origin", "destination", "hour"])["ridership"].sum()
    base_g = base_df.groupby(["origin", "destination", "hour"])["ridership"].mean()
    base_730_g = base_730_df.groupby(["origin", "destination", "hour"])["ridership"].mean()

    # Stations list
    stations = sorted(set(df["origin"].dropna().unique()).union(set(df["destination"].dropna().unique())))
    with open(out_dir / "stations.json", "w", encoding="utf-8") as f:
        json.dump([{"name": s} for s in stations], f, ensure_ascii=False)

    # Meta
        meta = {
        "service": service_key,
        "latest_date": latest_date,
        "earliest_date": earliest_date,
        "days_available": days_available,
        "generated_at": datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC"),
        "source": url,
    }

    with open(out_dir / "meta.json", "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False)

    # Per-origin files
    origins = sorted(set(df["origin"].dropna().unique()))
    for origin in origins:
        origin_slug = slugify(origin)

               # slice groups for this origin (IMPORTANT: spaces only, no tabs)
        try:
            base_o = base_g.xs(origin, level=0)      # index: (destination, hour)
        except KeyError:
            base_o = None

        try:
            base_730_o = base_730_g.xs(origin, level=0)  # index: (destination, hour)
        except KeyError:
            base_730_o = None

        try:
            today_o = today_g.xs(origin, level=0)    # index: (destination, hour)
        except KeyError:
            today_o = None


        dests = set()
        if base_o is not None:
            dests |= set(base_o.index.get_level_values(0).unique())
        if today_o is not None:
            dests |= set(today_o.index.get_level_values(0).unique())

        destinations = {}
        for dest in sorted(dests):
            # baseline
            baseline_24 = [None] * 24
            if base_o is not None:
                try:
                    b = base_o.xs(dest, level=0)  # index: hour
                    baseline_24 = series_to_24(b)
                except KeyError:
                    pass
                    # baseline_730 (last ~24 months)
baseline_730_24 = [None] * 24
if base_730_o is not None:
    try:
        b730 = base_730_o.xs(dest, level=0)  # index: hour
        baseline_730_24 = series_to_24(b730)
    except KeyError:
        pass


            # today
            today_24 = [None] * 24
            if today_o is not None:
                try:
                    t = today_o.xs(dest, level=0)  # index: hour
                    today_24 = series_to_24(t)
                except KeyError:
                    pass

           destinations[dest] = {
    "baseline": baseline_24,
    "baseline_730": baseline_730_24,
    "today": today_24
}


        payload = {
            "service": service_key,
            "origin": origin,
            "latest_date": latest_date,
            "destinations": destinations,
        }

        with open(out_dir / "by_origin" / f"{origin_slug}.json", "w", encoding="utf-8") as f:
            json.dump(payload, f, ensure_ascii=False)

def main():
    for key, spec in SERVICES.items():
        build_service(key, spec["url"], spec["out_dir"])

if __name__ == "__main__":
    main()
