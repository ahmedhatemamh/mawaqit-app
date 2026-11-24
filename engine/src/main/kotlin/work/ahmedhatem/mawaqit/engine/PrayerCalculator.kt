package work.ahmedhatem.mawaqit.engine

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.PI

/** The six times of one day, in minutes after local midnight (Egypt time). */
class DayTimes internal constructor(
    val date: LocalDate,
    /** Continuous values, before rounding. */
    val exactMinutes: List<Double>,
    val utcOffsetHours: Int,
    /** False when the summer-time rule for this year is only an estimate (see [EgyptDst]). */
    val dstConfirmed: Boolean,
) {
    /** Whole minutes, rounded to the nearest. */
    val minutes: List<Int> = exactMinutes.map { floor(it + 0.5).toInt() }

    operator fun get(prayer: Prayer): Int = minutes[prayer.ordinal]
}

/**
 * The calculation that best matched the official tables in the research phase (see research/esa-formula):
 * VSOP87 sun, sunrise and sunset at 0.845 degrees below the horizon, Fajr at 19.5, Isha at 17.5, Asr by the
 * shadow equal to the object using the Sun's declination at noon, and the equation of time of summer days
 * taken one hour later, which is how the tables behave. Ported line by line from solar.py.
 */
object PrayerCalculator {
    private const val FAJR_ANGLE = 19.5
    private const val ISHA_ANGLE = 17.5
    private const val RISE_ANGLE = 0.845
    private const val ITERATIONS = 3

    // Egypt's borders with a small margin. Anything outside is a caller's mistake, not a place to compute.
    private const val MIN_LATITUDE = 21.5
    private const val MAX_LATITUDE = 32.0
    private const val MIN_LONGITUDE = 24.0
    private const val MAX_LONGITUDE = 37.5

    fun timesFor(date: LocalDate, city: City): DayTimes = timesFor(date, city.latitude, city.longitude)

    fun timesFor(date: LocalDate, latitude: Double, longitude: Double): DayTimes {
        require(latitude in MIN_LATITUDE..MAX_LATITUDE && longitude in MIN_LONGITUDE..MAX_LONGITUDE) {
            "coordinates are outside Egypt: $latitude, $longitude"
        }
        val offset = EgyptDst.offsetFor(date)
        val exact = compute(date, latitude, longitude, offset.hours)
        return DayTimes(date, exact, offset.hours, offset.confirmed)
    }

    private fun rad(d: Double) = d * PI / 180.0
    private fun deg(r: Double) = r * 180.0 / PI

    /** Julian day at 0h UT of the civil date. */
    private fun julianDay(date: LocalDate) = date.toEpochDay() + 2440587.5

    private fun compute(date: LocalDate, lat: Double, lon: Double, tz: Int): List<Double> {
        val phi = rad(lat)
        val jd0 = julianDay(date)
        val dstShiftDays = (tz - 2) / 24.0
        // Summer days read the equation of time one hour later, in every part of the calculation.
        fun eqt(ut: Double) = Vsop87.sun(jd0 + ut / 24 + dstShiftDays).equationOfTimeMinutes
        fun dec(ut: Double) = Vsop87.sun(jd0 + ut / 24).declination

        fun transit(ut: Double) = 12 - lon / 15 - eqt(ut) / 60

        fun event(altitude: (Double) -> Double, sign: Int): Double {
            var ut = 12 - lon / 15
            repeat(ITERATIONS) {
                val d = dec(ut)
                val noon = 12 - lon / 15 - eqt(ut) / 60
                val cosH = (sin(rad(altitude(d))) - sin(phi) * sin(d)) / (cos(phi) * cos(d))
                val h = deg(acos(cosH.coerceIn(-1.0, 1.0))) / 15
                ut = noon + sign * h
            }
            return ut
        }

        var noonUt = 12 - lon / 15
        repeat(ITERATIONS) { noonUt = transit(noonUt) }
        val noonDec = dec(noonUt)
        val asrAltitude = { _: Double -> deg(atan(1 / (1 + tan(abs(phi - noonDec))))) }

        val ut = listOf(
            event({ -FAJR_ANGLE }, -1),
            event({ -RISE_ANGLE }, -1),
            noonUt,
            event(asrAltitude, +1),
            event({ -RISE_ANGLE }, +1),
            event({ -ISHA_ANGLE }, +1),
        )
        return ut.map { (it + tz) * 60 }
    }
}
