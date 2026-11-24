package work.ahmedhatem.mawaqit.engine

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

internal data class MoonPosition(val longitude: Double, val latitude: Double, val distanceKm: Double)

/**
 * Low-precision Sun and Moon positions (Meeus, Astronomical Algorithms, ch. 25 and 47, truncated; about 0.1 degree).
 * A line-by-line port of research/hijri-estimate/moon.py, checked against it by the golden-file tests. It is only used
 * to estimate crescent visibility, whose thresholds are far coarser than that.
 */
internal object Lunar {
    private const val D2R = Math.PI / 180.0

    /** Python's `%` for floats: the result takes the sign of the divisor. */
    fun mod(x: Double, m: Double): Double {
        val r = x % m
        return if (r != 0.0 && (r < 0) != (m < 0)) r + m else r
    }

    private fun centuries(jde: Double) = (jde - 2451545.0) / 36525.0

    /** Apparent ecliptic longitude of the Sun in degrees. */
    fun sunLongitude(jde: Double): Double {
        val t = centuries(jde)
        val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t
        val m = 357.52911 + 35999.05029 * t - 0.0001537 * t * t
        val mr = m * D2R
        val c = ((1.914602 - 0.004817 * t) * sin(mr) + (0.019993 - 0.000101 * t) * sin(2 * mr) + 0.000289 * sin(3 * mr))
        val omega = 125.04 - 1934.136 * t
        val apparent = l0 + c - 0.00569 - 0.00478 * sin(omega * D2R)
        return mod(apparent, 360.0)
    }

    private fun series(
        table: DoubleArray, d: Double, m: Double, mp: Double, f: Double, e: Double, useCosine: Boolean,
    ): Double {
        var sum = 0.0
        var i = 0
        while (i < table.size) {
            val arg = (table[i] * d + table[i + 1] * m + table[i + 2] * mp + table[i + 3] * f) * D2R
            val term = if (useCosine) cos(arg) else sin(arg)
            sum += table[i + 4] * e.pow(abs(table[i + 1])) * term
            i += 5
        }
        return sum
    }

    /** Geocentric ecliptic longitude and latitude (degrees) and distance (km) of the Moon. */
    fun moon(jde: Double): MoonPosition {
        val t = centuries(jde)
        val lp = 218.3164477 + 481267.88123421 * t - 0.0015786 * t * t
        val d = 297.8501921 + 445267.1114034 * t - 0.0018819 * t * t
        val m = 357.5291092 + 35999.0502909 * t - 0.0001536 * t * t
        val mp = 134.9633964 + 477198.8675055 * t + 0.0087414 * t * t
        val f = 93.2720950 + 483202.0175233 * t - 0.0036539 * t * t
        val a1 = 119.75 + 131.849 * t
        val a2 = 53.09 + 479264.290 * t
        val a3 = 313.45 + 481266.484 * t
        val e = 1 - 0.002516 * t - 0.0000074 * t * t
        var sl = series(MoonTables.LONGITUDE, d, m, mp, f, e, false)
        var sb = series(MoonTables.LATITUDE, d, m, mp, f, e, false)
        val sr = series(MoonTables.DISTANCE, d, m, mp, f, e, true)
        sl += 3958 * sin(a1 * D2R) + 1962 * sin((lp - f) * D2R) + 318 * sin(a2 * D2R)
        sb += (-2235 * sin(lp * D2R) + 382 * sin(a3 * D2R) + 175 * sin((a1 - f) * D2R) +
            175 * sin((a1 + f) * D2R) + 127 * sin((lp - mp) * D2R) - 115 * sin((lp + mp) * D2R))
        return MoonPosition(mod(lp + sl / 1e6, 360.0), sb / 1e6, 385000.56 + sr / 1000.0)
    }

    /** Nutation in longitude (degrees) and true obliquity of the ecliptic (degrees). */
    fun nutationAndObliquity(jde: Double): Pair<Double, Double> {
        val t = centuries(jde)
        val omega = (125.04452 - 1934.136261 * t) * D2R
        val ls = (280.4665 + 36000.7698 * t) * D2R
        val lm = (218.3165 + 481267.8813 * t) * D2R
        val dpsi = (-17.20 * sin(omega) - 1.32 * sin(2 * ls) - 0.23 * sin(2 * lm) + 0.21 * sin(2 * omega)) / 3600
        val deps = (9.20 * cos(omega) + 0.57 * cos(2 * ls) + 0.10 * cos(2 * lm) - 0.09 * cos(2 * omega)) / 3600
        val eps0 = 23.0 + 26.0 / 60 + 21.448 / 3600 - (46.8150 * t + 0.00059 * t * t - 0.001813 * t * t * t) / 3600
        return Pair(dpsi, eps0 + deps)
    }

    /** Right ascension and declination (radians) from ecliptic longitude and latitude (degrees). */
    fun toEquatorial(lon: Double, lat: Double, eps: Double): Pair<Double, Double> {
        val l = lon * D2R
        val b = lat * D2R
        val e = eps * D2R
        val ra = atan2(sin(l) * cos(e) - tan(b) * sin(e), cos(l))
        val dec = asin(sin(b) * cos(e) + cos(b) * sin(e) * sin(l))
        return Pair(mod(ra, 2 * Math.PI), dec)
    }

    /** Greenwich mean sidereal time in radians. */
    fun gmst(jdUt: Double): Double {
        val t = (jdUt - 2451545.0) / 36525.0
        val g = 280.46061837 + 360.98564736629 * (jdUt - 2451545.0) + 0.000387933 * t * t - t * t * t / 38710000.0
        return mod(g, 360.0) * D2R
    }
}
