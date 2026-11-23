import sys, multiprocessing as mp
sys.path.insert(0, sys.argv[1] + "/scripts")
import solar
solar.sun = lambda jd: solar.sun_meeus(jd, 0)
from fit import fit, score
from load_esa import load
D = load(sys.argv[1] + "/esa_year")
names = ["القاهرة", "الأسكندرية", "أسوان", "شرق العوينات"]
def run(sh):
    tot = 0; det = []
    for n in names:
        c = D[n] if n in D else None
        if c is None: continue
        lat, lon = fit(c, 28, 31, "nearest", rise=0.845, asr_noon=True, shift_h=sh)
        k, hit, w = score(c, lat, lon, "nearest", rise=0.845, asr_noon=True, shift_h=sh)
        tot += 6 * k - sum(hit); det.append(6 * k - sum(hit))
    return tot, sh, det
if __name__ == "__main__":
    print([n for n in names if n not in D])
    with mp.Pool(4) as p:
        for r in sorted(p.map(run, [x / 2 for x in range(-12, 13)]))[:8]:
            print("misses=%d shift_h=%+.1f per-city=%s" % r)
