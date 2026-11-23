import sys, numpy as np
sys.path.insert(0, sys.argv[1] + "/scripts")
exec(open(sys.argv[1] + "/scripts/eot_cheb.py").read().split("cands = {")[0])
ref = at_noon(lambda jd: vsop.sun_vsop(jd, 69)[1])
delta = 0.6 / 60
yd = np.arange(nc, nc + nd); z = np.full(nd, Z)
src = np.concatenate([V, U, z, yd]); dst = np.concatenate([U, V, yd, z])
w = np.concatenate([B, -A, -ref + delta, ref + delta])
def sp(s, d_, w_, start):
    dist = np.full(N, np.inf); dist[start] = 0
    for _ in range(N + 1):
        new = dist.copy(); np.minimum.at(new, d_, dist[s] + w_)
        if np.array_equal(new, dist): break
        dist = new
    return dist
f = sp(src, dst, w, Z)          # max(y_d - z) = f[y_d]
r = sp(dst, src, w, Z)          # max(z - y_d) = r[y_d]
ymax = f[nc:nc + nd]; ymin = -r[nc:nc + nd]
# e_d = -y_d ; deviation dev_d = e_d - ref_d (relative to z=0)
dmin = (-ymax - ref) * 60; dmax = (-ymin - ref) * 60
mid = (dmin + dmax) / 2
np.save(sys.argv[1] + "/eot_dev_mid.npy", mid)
print("band width median %.2f s" % np.median(dmax - dmin))
for k in range(0, nd, 10):
    print(dates[k], "dev in [%+.2f, %+.2f] s" % (dmin[k], dmax[k]))
