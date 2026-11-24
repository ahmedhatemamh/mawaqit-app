"""Score crescent-visibility criteria against the official month lengths (Dar al-Ifta statements).

Usage: python3 evaluate.py <hijri.json from mawaqit-data>
Label: a month of 29 days means the crescent was accepted on the evening of day 29.
"""
import datetime as dt
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import crescent


def load(path):
    state = json.load(open(path, encoding="utf-8"))
    rows = []
    for m in state["months"]:
        if m["length"] not in (29, 30):
            continue
        start = dt.date.fromisoformat(m["start"])
        evening = start + dt.timedelta(days=28)
        f = crescent.features(evening)
        f.update(year=m["year"], month=m["month"], evening=evening.isoformat(), seen=(m["length"] == 29))
        rows.append(f)
    return rows


def accuracy(rows, predict):
    ok = sum(1 for r in rows if predict(r) == r["seen"])
    return ok, len(rows)


def sweep(rows, key, values, op=">="):
    best = None
    for v in values:
        pred = (lambda r, v=v: r[key] >= v)
        ok, n = accuracy(rows, pred)
        if best is None or ok > best[1]:
            best = (v, ok, n)
    return best


if __name__ == "__main__":
    rows = load(sys.argv[1])
    n = len(rows)
    seen = sum(r["seen"] for r in rows)
    print("months with a known length:", n, "| 29 days:", seen, "| 30 days:", n - seen)
    print("baseline always 30: %d/%d" % (n - seen, n))
    print("baseline always 29: %d/%d" % (seen, n))
    ok, _ = accuracy(rows, lambda r: r["age_h"] > 0)
    print("conjunction before sunset: %d/%d" % (ok, n))
    ok, _ = accuracy(rows, lambda r: r["age_h"] > 0 and r["lag_min"] > 0)
    print("conjunction before sunset and moon sets after sun: %d/%d" % (ok, n))
    for key, values in [("age_h", [x / 2 for x in range(-4, 60)]), ("lag_min", list(range(-10, 60))),
                        ("moon_alt", [x / 4 for x in range(-8, 40)]), ("elong", [x / 2 for x in range(0, 40)]),
                        ("arcv", [x / 4 for x in range(-8, 60)]), ("q", [x / 20 for x in range(-30, 10)]),
                        ("v", [x / 4 for x in range(-20, 20)])]:
        v, ok, n2 = sweep(rows, key, values)
        half = n2 // 2
        a, _ = accuracy(rows[:half], lambda r, v=v, k=key: r[k] >= v)
        b, _ = accuracy(rows[half:], lambda r, v=v, k=key: r[k] >= v)
        print("best threshold %-8s >= %-6s : %d/%d  (first half %d/%d, second half %d/%d)" % (key, v, ok, n2, a, half, b, n2 - half))
    wrong = [r for r in rows if (r["age_h"] > 0 and r["lag_min"] > 0) != r["seen"]]
    print("months where 'conjunction before sunset and moon sets after sun' disagrees:", len(wrong))
    for r in wrong[:12]:
        print("  %d-%02d evening %s seen=%s age=%.1fh lag=%.0fmin alt=%.1f elong=%.1f" %
              (r["year"], r["month"], r["evening"], r["seen"], r["age_h"], r["lag_min"], r["moon_alt"], r["elong"]))
