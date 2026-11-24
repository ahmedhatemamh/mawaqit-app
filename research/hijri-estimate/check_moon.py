"""Check moon.py against new-moon (conjunction) times known from published almanacs/eclipse tables."""
import datetime as dt
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import moon

# UTC times of new moon, from widely published tables (minute precision).
KNOWN = [
    dt.datetime(2023, 10, 14, 17, 55), dt.datetime(2024, 4, 8, 18, 21), dt.datetime(2025, 3, 29, 10, 58),
    dt.datetime(2025, 9, 21, 19, 54), dt.datetime(2026, 2, 17, 12, 1),
]
EPOCH = dt.datetime(2000, 1, 1, 12)


def jd_of(t):
    return 2451545.0 + (t - EPOCH).total_seconds() / 86400.0


def conjunction_near(jd):
    for _ in range(30):
        ml, _b, _d = moon.moon(jd)
        sl, _r = moon.sun_longitude(jd)
        diff = (ml - sl + 180) % 360 - 180
        jd -= diff / 12.19  # mean relative motion, degrees per day
    return jd


if __name__ == "__main__":
    worst = 0
    for t in KNOWN:
        jd = conjunction_near(jd_of(t))
        got = EPOCH + dt.timedelta(days=jd - 2451545.0)
        err = (got - t).total_seconds() / 60
        worst = max(worst, abs(err))
        print(t, "->", got.strftime("%Y-%m-%d %H:%M"), "error %+.1f min" % err)
    print("worst error %.1f minutes" % worst)
