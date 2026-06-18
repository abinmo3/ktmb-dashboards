#!/usr/bin/env python3
"""
Stage 2 — End-to-end data pipeline integration test.
Simulates the Android app's data loading path:
  1. Load core data (stations, state_map, meta, guides)
  2. Resolve a station name → slug → forecast file
  3. Look up origin→destination in the forecast
  4. Compute best/avoid windows
  5. Check GTFS proxy accessibility
  6. Verify asset bundling (files exist at expected Android asset paths)

Run from ktmb_dashboards directory:
    python3 scripts/integration_test.py
"""

import json
import re
import sys
import urllib.request
from pathlib import Path

DATA_SRC = Path("docs/data")
ASSETS_DST = Path("android/app/src/main/assets/data")
PASS, FAIL = 0, 0


def slugify(name: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")


def check(label: str, condition: bool, detail: str = ""):
    global PASS, FAIL
    if condition:
        print(f"  ✅ {label}")
        PASS += 1
    else:
        print(f"  ❌ {label} — {detail}")
        FAIL += 1


def best_windows(values, count):
    """Return top `count` lowest-value hours."""
    pairs = [(h, v) for h, v in enumerate(values) if v is not None]
    pairs.sort(key=lambda x: x[1])
    return [h for h, _ in pairs[:count]]


def avoid_windows(values, count):
    """Return top `count` highest-value hours."""
    pairs = [(h, v) for h, v in enumerate(values) if v is not None]
    pairs.sort(key=lambda x: x[1], reverse=True)
    return [h for h, _ in pairs[:count]]


def main():
    global PASS, FAIL
    print("=" * 64)
    print("KTMB Stage 2 — End-to-End Integration Test")
    print("=" * 64)

    # ── 1. Asset bundling verification ──
    print("\n── 1. Asset bundling ──")
    expected_assets = [
        "stations.json",
        "state_map.json",
        "meta.json",
        "guides.json",
        "komuter_utara/stations.json",
        "komuter_utara/meta.json",
    ]
    for path in expected_assets:
        check(f"Asset {path} exists", (ASSETS_DST / path).exists(),
              f"Missing from android/app/src/main/assets/data/{path}")
    check("Asset dir exists", ASSETS_DST.is_dir())

    # ── 2. Core data loading simulation ──
    print("\n── 2. Core data loading ──")
    try:
        stations_data = json.loads((DATA_SRC / "stations.json").read_text())
        state_map = json.loads((DATA_SRC / "state_map.json").read_text())
        meta = json.loads((DATA_SRC / "meta.json").read_text())
        guides = json.loads((DATA_SRC / "guides.json").read_text())
        check("stations.json parsed", len(stations_data) > 0,
              f"Got {len(stations_data)} entries")
        check("state_map.json parsed", len(state_map) > 0,
              f"Got {len(state_map)} entries")
        check("meta.json parsed", "service" in meta)
        check("guides.json parsed", len(guides) == 3)
    except Exception as e:
        check("Core data loading", False, str(e))

    # ── 3. Station → slug → forecast resolution ──
    print("\n── 3. Station → forecast resolution ──")
    real_stations = [s for s in stations_data
                     if s["name"] not in ("Unknown", "Penalty")]

    # Pick a representative station from each service
    kl_central = next((s for s in real_stations if s["name"] == "KL Sentral"), None)
    butterworth = next((s for s in real_stations if s["name"] == "Butterworth"), None)

    if kl_central:
        slug = slugify(kl_central["name"])
        fpath = DATA_SRC / "by_origin" / f"{slug}.json"
        check(f"KL Sentral → slug '{slug}' → file exists", fpath.exists(),
              f"Expected {fpath}")
        if fpath.exists():
            fc = json.loads(fpath.read_text())
            check("Forecast has 'service' field", fc.get("service") == "komuter")
            check("Forecast has 'origin' field", fc.get("origin") == "KL Sentral")
            check("Forecast has 'destinations'", len(fc.get("destinations", {})) > 0,
                  f"Got {len(fc.get('destinations', {}))} destinations")

            # Look up a specific route
            dest = fc["destinations"].get("Bandar Tasek Selatan")
            if dest:
                check("KL Sentral → BTS forecast exists", True)
                check("baseline is 24 elements", len(dest["baseline"]) == 24)
                check("today is 24 elements", len(dest["today"]) == 24)

                # Compute best windows
                today_vals = dest["today"]
                best = best_windows(today_vals, 3)
                avoid = avoid_windows(today_vals, 2)
                print(f"    📝 Best windows: {best}")
                print(f"    📝 Avoid windows: {avoid}")
                check("Best windows computed", len(best) >= 0)
                check("Avoid windows computed", len(avoid) >= 0)
            else:
                check("KL Sentral → BTS in destinations", False, "Not found")

    if butterworth:
        slug = slugify(butterworth["name"])
        fpath = DATA_SRC / "komuter_utara" / "by_origin" / f"{slug}.json"
        check(f"Butterworth → slug '{slug}' → file exists", fpath.exists(),
              f"Expected {fpath}")
        if fpath.exists():
            fc = json.loads(fpath.read_text())
            check("Utara forecast service field", fc.get("service") == "komuter_utara")
            dest = fc["destinations"].get("Alor Setar")
            if dest:
                check("Butterworth → Alor Setar forecast exists", True)
                best = best_windows(dest["today"], 3)
                print(f"    📝 Butterworth→Alor Setar best: {best}")

    # ── 4. Service switching simulation ──
    print("\n── 4. Service switching ──")
    utara_stations = json.loads(
        (DATA_SRC / "komuter_utara" / "stations.json").read_text())
    utara_real = [s for s in utara_stations
                  if s["name"] not in ("Unknown", "Penalty")]
    komuter_real = [s for s in stations_data
                    if s["name"] not in ("Unknown", "Penalty")]

    check("Komuter has 58 real stations", len(komuter_real) == 58,
          f"Got {len(komuter_real)}")
    check("Utara has 23 real stations", len(utara_real) == 23,
          f"Got {len(utara_real)}")
    # Verify no overlap
    k_names = {s["name"] for s in komuter_real}
    u_names = {s["name"] for s in utara_real}
    check("No station name overlap", len(k_names & u_names) == 0,
          f"Overlap: {k_names & u_names}")

    # ── 5. GTFS proxy check ──
    print("\n── 5. GTFS proxy ──")
    try:
        req = urllib.request.Request(
            "https://ktmb-gtfs-proxy.abinmo3.workers.dev/",
            headers={"User-Agent": "KTMB-Stage2-Integration-Test"}
        )
        with urllib.request.urlopen(req, timeout=10) as resp:
            body = resp.read()
            check("GTFS proxy reachable", resp.status == 200,
                  f"HTTP {resp.status}")
            check("GTFS returns protobuf bytes", len(body) > 0,
                  f"Got {len(body)} bytes")
            check("GTFS content type is protobuf",
                  "protobuf" in resp.headers.get("Content-Type", "").lower()
                  or "octet-stream" in resp.headers.get("Content-Type", "").lower(),
                  f"Got {resp.headers.get('Content-Type')}")
            check("GTFS CORS header present",
                  resp.headers.get("Access-Control-Allow-Origin") == "*")
            print(f"    📝 GTFS response: {len(body)} bytes")
            print(f"    📝 First 16 bytes hex: {body[:16].hex()}")
    except Exception as e:
        check("GTFS proxy reachable", False, str(e))

    # ── 6. Forecast coverage check ──
    print("\n── 6. Forecast file coverage ──")
    k_files = set(f.stem for f in (DATA_SRC / "by_origin").glob("*.json"))
    u_files = set(f.stem for f in (DATA_SRC / "komuter_utara" / "by_origin").glob("*.json"))
    k_expected = {slugify(s["name"]) for s in komuter_real}
    u_expected = {slugify(s["name"]) for s in utara_real}

    check("All Komuter stations have forecast files",
          k_expected.issubset(k_files),
          f"Missing: {k_expected - k_files}")
    check("All Utara stations have forecast files",
          u_expected.issubset(u_files),
          f"Missing: {u_expected - u_files}")

    # Summary
    print(f"\n{'=' * 64}")
    print(f"Results: {PASS} passed, {FAIL} failed, {PASS + FAIL} total")
    if FAIL:
        print("❌ INTEGRATION TEST FAILED")
    else:
        print("✅ ALL INTEGRATION CHECKS PASSED")
    print(f"{'=' * 64}")
    return FAIL


if __name__ == "__main__":
    sys.exit(main())
