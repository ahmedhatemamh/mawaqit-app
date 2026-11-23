import sys, math
sys.path.insert(0, sys.argv[1] + "/scripts")
from solar import times
from load_esa import load

ROUND = {"nearest": lambda x: math.floor(x + 0.5), "floor": math.floor, "ceil": math.ceil}
SHIFT = {"nearest": 0.0, "floor": 0.5, "ceil": -0.5}

def resid(city, lat, lon, mode, **kw):
    out = []
    for d, e in city.items():
        t = times(d, lat, lon, **kw)
        out.append([x - (v + SHIFT[mode]) for x, v in zip(t, e)])
    return out

def fit(city, lat, lon, mode, **kw):
    """Gauss-Newton on lat/lon, least squares over all prayers."""
    for _ in range(6):
        r0 = sum(resid(city, lat, lon, mode, **kw), [])
        h = 1e-4
        rl = sum(resid(city, lat + h, lon, mode, **kw), [])
        rn = sum(resid(city, lat, lon + h, mode, **kw), [])
        J = [((a - b) / h, (c - b) / h) for a, c, b in zip(rl, rn, r0)]
        A11 = sum(j[0] ** 2 for j in J); A12 = sum(j[0] * j[1] for j in J); A22 = sum(j[1] ** 2 for j in J)
        b1 = -sum(j[0] * r for j, r in zip(J, r0)); b2 = -sum(j[1] * r for j, r in zip(J, r0))
        det = A11 * A22 - A12 ** 2
        dl, dn = (b1 * A22 - b2 * A12) / det, (A11 * b2 - A12 * b1) / det
        lat, lon = lat + dl, lon + dn
        if abs(dl) + abs(dn) < 1e-7:
            break
    return lat, lon

def score(city, lat, lon, mode, **kw):
    hit = [0] * 6; n = 0; worst = [0] * 6
    for d, e in city.items():
        t = times(d, lat, lon, **kw); n += 1
        for k in range(6):
            diff = ROUND[mode](t[k]) - e[k]
            hit[k] += diff == 0; worst[k] = max(worst[k], abs(diff))
    return n, hit, worst

if __name__ == "__main__":
    D = load(sys.argv[1] + "/esa_year")
    name = sys.argv[2] if len(sys.argv) > 2 else "القاهرة"
    c = D[name]
    for mode in ROUND:
        lat, lon = fit(c, 30.04, 31.24, mode)
        n, hit, worst = score(c, lat, lon, mode)
        print(f"{mode:8s} lat={lat:.4f} lon={lon:.4f} days={n} exact={hit} worst={worst}")
