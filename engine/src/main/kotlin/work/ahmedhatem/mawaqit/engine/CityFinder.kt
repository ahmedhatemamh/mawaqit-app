package work.ahmedhatem.mawaqit.engine

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Lookups over [Cities]. */
object CityFinder {
    private const val EARTH_RADIUS_KM = 6371.0

    /** The city shown before the user chooses one. */
    val DEFAULT: City = Cities.all.first { it.name == "القاهرة" }

    fun byName(name: String): City? = Cities.all.firstOrNull { it.name == name }

    /** The listed city closest to a point, by great-circle distance. */
    fun nearest(latitude: Double, longitude: Double): City =
        Cities.all.minByOrNull { distanceKm(latitude, longitude, it.latitude, it.longitude) }!!

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dLat = p2 - p1
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(p1) * cos(p2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
    }
}
