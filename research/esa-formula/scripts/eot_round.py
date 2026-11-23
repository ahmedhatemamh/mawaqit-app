import sys, json, math, numpy as np
sys.path.insert(0, sys.argv[1] + "/scripts")
exec(open(sys.argv[1] + "/scripts/eot_percity.py").read().split("global A, B")[0])
A0, B0 = A.copy(), B.copy()
V69 = lambda jd: vsop.sun_vsop(jd, 69)[1]
MS = lambda jd: solar.sun_meeus(jd, 0)[1]
def rnd(f, step_s, mode):
    g = {"round": lambda x: math.floor(x + 0.5), "floor": math.floor, "trunc": math.trunc}[mode]
    return lambda jd: g(f(jd) * 60 / step_s) * step_s / 60
tests = [("VSOP E rounded 1s", rnd(V69, 1, "round")), ("VSOP E trunc 1s", rnd(V69, 1, "trunc")), ("VSOP E floor 1s", rnd(V69, 1, "floor")),
         ("Meeus E rounded 1s", rnd(MS, 1, "round")), ("Meeus E trunc 1s", rnd(MS, 1, "trunc")),
         ("VSOP E rounded 0.1min", rnd(V69, 6, "round"))]
for name, f in tests:
    for where in ("transit", "noon-Cairo", "0h UT"):
        if where == "transit":
            E = build(f)
        else:
            off = (12 - 31.3 / 15) / 24 if where == "noon-Cairo" else 0.0
            E = np.array([f(solar.jd_of(d) + off) for c in cities for d in D[c]])
        A = A0 - E; B = B0 - E
        globals()["A"], globals()["B"] = A, B
        print("%-22s %-11s closed-interval feasible: %-5s  max dev needed %.3f s" % (name, where, feas(np.zeros(nd), 0.0), mindelta(np.zeros(nd)) * 60), flush=True)
