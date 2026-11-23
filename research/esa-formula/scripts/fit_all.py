import sys, json, math, multiprocessing as mp
sys.path.insert(0, sys.argv[1] + "/scripts")
import solar
solar.sun = lambda jd: solar.sun_meeus(jd, 0)
from fit import fit, score
from load_esa import load

RISE = float(sys.argv[2]) if len(sys.argv) > 2 else 0.845
KW = dict(rise=RISE, asr_noon=True)

def initial(city):
    lons = []
    for d, e in city.items():
        _, eqt = solar.sun(solar.jd_of(d) + 0.4)
        lons.append(15 * (12 + solar.cairo_offset(d) - eqt / 60 - e[2] / 60))
    lon = sum(lons) / len(lons)
    best = None
    for lat10 in range(210, 330, 5):
        lat = lat10 / 10
        err = 0
        for d, e in list(city.items())[::15]:
            t = solar.times(d, lat, lon, **KW)
            err += (t[1] - e[1]) ** 2 + (t[4] - e[4]) ** 2
        if best is None or err < best[0]:
            best = (err, lat)
    return best[1], lon

def work(item):
    name, city = item
    lat, lon = initial(city)
    lat, lon = fit(city, lat, lon, "nearest", **KW)
    n, hit, worst = score(city, lat, lon, "nearest", **KW)
    return name, round(lat, 4), round(lon, 4), n, hit, worst

if __name__ == "__main__":
    D = load(sys.argv[1] + "/esa_year")
    with mp.Pool(4) as p:
        res = p.map(work, sorted(D.items()))
    json.dump(res, open(sys.argv[1] + "/fit_rise_%s.json" % RISE, "w"), ensure_ascii=False)
    tot_n = sum(r[3] for r in res) * 6
    tot_hit = sum(sum(r[4]) for r in res)
    per = [sum(r[4][k] for r in res) for k in range(6)]
    n_days = sum(r[3] for r in res)
    print("rise", RISE, "overall exact %.3f%%" % (100 * tot_hit / tot_n), "misses", tot_n - tot_hit)
    print("per prayer exact % [fajr,sunrise,dhuhr,asr,maghrib,isha]:", [round(100 * x / n_days, 2) for x in per])
    print("worst diff any city:", max(max(r[5]) for r in res))
    for r in sorted(res, key=lambda r: sum(r[4]))[:8]:
        print("lowest:", r)
