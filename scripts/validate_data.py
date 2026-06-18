#!/usr/bin/env python3
"""
Stage 0 data validation script.
Run from the ktmb_dashboards directory:
    python3 scripts/validate_data.py

Checks:
  1. Station coverage in state_map.json
  2. Forecast file coverage for all stations
  3. Forecast file structural validity
  4. Service overlap
  5. Directionality check
  6. Today data coverage
"""

import json
import re
import sys
from pathlib import Path

BASE = Path("docs/data")
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


def main():
    global PASS, FAIL
    print("=" * 60)
    print("KTMB Data Feasibility — Validation Script")
    print("=" * 60)

    # Load data
    ks = json.loads((BASE / "stations.json").read_text(encoding="utf-8"))
    us = json.loads((BASE / "komuter_utara" / "stations.json").read_text(encoding="utf-8"))
    sm = json.loads((BASE / "state_map.json").read_text(encoding="utf-8"))
    km = json.loads((BASE / "meta.json").read_text(encoding="utf-8"))
    um = json.loads((BASE / "komuter_utara" / "meta.json").read_text(encoding="utf-8"))

    k_names = {s["name"] for s in ks} - {"Unknown", "Penalty"}
    u_names = {s["name"] for s in us} - {"Unknown", "Penalty"}

    print(f"\nKomuter: {len(k_names)} real stations, Utara: {len(u_names)} real stations\n")

    # 1. state_map coverage
    check("Komuter stations in state_map",
          k_names.issubset(set(sm.keys())),
          f"missing: {k_names - set(sm.keys())}")
    check("Utara stations in state_map",
          u_names.issubset(set(sm.keys())),
          f"missing: {u_names - set(sm.keys())}")

    # 2. Forecast file coverage
    k_files = {f.stem for f in (BASE / "by_origin").glob("*.json")}
    u_files = {f.stem for f in (BASE / "komuter_utara" / "by_origin").glob("*.json")}
    k_expected = {slugify(n) for n in k_names}
    u_expected = {slugify(n) for n in u_names}

    check("Komuter forecast files cover all stations",
          k_expected.issubset(k_files),
          f"missing: {k_expected - k_files}")
    check("Utara forecast files cover all stations",
          u_expected.issubset(u_files),
          f"missing: {u_expected - u_files}")
    check("No orphan Komuter forecast files",
          (k_files - k_expected - {"unknown"}) == set(),
          f"orphans: {k_files - k_expected - {'unknown'}}")
    check("No orphan Utara forecast files",
          (u_files - u_expected - {"unknown"}) == set(),
          f"orphans: {u_files - u_expected - {'unknown'}}")

    # 3. Forecast structure
    def validate(path, label):
        data = json.loads(path.read_text(encoding="utf-8"))
        issues = []
        for key in ["service", "origin", "latest_date", "destinations"]:
            if key not in data:
                issues.append(f"missing '{key}'")
        if "destinations" in data:
            for dn, dd in data["destinations"].items():
                for key in ["baseline", "baseline_730", "today"]:
                    arr = dd.get(key)
                    if arr is None:
                        issues.append(f"{dn}: missing '{key}'")
                    elif not isinstance(arr, list) or len(arr) != 24:
                        issues.append(f"{dn}: '{key}' not 24-list")
        check(f"{label} structure valid", not issues, "; ".join(issues))

    validate(BASE / "by_origin" / "kl-sentral.json", "kl-sentral.json")
    validate(BASE / "komuter_utara" / "by_origin" / "butterworth.json", "butterworth.json")

    # 4. Service overlap
    check("Zero station overlap between services",
          (k_names & u_names) == set(),
          f"overlap: {k_names & u_names}")

    # 5. Directionality
    kkl = json.loads((BASE / "by_origin" / "kl-sentral.json").read_text(encoding="utf-8"))
    kbts = json.loads((BASE / "by_origin" / "bandar-tasek-selatan.json").read_text(encoding="utf-8"))
    a = kkl["destinations"]["Bandar Tasek Selatan"]["baseline"]
    b = kbts["destinations"]["KL Sentral"]["baseline"]
    is_symmetric = a == b
    direction = "SYMMETRIC (direction-agnostic)" if is_symmetric else "DIRECTIONAL"
    print(f"  📝 Directionality: {direction}")

    # 6. Today data coverage
    tc = sum(1 for d in kkl["destinations"].values()
             if any(v is not None for v in d.get("today", [])))
    tn = len(kkl["destinations"]) - tc
    print(f"  📝 KL Sentral: {tc}/{tc+tn} destinations have today data")

    # 7. Meta freshness
    print(f"  📝 Komuter: latest={km['latest_date']}, days={km['days_available']}")
    print(f"  📝 Utara: latest={um['latest_date']}, days={um['days_available']}")
    print(f"  📝 Source: {km['source']}")

    # Summary
    print(f"\n{'=' * 60}")
    print(f"Results: {PASS} passed, {FAIL} failed")
    if FAIL:
        print("❌ DATA VALIDATION FAILED — fix issues above")
    else:
        print("✅ ALL CHECKS PASSED — Stage 1 can proceed")
    print(f"{'=' * 60}")
    return FAIL


if __name__ == "__main__":
    sys.exit(main())
