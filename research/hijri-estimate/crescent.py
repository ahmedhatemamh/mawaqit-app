"""Crescent features at sunset on the evening of day 29 of a Hijri month, for Cairo."""
import datetime as dt
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import moon

D2R = math.pi / 180.0
LAT, LON = 30.0444, 31.2357  # Cairo
SUN_HORIZON = -0.8333
EARTH_RADIUS_KM = 6378.14


def jd_at(date, ut_hours):
    return date.toordinal() + 1721424.5 + ut_hours / 24.0


def altitude(ra, dec, jd):
    h = moon.gmst(jd) + LON * D2R - ra
    phi = LAT * D2R
    return math.degrees(math.asin(math.sin(phi) * math.sin(dec) + math.cos(phi) * math.cos(dec) * math.cos(h)))


def sun_alt(jd):
    lon, _r = moon.sun_longitude(jd)
    dpsi, eps = moon.nutation_and_obliquity(jd)
    ra, dec = moon.to_equatorial(lon, 0.0, eps)
    return altitude(ra, dec, jd), lon


def moon_state(jd):
    lon, lat, dist = moon.moon(jd)
    dpsi, eps = moon.nutation_and_obliquity(jd)
    lon += dpsi
    ra, dec = moon.to_equatorial(lon, lat, eps)
    geo = altitude(ra, dec, jd)
    parallax = math.degrees(math.asin(EARTH_RADIUS_KM / dist))
    topo = geo - parallax * math.cos(geo * D2R)
    return topo, lon, lat, dist, parallax


def find_crossing(f, jd_lo, jd_hi, target):
    """Bisection for the moment f(jd) goes from above to below target."""
    lo, hi = jd_lo, jd_hi
    for _ in range(50):
        mid = (lo + hi) / 2
        if f(mid) > target:
            lo = mid
        else:
            hi = mid
    return (lo + hi) / 2


def conjunction_before(jd):
    for _ in range(40):
        ml, _b, _d = moon.moon(jd)
        sl, _r = moon.sun_longitude(jd)
        diff = (ml - sl + 180) % 360 - 180
        jd -= diff / 12.19
    return jd


def features(evening):
    """evening: the civil date whose sunset ends day 29 of the month."""
    sunset = find_crossing(lambda j: sun_alt(j)[0], jd_at(evening, 12), jd_at(evening, 19), SUN_HORIZON)
    topo, mlon, mlat, dist, par = moon_state(sunset)
    slon = sun_alt(sunset)[1]
    sd = 0.2725 * par  # Moon's semi-diameter, degrees
    moon_horizon = -0.5667 - sd
    moonset = find_crossing(lambda j: moon_state(j)[0], sunset - 0.2, sunset + 0.3, moon_horizon) \
        if topo > moon_horizon else None
    conj = conjunction_before(sunset)
    # elongation (geocentric angular distance) and crescent width
    elong = math.degrees(math.acos(max(-1, min(1, math.cos((mlon - slon) * D2R) * math.cos(mlat * D2R)))))
    sun_alt_now = SUN_HORIZON
    arcv = topo - sun_alt_now
    w = sd * 60 * (1 - math.cos(elong * D2R))  # arc minutes
    yallop_q = (arcv - (11.8371 - 6.3226 * w + 0.7319 * w * w - 0.1018 * w ** 3)) / 10
    odeh_v = arcv - (7.1651 - 6.3226 * w + 0.7319 * w * w - 0.1018 * w ** 3)
    return {
        "age_h": (sunset - conj) * 24,
        "lag_min": (moonset - sunset) * 1440 if moonset is not None else -99.0,
        "moon_alt": topo, "elong": elong, "arcv": arcv, "w": w, "q": yallop_q, "v": odeh_v,
    }
