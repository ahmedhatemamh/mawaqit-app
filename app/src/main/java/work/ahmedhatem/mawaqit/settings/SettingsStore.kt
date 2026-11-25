package work.ahmedhatem.mawaqit.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

/** What happened to a change the user asked for. */
enum class SaveResult {
    SAVED,
    /** The value is not one of the allowed ones; nothing changed. */
    INVALID,
    /** The phone's storage refused the write (full, or not writable); the old choice is still in place. */
    STORAGE_FAILED,
}

/**
 * The user's choices, kept in a DataStore file. Whatever is read is checked again: a value that is missing or no
 * longer valid (a city that is not in the list, a duration that is not one of the four) gives the default for that
 * part only.
 *
 * A file that cannot be read is not fatal: the defaults are shown (nothing is written, so nothing saved is lost) and
 * reading is tried again with a growing pause, until it works. Any other error is a bug and is not hidden.
 */
class SettingsStore(
    private val store: DataStore<Preferences>,
    private val retryPauseMillis: Long = 1_000,
) {
    val settings: Flow<Settings> = flow { emitAll(store.data.map { read(it) }) }
        .retryWhen { cause, attempt ->
            if (cause !is IOException) return@retryWhen false
            if (attempt == 0L) emit(Settings())
            delay(minOf(retryPauseMillis * (attempt + 1), MAX_RETRY_PAUSE_MILLIS))
            true
        }

    suspend fun chooseCity(name: String): SaveResult {
        Place.ofCity(name) ?: return SaveResult.INVALID
        return write {
            it[CITY] = name
            it.remove(POINT_LAT)
            it.remove(POINT_LON)
        }
    }

    /** Refused (INVALID, the old choice stays) when the point is outside Egypt. */
    suspend fun choosePoint(latitude: Double, longitude: Double): SaveResult {
        Place.ofPoint(latitude, longitude) ?: return SaveResult.INVALID
        return write {
            it[POINT_LAT] = latitude
            it[POINT_LON] = longitude
            it.remove(CITY)
        }
    }

    suspend fun chooseMakruhMinutes(minutes: Int): SaveResult {
        if (minutes !in Settings.MAKRUH_CHOICES) return SaveResult.INVALID
        return write { it[MAKRUH] = minutes }
    }

    private suspend fun write(change: (androidx.datastore.preferences.core.MutablePreferences) -> Unit): SaveResult =
        try {
            store.edit(change)
            SaveResult.SAVED
        } catch (e: IOException) {
            SaveResult.STORAGE_FAILED
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
        const val MAX_RETRY_PAUSE_MILLIS = 30_000L
        val CITY = stringPreferencesKey("city")
        val POINT_LAT = doublePreferencesKey("point_lat")
        val POINT_LON = doublePreferencesKey("point_lon")
        val MAKRUH = intPreferencesKey("makruh_minutes")
    }
}
