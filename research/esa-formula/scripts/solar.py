"""Precise sun position (NOAA / Meeus) and Egyptian prayer times, continuous minutes."""
import math, datetime as dt

def sun(jd):
    """Return (declination rad, equation of time minutes) for Julian day jd (UT)."""
    T = (jd - 2451545.0) / 36525.0
    L0 = (280.46646 + T * (36000.76983 + 0.0003032 * T)) % 360
    M = 357.52911 + T * (35999.05029 - 0.0001537 * T)
    e = 0.016708634 - T * (0.000042037 + 0.0000001267 * T)
    Mr = math.radians(M)
    C = (math.sin(Mr) * (1.914602 - T * (0.004817 + 0.000014 * T))
         + math.sin(2 * Mr) * (0.019993 - 0.000101 * T) + math.sin(3 * Mr) * 0.000289)
    true_long = L0 + C
    omega = 125.04 - 1934.136 * T
    app_long = true_long - 0.00569 - 0.00478 * math.sin(math.radians(omega))
    eps0 = 23 + (26 + (21.448 - T * (46.815 + T * (0.00059 - T * 0.001813))) / 60) / 60
    eps = eps0 + 0.00256 * math.cos(math.radians(omega))
    epsr = math.radians(eps)
    dec = math.asin(math.sin(epsr) * math.sin(math.radians(app_long)))
    y = math.tan(epsr / 2) ** 2
    L0r = math.radians(L0)
    eqt = 4 * math.degrees(y * math.sin(2 * L0r) - 2 * e * math.sin(Mr)
                           + 4 * e * y * math.sin(Mr) * math.cos(2 * L0r)
                           - 0.5 * y * y * math.sin(4 * L0r) - 1.25 * e * e * math.sin(2 * Mr))
    return dec, eqt

def jd_of(date):
    return date.toordinal() + 1721424.5  # JD at 0h UT of that civil date

def cairo_offset(date):
    """Egypt: UTC+2, DST UTC+3 from last Friday of April to last Thursday of October (inclusive)."""
    def last(month, weekday):
        d = dt.date(date.year, month + 1, 1) - dt.timedelta(days=1)
        while d.weekday() != weekday:
            d -= dt.timedelta(days=1)
        return d
    start, end = last(4, 4), last(10, 3)
    return 3 if start <= date <= end else 2

def times(date, lat, lon, fajr=19.5, isha=17.5, rise=0.833, asr_factor=1, tz=None, iters=3, sun_at=None, asr_noon=False, shift_h=0.0, dec_noon_all=False, dst_std=False, dst_mode=None):
    """Six times in local minutes (continuous): fajr, sunrise, dhuhr, asr, maghrib, isha."""
    tz = cairo_offset(date) if tz is None else tz
    phi = math.radians(lat)
    jd0 = jd_of(date)
    if sun_at is not None:
        fixed = sun(jd0 + sun_at / 24)
        _sun = sun
        sunf = lambda jd: fixed
    else:
        _sh = shift_h + ((tz - 2) if dst_std else 0)
        sunf = (lambda jd: sun(jd + _sh / 24)) if _sh else sun
    _dsh = (tz - 2) / 24 if dst_mode else 0.0
    def eqt_at(jd, kind):
        if dst_mode == "eqt_all" or (dst_mode == "dhuhr" and kind == "dhuhr"):
            return sunf(jd + _dsh)[1]
        return sunf(jd)[1]
    def transit(ut_h):
        eqt = eqt_at(jd0 + ut_h / 24, "dhuhr")
        return 12 - lon / 15 - eqt / 60          # UT hours
    def event(alt_deg, sign, alt_fn=None):
        ut = 12 - lon / 15
        for _ in range(iters):
            dec, _e = sunf(jd0 + ut / 24)
            eqt = eqt_at(jd0 + ut / 24, 'event')
            noon = 12 - lon / 15 - eqt / 60
            a = alt_fn(dec) if alt_fn else alt_deg
            cosH = (math.sin(math.radians(a)) - math.sin(phi) * math.sin(dec)) / (math.cos(phi) * math.cos(dec))
            H = math.degrees(math.acos(max(-1, min(1, cosH)))) / 15
            ut = noon + sign * H
        return ut
    noon_ut = 12 - lon / 15
    for _ in range(iters):
        noon_ut = transit(noon_ut)
    noon_dec = sunf(jd0 + noon_ut / 24)[0]
    asr_alt = lambda dec: math.degrees(math.atan(1 / (asr_factor + math.tan(abs(phi - (noon_dec if asr_noon else dec))))))
    ut = [event(-fajr, -1), event(-rise, -1), noon_ut, event(None, +1, asr_alt), event(-rise, +1), event(-isha, +1)]
    return [(u + tz) * 60 for u in ut]

def sun_meeus(jd, delta_t_s=0.0):
    """Meeus ch.25/28: apparent declination and equation of time (via right ascension + nutation)."""
    jde = jd + delta_t_s / 86400
    T = (jde - 2451545.0) / 36525.0
    L0 = (280.46646 + T * (36000.76983 + 0.0003032 * T)) % 360
    M = math.radians(357.52911 + T * (35999.05029 - 0.0001537 * T))
    C = ((1.914602 - T * (0.004817 + 0.000014 * T)) * math.sin(M)
         + (0.019993 - 0.000101 * T) * math.sin(2 * M) + 0.000289 * math.sin(3 * M))
    omega = math.radians(125.04452 - 1934.136261 * T)
    Ls = math.radians(280.4665 + 36000.7698 * T); Lm = math.radians(218.3165 + 481267.8813 * T)
    dpsi = (-17.20 * math.sin(omega) - 1.32 * math.sin(2 * Ls) - 0.23 * math.sin(2 * Lm) + 0.21 * math.sin(2 * omega)) / 3600
    deps = (9.20 * math.cos(omega) + 0.57 * math.cos(2 * Ls) + 0.10 * math.cos(2 * Lm) - 0.09 * math.cos(2 * omega)) / 3600
    eps0 = 23 + 26 / 60 + 21.448 / 3600 - (46.8150 * T + 0.00059 * T * T - 0.001813 * T ** 3) / 3600
    eps = math.radians(eps0 + deps)
    lam = math.radians(L0 + C - 0.00569 - 0.00478 * math.sin(omega))
    ra = math.degrees(math.atan2(math.cos(eps) * math.sin(lam), math.cos(lam))) % 360
    dec = math.asin(math.sin(eps) * math.sin(lam))
    E = (L0 - 0.0057183 - ra + dpsi * math.cos(eps))
    E = (E + 180) % 360 - 180
    return dec, 4 * E
