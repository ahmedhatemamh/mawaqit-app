"""Estimate the first day of a Hijri month from the crescent at Cairo (the reference for the Kotlin engine).

For one lunation: take the Cairo date of the new moon, then the first evening (that day or the next three) on which the
Moon is at least MIN_ALTITUDE degrees above the horizon at sunset. The month starts the next day.
Months are numbered from a known month start (the anchor): the lunation whose new moon falls 0 to 4 days before it.
"""
import datetime as dt
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import crescent

MIN_ALTITUDE = 1.6        # degrees, topocentric altitude of the Moon's centre at sunset (see README)
SYNODIC_MONTH = 29.530588861
REFERENCE_NEW_MOON_JDE = 2451550.09766  # Meeus ch. 49, lunation 0


def new_moon(k):
    """Julian day (UT, treated as dynamical) of the new moon of lunation k."""
    return crescent.conjunction_before(REFERENCE_NEW_MOON_JDE + k * SYNODIC_MONTH)


def cairo_date(jd):
    return dt.date.fromordinal(int(math.floor(jd - 1721424.5 + 2 / 24)))


def estimated_start_and_margin(k):
    """(first day of the month, smallest distance in degrees between a checked altitude and the threshold)."""
    first = cairo_date(new_moon(k))
    margin = 1e9
    for extra in range(4):
        evening = first + dt.timedelta(days=extra)
        alt = crescent.features(evening)["moon_alt"]
        margin = min(margin, abs(alt - MIN_ALTITUDE))
        if alt >= MIN_ALTITUDE:
            return evening + dt.timedelta(days=1), margin
    return first + dt.timedelta(days=4), margin


def estimated_start(k):
    """First day of the Hijri month that follows the new moon of lunation k."""
    return estimated_start_and_margin(k)[0]


def lunation_of_start(start):
    """Lunation whose new moon falls 0 to 4 days before a known month start (the range estimated_start can produce)."""
    k0 = round((crescent.jd_at(start, 0) - REFERENCE_NEW_MOON_JDE) / SYNODIC_MONTH)
    for k in (k0 - 1, k0, k0 + 1):
        d = cairo_date(new_moon(k))
        if start - dt.timedelta(days=4) <= d <= start:
            return k
    raise ValueError("no lunation matches the month start %s" % start)


def estimate_from_anchor(anchor_start, anchor_index, month_index):
    """Estimated first day of the month number month_index (year*12+month-1), counted from a known month start."""
    return estimated_start(lunation_of_start(anchor_start) + (month_index - anchor_index))
