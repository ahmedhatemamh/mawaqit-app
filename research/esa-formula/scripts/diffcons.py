"""Is there ANY per-day EoT e_d and per-city lon u_c with C_d - u_c - e_d inside the closed interval
[ESA dhuhr - 0.5, ESA dhuhr + 0.5] for all (c,d)? This is a closed-interval relaxation: a value on a shared
endpoint (for example 719.5) counts as matching both 719 and 720, which no real rounding rule does.
Finds the smallest widening delta (minutes) of the interval that makes the system feasible."""
import sys, numpy as np
sys.path.insert(0, sys.argv[1] + "/scripts")
import solar
from load_esa import load
D = load(sys.argv[1] + "/esa_year")
cities = sorted(D); dates = sorted(D["القاهرة"])
ci = {c: i for i, c in enumerate(cities)}; di = {d: len(cities) + i for i, d in enumerate(dates)}
N = len(cities) + len(dates)
A = []; Bv = []; U = []; V = []
for c in cities:
    for d, e in D[c].items():
        C = (12 + solar.cairo_offset(d)) * 60
        lo, hi = C - e[2] - 0.5, C - e[2] + 0.5    # u_c - y_d in [lo, hi], y_d = -e_d
        U.append(ci[c]); V.append(di[d]); A.append(lo); Bv.append(hi)
U = np.array(U); V = np.array(V); A = np.array(A); Bv = np.array(Bv)
def feasible(delta):
    # edges: y_d -> u_c weight hi+delta ; u_c -> y_d weight -(lo-delta)
    src = np.concatenate([V, U]); dst = np.concatenate([U, V]); w = np.concatenate([Bv + delta, -(A - delta)])
    dist = np.zeros(N)
    for it in range(N + 1):
        cand = dist[src] + w
        new = dist.copy(); np.minimum.at(new, dst, cand)
        if np.allclose(new, dist, rtol=0, atol=1e-12):
            return True, dist
        dist = new
    return False, dist
lo, hi = 0.0, 1.0
ok0, _ = feasible(0.0)
print("feasible as a closed-interval relaxation (not proof of exact rounding):", ok0)
if not ok0:
    if not feasible(hi)[0]:
        raise SystemExit("input error: infeasible even with a widening of %.1f min" % hi)
    for _ in range(14):
        mid = (lo + hi) / 2
        if feasible(mid)[0]: hi = mid
        else: lo = mid
    print("minimal widening needed: %.4f min = %.2f s" % (hi, hi * 60))
ok, dist = feasible(hi if not ok0 else 0.0)
u = dist[:len(cities)]; y = dist[len(cities):]
e = -y
np.save(sys.argv[1] + "/eot_esa.npy", e)
import vsop
ref = np.array([vsop.sun_vsop(solar.jd_of(d) + (12 - 31.3 / 15) / 24, 69)[1] for d in dates])
diff = (e - ref); diff -= np.median(diff)
print("ESA-implied EoT minus VSOP EoT (seconds), every 20 days:")
for k in range(0, len(dates), 20):
    print(dates[k], "%+.2f s" % (diff[k] * 60))
