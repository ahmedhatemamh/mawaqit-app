import sys, json, collections
sys.path.insert(0, sys.argv[1] + "/scripts")
import solar, vsop
from load_esa import load
D = load(sys.argv[1] + "/esa_year")
lonfit = {r[0]: r[2] for r in json.load(open(sys.argv[1] + "/fit_rise_0.845.json"))}
rows = []
for n, c in D.items():
    pre = []
    for d, e in c.items():
        jd0 = solar.jd_of(d); lon = lonfit[n]; ut = 12 - lon / 15
        for _ in range(2): ut = 12 - lon / 15 - vsop.sun_vsop(jd0 + ut / 24, 69)[1] / 60
        base = (12 + solar.cairo_offset(d)) * 60 - vsop.sun_vsop(jd0 + ut / 24, 69)[1]   # dhuhr = base - 4lon
        pre.append((d, base, e[2]))
    # best lon: maximize count of round(base-4lon)==m ; sweep
    ev = []
    for d, b, m in pre:
        ev.append(((b - m - 0.5) / 4, 1)); ev.append(((b - m + 0.5) / 4, -1))
    ev.sort(); cur = best = 0; at = None
    for i, (x, s) in enumerate(ev):
        cur += s
        if cur > best: best, at = cur, (x + ev[i + 1][0]) / 2
    for d, b, m in pre:
        t = b - 4 * at
        if round(t) != m:
            rows.append((n, d, t - m))
print("dhuhr misses:", len(rows))
dist = [abs(abs(r[2]) - 0.5) * 60 for r in rows]
if dist:
    print("distance from rounding boundary (s): max %.2f, median %.2f" % (max(dist), sorted(dist)[len(dist)//2]))
else:
    print("no mismatches")
print("misses by month:", sorted(collections.Counter(r[1].month for r in rows).items()))
print("dates with >=3 cities missing:", [(d.isoformat(), k) for d, k in collections.Counter(r[1] for r in rows).most_common(8)])
