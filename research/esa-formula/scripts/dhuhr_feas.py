import sys, json
sys.path.insert(0, sys.argv[1] + "/scripts")
import solar
from load_esa import load
D = load(sys.argv[1] + "/esa_year")
EQ = {"meeus": lambda jd: solar.sun_meeus(jd, 0)[1], "noaa": lambda jd: solar.sun(jd)[1]}
def best_lon(city, eq):
    # dhuhr_min = (12+tz)*60 - 4*lon - eqt(at transit UT); choose lon maximizing days with round(...)==esa
    ev = []
    for d, e in city.items():
        tz = solar.cairo_offset(d); jd0 = solar.jd_of(d)
        ut = 12 - 31.3 / 15
        E = eq(jd0 + ut / 24)
        # need round(c - 4 lon) == m  -> 4lon in (c - m - 0.5, c - m + 0.5]
        c = (12 + tz) * 60 - E; m = e[2]
        ev.append(((c - m - 0.5) / 4, +1)); ev.append(((c - m + 0.5) / 4, -1))
    ev.sort(); cur = best = 0; at = None
    for x, s in ev:
        cur += s
        if cur > best: best, at = cur, x
    return best, len(city), at
for model in EQ:
    tot = good = 0; worst = []
    for n, c in D.items():
        b, k, at = best_lon(c, EQ[model]); tot += k; good += b; worst.append((b - k, n, at))
    print(model, "dhuhr best-case exact %.3f%%" % (100 * good / tot), "cities with 100%:", sum(1 for w in worst if w[0] == 0), "worst:", sorted(worst)[:3])
