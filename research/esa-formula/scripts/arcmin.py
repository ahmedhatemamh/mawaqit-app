import sys, math, itertools, multiprocessing as mp
sys.path.insert(0, sys.argv[1] + "/scripts")
import solar
solar.sun = lambda jd: solar.sun_meeus(jd, 0)
from load_esa import load
from fit import ROUND
NAME = sys.argv[2]; LAT0, LON0 = float(sys.argv[3]), float(sys.argv[4])
city = load(sys.argv[1] + "/esa_year")[NAME]
days = sorted(city)
def run(args):
    mode, dlat, dlon, rise = args
    lat = (round(LAT0 * 60) + dlat) / 60; lon = (round(LON0 * 60) + dlon) / 60
    miss = 0
    for d in days:
        t = solar.times(d, lat, lon, rise=rise, asr_noon=True)
        miss += sum(ROUND[mode](x) != e for x, e in zip(t, city[d]))
        if miss > 60:
            return None   # early stop: partial count, never ranked
    return miss, mode, lat, lon, rise
if __name__ == "__main__":
    shifts = {"nearest": 0, "floor": 7.5, "ceil": -7.5}   # arcmin shift of lon for 0.5 min
    jobs = []
    for mode, sh in shifts.items():
        for dlat in range(-4, 5):
            for dlon in range(-4, 5):
                for rise in (0.833, 0.845, 0.85):
                    jobs.append((mode, dlat, dlon + round(-sh), rise))
    with mp.Pool(4) as p:
        res = sorted(r for r in p.map(run, jobs) if r is not None)
    for r in res[:6]:
        print("miss=%d mode=%s lat=%.4f (%d°%02d') lon=%.4f (%d°%02d') rise=%.3f" % (r[0], r[1], r[2], int(r[2]), round(r[2] % 1 * 60), r[3], int(r[3]), round(r[3] % 1 * 60), r[4]))
