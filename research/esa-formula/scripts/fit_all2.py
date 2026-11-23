import sys, json, multiprocessing as mp
sys.path.insert(0, sys.argv[1] + "/scripts")
import solar, vsop
MODEL = sys.argv[2]; RISE = float(sys.argv[3]); DST = False; DMODE = sys.argv[4] if sys.argv[4] not in ("0","1") else None
solar.sun = (lambda jd: vsop.sun_vsop(jd, 69)) if MODEL == "vsop" else (lambda jd: solar.sun_meeus(jd, 0))
from fit import fit, score
from load_esa import load
KW = dict(rise=RISE, asr_noon=True, dst_mode=DMODE)
prev = {r[0]: (r[1], r[2]) for r in json.load(open(sys.argv[1] + "/fit_rise_0.845.json"))}
def work(item):
    name, city = item
    lat, lon = fit(city, *prev[name], "nearest", **KW)
    n, hit, worst = score(city, lat, lon, "nearest", **KW)
    return name, lat, lon, n, hit, worst
if __name__ == "__main__":
    D = load(sys.argv[1] + "/esa_year")
    with mp.Pool(4) as p:
        res = p.map(work, sorted(D.items()))
    json.dump(res, open(sys.argv[1] + "/fit2_%s_%s_%s.json" % (MODEL, RISE, int(DST)), "w"), ensure_ascii=False)
    nd = sum(r[3] for r in res)
    per = [sum(r[4][k] for r in res) for k in range(6)]
    print(MODEL, "rise", RISE, "dst_mode", DMODE, "exact %.3f%%" % (100 * sum(per) / (6 * nd)), "misses", 6 * nd - sum(per),
          "per prayer %", [round(100 * x / nd, 2) for x in per], "worst", max(max(r[5]) for r in res), flush=True)
