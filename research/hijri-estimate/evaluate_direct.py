"""Accuracy of the estimator against every known month start (Dar al-Ifta), counting from each known month.

Usage: python3 evaluate_direct.py <hijri.json from mawaqit-data>
"""
import datetime as dt
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import estimator

state = json.load(open(sys.argv[1], encoding="utf-8"))
months = [(m["year"] * 12 + m["month"] - 1, dt.date.fromisoformat(m["start"])) for m in state["months"]]
print("known month starts:", len(months))
for ahead in (0, 1, 3, 12, 36):
    exact = total = 0
    offsets = {}
    for i in range(0, len(months) - ahead):
        idx0, start0 = months[i]
        idx, truth = months[i + ahead]
        if ahead == 0:
            est = estimator.estimated_start(estimator.lunation_of_start(start0))
        else:
            est = estimator.estimate_from_anchor(start0, idx0, idx)
        off = (est - truth).days
        offsets[off] = offsets.get(off, 0) + 1
        total += 1
        exact += off == 0
    label = "the month itself (anchor used only to number the lunation)" if ahead == 0 else "%d months after the anchor" % ahead
    print("%-58s exact %d/%d (%.1f%%) offsets %s" % (label, exact, total, 100.0 * exact / total, dict(sorted(offsets.items()))))
