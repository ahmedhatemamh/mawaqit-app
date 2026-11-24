package work.ahmedhatem.mawaqit.settings

import work.ahmedhatem.mawaqit.engine.CityFinder
import work.ahmedhatem.mawaqit.engine.PrayerCalculator

/**
 * Where the times are computed for: a city of the official list, or the exact point the phone reported (decision 2ب).
 * [label] is what the screen shows: the city name, or "قريب من <nearest city>" for a point.
 */
data class Place(val label: String, val latitude: Double, val longitude: Double, val isPoint: Boolean) {
    companion object {
        val DEFAULT: Place = CityFinder.DEFAULT.let { Place(it.name, it.latitude, it.longitude, isPoint = false) }

        fun ofCity(name: String): Place? =
            CityFinder.byName(name)?.let { Place(it.name, it.latitude, it.longitude, isPoint = false) }

        /** A phone position, or null when it is not inside Egypt (the tables do not cover it). */
        fun ofPoint(latitude: Double, longitude: Double): Place? {
            if (!PrayerCalculator.isInsideEgypt(latitude, longitude)) return null
            val near = CityFinder.nearest(latitude, longitude)
            return Place("قريب من ${near.name}", latitude, longitude, isPoint = true)
        }
    }
}

/** What the user chose; every field has a safe default. */
data class Settings(val place: Place = Place.DEFAULT, val makruhMinutes: Int = DEFAULT_MAKRUH_MINUTES) {
    companion object {
        val MAKRUH_CHOICES = listOf(10, 15, 20, 25)
        const val DEFAULT_MAKRUH_MINUTES = 25
    }
}
