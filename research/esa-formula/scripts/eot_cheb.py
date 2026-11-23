import sys, numpy as np
sys.path.insert(0, sys.argv[1] + "/scripts")
import solar, vsop
from load_esa import load
D = load(sys.argv[1] + "/esa_year")
cities = sorted(D); dates = sorted(D["القاهرة"])
nc, nd = len(cities), len(dates)
Z = nc + nd; N = Z + 1
U=[];V=[];A=[];B=[]
for i, c in enumerate(cities):
    for d, e in D[c].items():
        C = (12 + solar.cairo_offset(d)) * 60
        U.append(i); V.append(nc + dates.index(d)); A.append(C - e[2] - 0.5); B.append(C - e[2] + 0.5)
U=np.array(U);V=np.array(V);A=np.array(A);B=np.array(B)
def feas(ref, delta):
    yd = np.arange(nc, nc + nd); z = np.full(nd, Z)
    src = np.concatenate([V, U, z, yd]); dst = np.concatenate([U, V, yd, z])
    w = np.concatenate([B, -A, -ref + delta, ref + delta])     # y_d - z <= -ref+delta ; z - y_d <= ref+delta
    dist = np.zeros(N)
    for _ in range(N + 1):
        new = dist.copy(); np.minimum.at(new, dst, dist[src] + w)
        if np.allclose(new, dist, atol=1e-12): return True
        dist = new
    return False
def mindelta(ref):
    lo, hi = 0.0, 2.0
    for _ in range(16):
        mid = (lo + hi) / 2
        if feas(ref, mid): hi = mid
        else: lo = mid
    return hi
def at_noon(f):
    return np.array([f(solar.jd_of(d) + (12 - 31.3 / 15) / 24) for d in dates])
cands = {
 "VSOP87 dT=69s": at_noon(lambda jd: vsop.sun_vsop(jd, 69)[1]),
 "VSOP87 dT=0": at_noon(lambda jd: vsop.sun_vsop(jd, 0)[1]),
 "Meeus short": at_noon(lambda jd: solar.sun_meeus(jd, 0)[1]),
 "NOAA": at_noon(lambda jd: solar.sun(jd)[1]),
 "VSOP87 at 0h UT": np.array([vsop.sun_vsop(solar.jd_of(d), 69)[1] for d in dates]),
 "VSOP87 at 12h UT": np.array([vsop.sun_vsop(solar.jd_of(d) + 0.5, 69)[1] for d in dates]),
}
for k, ref in cands.items():
    print("%-18s max deviation needed: %.2f s" % (k, mindelta(ref) * 60), flush=True)
