import sys, json
sys.path.insert(0, sys.argv[1] + "/scripts")
import solar, vsop
from load_esa import load
D = load(sys.argv[1] + "/esa_year")
fitres = {r[0]: r[2] for r in json.load(open(sys.argv[1] + "/fit_rise_0.845.json"))}
def best_lon(city, lon0, dT, ut_mode):
    ev = []
    for d, e in city.items():
        tz = solar.cairo_offset(d); jd0 = solar.jd_of(d)
        if ut_mode == "transit":
            ut = 12 - lon0 / 15
            for _ in range(2):
                ut = 12 - lon0 / 15 - vsop.sun_vsop(jd0 + ut / 24, dT)[1] / 60
        else:
            ut = ut_mode
        E = vsop.sun_vsop(jd0 + ut / 24, dT)[1]
        c = (12 + tz) * 60 - E; m = e[2]
        ev.append(((c - m - 0.5) / 4, 1)); ev.append(((c - m + 0.5) / 4, -1))
    ev.sort(); cur = best = 0; lo = hi = None
    for i, (x, s) in enumerate(ev):
        cur += s
        if cur > best: best, lo, hi = cur, x, ev[i + 1][0]
    return best, len(city), (lo, hi)
for dT in (69, 0):
    tot = good = full = 0; widths = []
    for n, c in D.items():
        b, k, (lo, hi) = best_lon(c, fitres[n], dT, "transit"); tot += k; good += b; full += b == k
        if b == k: widths.append((hi - lo) * 3600)
    print("dT=%d per-city transit: dhuhr exact %.3f%%, cities 100%%: %d/78, lon window width (arcsec) median %.1f" % (dT, 100 * good / tot, full, sorted(widths)[len(widths) // 2] if widths else -1))
