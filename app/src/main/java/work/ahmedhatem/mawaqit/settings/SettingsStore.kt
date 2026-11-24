package work.ahmedhatem.mawaqit.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The user's choices, kept in a DataStore file. Whatever is read is checked again: a value that is missing or no
 * longer valid (a city that is not in the list, a duration that is not one of the four) gives the default for that
 * part only.
 */
class SettingsStore(private val store: DataStore<Preferences>) {
    val settings: Flow<Settings> = store.data.map { read(it) }

    suspend fun chooseCity(name: String): Boolean {
        Place.ofCity(name) ?: return false
        store.edit {
            it[CITY] = name
            it.remove(POINT_LAT)
            it.remove(POINT_LON)
        }
        return true
    }

    /** Returns false (and keeps the old choice) when the point is outside Egypt. */
    suspend fun choosePoint(latitude: Double, longitude: Double): Boolean {
        Place.ofPoint(latitude, longitude) ?: return false
        store.edit {
            it[POINT_LAT] = latitude
            it[POINT_LON] = longitude
            it.remove(CITY)
        }
        return true
    }

    suspend fun chooseMakruhMinutes(minutes: Int): Boolean {
        if (minutes !in Settings.MAKRUH_CHOICES) return false
        store.edit { it[MAKRUH] = minutes }
        return true
    }

    private fun read(p: Preferences): Settings {
        val lat = p[POINT_LAT]
        val lon = p[POINT_LON]
        val place = (if (lat != null && lon != null) Place.ofPoint(lat, lon) else null)
            ?: p[CITY]?.let { Place.ofCity(it) }
            ?: Place.DEFAULT
        val makruh = p[MAKRUH]?.takeIf { it in Settings.MAKRUH_CHOICES } ?: Settings.DEFAULT_MAKRUH_MINUTES
        return Settings(place, makruh)
    }

    private companion object {
        val CITY = stringPreferencesKey("city")
        val POINT_LAT = doublePreferencesKey("point_lat")
        val POINT_LON = doublePreferencesKey("point_lon")
        val MAKRUH = intPreferencesKey("makruh_minutes")
    }
}
