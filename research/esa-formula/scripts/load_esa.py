import json, glob, re, datetime as dt
def norm(s):
    return re.sub(r"\s+", " ", s.replace("ـ", "")).strip()
def mins(s):
    m = re.match(r"(\d+):(\d+) (ص|م)", s.strip())
    h = int(m.group(1)) % 12 + (12 if m.group(3) == "م" else 0)
    return h * 60 + int(m.group(2))
def load(folder):
    """{city: {date: [6 minutes]}}"""
    data = {}
    for f in sorted(glob.glob(folder + "/2026-*.json")):
        for r in json.load(open(f, encoding="utf-8")):
            d = dt.date.fromisoformat(r[1])
            v = [mins(x) for x in r[3:9]]
            data.setdefault(norm(r[0]), {})[d] = v
    if not data:
        raise SystemExit("input error: no ESA day files (2026-*.json) found in %s" % folder)
    return data
