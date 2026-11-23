import sys, numpy as np, copy
sys.path.insert(0, sys.argv[1] + "/scripts")
exec(open(sys.argv[1] + "/scripts/eot_cheb.py").read().split("cands = {")[0])
import vsop
orig = {k: list(getattr(vsop, k)) for k in ("L0", "L1", "L2", "L3", "L4", "L5", "B0", "B1", "R0")}
def variant(drop):
    for k, v in orig.items():
        setattr(vsop, k, [t for t in v if not drop(k, t)])
    ref = at_noon(lambda jd: vsop.sun_vsop(jd, 69)[1])
    for k, v in orig.items(): setattr(vsop, k, list(v))
    return ref
tests = {
  "full VSOP": lambda k, t: False,
  "no lunar 77713": lambda k, t: abs(t[2] - 77713.77) < 1,
  "only 6283 harmonics (Kepler)": lambda k, t: k.startswith("L") and t[2] != 0 and abs(t[2] / 6283.0758 - round(t[2] / 6283.0758)) > 1e-3,
  "Kepler + lunar": lambda k, t: k.startswith("L") and t[2] != 0 and abs(t[2] / 6283.0758 - round(t[2] / 6283.0758)) > 1e-3 and abs(t[2] - 77713.77) > 1,
  "drop terms < 100": lambda k, t: k == "L0" and t[0] < 100,
  "drop terms < 1000": lambda k, t: k == "L0" and t[0] < 1000,
}
for name, fn in tests.items():
    print("%-32s max deviation needed: %.2f s" % (name, mindelta(variant(fn)) * 60), flush=True)
