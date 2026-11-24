package work.ahmedhatem.mawaqit.engine

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.asin
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.PI

/** Apparent declination (radians) and equation of time (minutes) of the Sun. */
internal data class SunPosition(val declination: Double, val equationOfTimeMinutes: Double)

/**
 * Apparent position of the Sun from the abridged VSOP87 Earth series (Meeus, Astronomical Algorithms, ch. 25 and 32).
 * A line-by-line port of research/esa-formula/scripts/vsop.py, checked against it by the golden-file test.
 */
internal object Vsop87 {
    private fun toRadians(d: Double) = d * PI / 180.0
    private fun toDegrees(r: Double) = r * 180.0 / PI

    /** Python's `%` for floats: the result takes the sign of the divisor. */
    private fun mod(x: Double, m: Double): Double {
        val r = x % m
        return if (r != 0.0 && (r < 0) != (m < 0)) r + m else r
    }

    private fun series(t: Double, terms: DoubleArray): Double {
        var sum = 0.0
        var i = 0
        while (i < terms.size) {
            sum += terms[i] * cos(terms[i + 1] + terms[i + 2] * t)
            i += 3
        }
        return sum
    }

    /** [jd] is the Julian day in UT; [deltaTSeconds] converts it to dynamical time. */
    fun sun(jd: Double, deltaTSeconds: Double = DELTA_T_SECONDS): SunPosition {
        val jde = jd + deltaTSeconds / 86400.0
        val tau = (jde - 2451545.0) / 365250.0
        val t = tau * 10
        val l = (series(tau, Vsop87Tables.L0) + series(tau, Vsop87Tables.L1) * tau + series(tau, Vsop87Tables.L2) * tau * tau +
            series(tau, Vsop87Tables.L3) * Math.pow(tau, 3.0) + series(tau, Vsop87Tables.L4) * Math.pow(tau, 4.0) +
            series(tau, Vsop87Tables.L5) * Math.pow(tau, 5.0)) / 1e8
        val b = (series(tau, Vsop87Tables.B0) + series(tau, Vsop87Tables.B1) * tau) / 1e8
        val r = (series(tau, Vsop87Tables.R0) + series(tau, Vsop87Tables.R1) * tau + series(tau, Vsop87Tables.R2) * tau * tau) / 1e8
        var lam = toDegrees(l) + 180.0
        var beta = -toDegrees(b)
        // FK5 correction
        val lp = toRadians(lam - 1.397 * t - 0.00031 * t * t)
        lam += -0.09033 / 3600
        beta += 0.03916 / 3600 * (cos(lp) - sin(lp))
        // nutation (Meeus 22, low order) and obliquity
        val omega = toRadians(125.04452 - 1934.136261 * t)
        val ls = toRadians(280.4665 + 36000.7698 * t)
        val lm = toRadians(218.3165 + 481267.8813 * t)
        val dpsi = (-17.20 * sin(omega) - 1.32 * sin(2 * ls) - 0.23 * sin(2 * lm) + 0.21 * sin(2 * omega)) / 3600
        val deps = (9.20 * cos(omega) + 0.57 * cos(2 * ls) + 0.10 * cos(2 * lm) - 0.09 * cos(2 * omega)) / 3600
        val u = t / 100
        val eps0 = 23 + 26.0 / 60 + 21.448 / 3600 + (-4680.93 * u - 1.55 * Math.pow(u, 2.0) + 1999.25 * Math.pow(u, 3.0) -
            51.38 * Math.pow(u, 4.0) - 249.67 * Math.pow(u, 5.0) - 39.05 * Math.pow(u, 6.0) + 7.12 * Math.pow(u, 7.0) +
            27.87 * Math.pow(u, 8.0) + 5.79 * Math.pow(u, 9.0) + 2.45 * Math.pow(u, 10.0)) / 3600
        val eps = toRadians(eps0 + deps)
        val lamApp = toRadians(lam + dpsi - 20.4898 / 3600 / r)
        val bRad = toRadians(beta)
        val ra = mod(toDegrees(atan2(sin(lamApp) * cos(eps) - tan(bRad) * sin(eps), cos(lamApp))), 360.0)
        val dec = asin(sin(bRad) * cos(eps) + cos(bRad) * sin(eps) * sin(lamApp))
        val l0m = mod(
            280.4664567 + 360007.6982779 * tau + 0.03032028 * tau * tau + Math.pow(tau, 3.0) / 49931 -
                Math.pow(tau, 4.0) / 15300 - Math.pow(tau, 5.0) / 2000000,
            360.0,
        )
        var e = l0m - 0.0057183 - ra + dpsi * cos(eps)
        e = mod(e + 180, 360.0) - 180
        return SunPosition(dec, 4 * e)
    }

    /** Difference between dynamical time and UT that the best-fit configuration uses, in seconds. */
    const val DELTA_T_SECONDS = 69.0
}
