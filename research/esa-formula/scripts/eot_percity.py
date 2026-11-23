import sys, json, numpy as np
sys.path.insert(0, sys.argv[1] + "/scripts")
exec(open(sys.argv[1] + "/scripts/eot_cheb.py").read().split("cands = {")[0])
import vsop
lonfit = {r[0]: r[2] for r in json.load(open(sys.argv[1] + "/fit_rise_0.845.json"))}
def build(sunE):
    Es = []
    for i, c in enumerate(cities):
        lon = lonfit[c]
        for d in D[c]:
            jd0 = solar.jd_of(d); ut = 12 - lon / 15
            for _ in range(2): ut = 12 - lon / 15 - sunE(jd0 + ut / 24) / 60
            Es.append(sunE(jd0 + ut / 24))
    return np.array(Es)
global A, B
A0, B0 = A.copy(), B.copy()
for name, f in (("VSOP dT69", lambda jd: vsop.sun_vsop(jd, 69)[1]), ("Meeus short", lambda jd: solar.sun_meeus(jd, 0)[1])):
    E = build(f)
    A = A0 - E; B = B0 - E
    print("%-12s per-city transit: max per-day deviation needed %.3f s" % (name, mindelta(np.zeros(nd)) * 60), flush=True)
    print("   closed-interval feasible without any correction?", feas(np.zeros(nd), 0.0))
