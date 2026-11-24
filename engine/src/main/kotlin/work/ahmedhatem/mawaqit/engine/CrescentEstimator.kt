package work.ahmedhatem.mawaqit.engine

import java.time.LocalDate
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sin

/**
 * Estimates the first day of a Hijri month from the crescent at Cairo. A line-by-line port of
 * research/hijri-estimate/estimator.py, which holds the measured accuracy: 91.7% of the 133 official month starts
 * from 2016 to 2026, always off by one day when wrong. The result is only ever shown as "not certain".
 *
 * For one lunation: take the Cairo date of the new moon, then the first evening (that day or the next three) on which
 * the Moon's centre is at least [MIN_ALTITUDE] degrees above the horizon at sunset. The month starts the next day.
 */
internal object CrescentEstimator {
    const val MIN_ALTITUDE = 1.6
    const val SYNODIC_MONTH = 29.530588861
    private const val REFERENCE_NEW_MOON_JDE = 2451550.09766  // Meeus ch. 49, lunation 0
    private const val LATITUDE = 30.0444   // Cairo
    private const val LONGITUDE = 31.2357
    private const val SUN_HORIZON = -0.8333
    private const val EARTH_RADIUS_KM = 6378.14
    private const val D2R = Math.PI / 180.0
    private const val JD_AT_ORDINAL_ZERO = 1721424.5      // Julian day of 0h UT on proleptic Gregorian ordinal 0
    private const val ORDINAL_OF_EPOCH_DAY_ZERO = 719163L  // ordinal of 1970-01-01

    private fun jdAt(date: LocalDate, utHours: Double) =
        date.toEpochDay() + ORDINAL_OF_EPOCH_DAY_ZERO + JD_AT_ORDINAL_ZERO + utHours / 24.0

    private fun altitude(ra: Double, dec: Double, jd: Double): Double {
        val h = Lunar.gmst(jd) + LONGITUDE * D2R - ra
        val phi = LATITUDE * D2R
        return Math.toDegrees(asin(sin(phi) * sin(dec) + cos(phi) * cos(dec) * cos(h)))
    }

    private fun sunAltitude(jd: Double): Double {
        val lon = Lunar.sunLongitude(jd)
        val eps = Lunar.nutationAndObliquity(jd).second
        val (ra, dec) = Lunar.toEquatorial(lon, 0.0, eps)
        return altitude(ra, dec, jd)
    }

    /** Topocentric altitude of the Moon's centre in degrees. */
    private fun moonAltitude(jd: Double): Double {
        val p = Lunar.moon(jd)
        val (dpsi, eps) = Lunar.nutationAndObliquity(jd)
        val (ra, dec) = Lunar.toEquatorial(p.longitude + dpsi, p.latitude, eps)
        val geo = altitude(ra, dec, jd)
        val parallax = Math.toDegrees(asin(EARTH_RADIUS_KM / p.distanceKm))
        return geo - parallax * cos(geo * D2R)
    }

    /** Bisection for the moment f falls through [target]; f must be above it at [lo] and below it at [hi]. */
    private fun crossing(f: (Double) -> Double, lo: Double, hi: Double, target: Double): Double {
        var a = lo
        var b = hi
        repeat(50) {
            val mid = (a + b) / 2
            if (f(mid) > target) a = mid else b = mid
        }
        return (a + b) / 2
    }

    /** Julian day of the new moon nearest to [guess] (Newton steps on the Moon-Sun longitude difference). */
    private fun conjunctionNear(guess: Double): Double {
        var jd = guess
        repeat(40) {
            val diff = Lunar.mod(Lunar.moon(jd).longitude - Lunar.sunLongitude(jd) + 180, 360.0) - 180
            jd -= diff / 12.19  // mean relative motion of the Moon, degrees per day
        }
        return jd
    }

    fun newMoon(k: Long): Double = conjunctionNear(REFERENCE_NEW_MOON_JDE + k * SYNODIC_MONTH)

    /** The Cairo civil date (taken as UTC+2, since only the day matters) of a Julian day. */
    fun cairoDate(jd: Double): LocalDate {
        val ordinal = floor(jd - JD_AT_ORDINAL_ZERO + 2.0 / 24).toLong()
        return LocalDate.ofEpochDay(ordinal - ORDINAL_OF_EPOCH_DAY_ZERO)
    }

    /** Altitude of the Moon's centre at sunset in Cairo on [evening]. */
    fun moonAltitudeAtSunset(evening: LocalDate): Double {
        val sunset = crossing(::sunAltitude, jdAt(evening, 12.0), jdAt(evening, 19.0), SUN_HORIZON)
        return moonAltitude(sunset)
    }

    /** First day of the Hijri month that follows the new moon of lunation [k]. */
    fun estimatedStart(k: Long): LocalDate {
        val first = cairoDate(newMoon(k))
        for (extra in 0L until 4L) {
            val evening = first.plusDays(extra)
            if (moonAltitudeAtSunset(evening) >= MIN_ALTITUDE) return evening.plusDays(1)
        }
        return first.plusDays(4)
    }

    /** The lunation whose new moon falls 0 to 3 days before a known month start. */
    fun lunationOfStart(start: LocalDate): Long {
        val k0 = round((jdAt(start, 0.0) - REFERENCE_NEW_MOON_JDE) / SYNODIC_MONTH).toLong()
        for (k in longArrayOf(k0 - 1, k0, k0 + 1)) {
            val d = cairoDate(newMoon(k))
            if (!d.isBefore(start.minusDays(3)) && !d.isAfter(start)) return k
        }
        throw IllegalArgumentException("no lunation matches the month start $start")
    }
}
