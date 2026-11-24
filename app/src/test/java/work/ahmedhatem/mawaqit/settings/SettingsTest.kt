package work.ahmedhatem.mawaqit.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.Assert.assertThrows
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsTest {
    @get:Rule
    val folder = TemporaryFolder()
    private val scopes = ArrayList<CoroutineScope>()

    private fun dataStore(name: String = "settings.preferences_pb") =
        CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scopes += it }.let { scope ->
            PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, name) }
        }

    /** Cancels every store scope and waits until it has really finished: only then is the file free to open again. */
    private fun closeAll() = runBlocking {
        scopes.forEach { it.coroutineContext[Job]!!.cancelAndJoin() }
        scopes.clear()
    }

    @After
    fun stop() = closeAll()

    @Test
    fun defaultsWhenNothingIsSaved() = runBlocking {
        val s = SettingsStore(dataStore()).settings.first()
        assertEquals("القاهرة", s.place.label)
        assertFalse(s.place.isPoint)
        assertEquals(25, s.makruhMinutes)
    }

    @Test
    fun choicesSurviveRestart() = runBlocking {
        val first = SettingsStore(dataStore())
        assertEquals(SaveResult.SAVED, first.chooseCity("أسوان"))
        assertEquals(SaveResult.SAVED, first.chooseMakruhMinutes(15))
        closeAll()   // the process "ends"; a new store opens the same file
        val second = SettingsStore(dataStore()).settings.first()
        assertEquals("أسوان", second.place.label)
        assertEquals(15, second.makruhMinutes)
    }

    @Test
    fun aPointKeepsItsCoordinatesAndNamesTheNearestCity() = runBlocking {
        val store = SettingsStore(dataStore())
        assertEquals(SaveResult.SAVED, store.choosePoint(30.46, 31.18))    // near Benha
        val place = store.settings.first().place
        assertTrue(place.isPoint)
        assertEquals(30.46, place.latitude, 0.0)
        assertEquals(31.18, place.longitude, 0.0)
        assertTrue(place.label, place.label.startsWith("قريب من "))
        // choosing a city afterwards replaces the point
        assertEquals(SaveResult.SAVED, store.chooseCity("القاهرة"))
        assertFalse(store.settings.first().place.isPoint)
    }

    @Test
    fun badChoicesAreRefusedAndTheOldOnesStay() = runBlocking {
        val store = SettingsStore(dataStore())
        store.chooseCity("أسوان")
        store.chooseMakruhMinutes(10)
        assertEquals(SaveResult.INVALID, store.chooseCity("لندن"))
        assertEquals(SaveResult.INVALID, store.choosePoint(51.5, -0.1))     // outside Egypt
        assertEquals(SaveResult.INVALID, store.choosePoint(Double.NaN, 31.0))
        assertEquals(SaveResult.INVALID, store.chooseMakruhMinutes(7))
        assertEquals(SaveResult.INVALID, store.chooseMakruhMinutes(0))
        assertEquals(SaveResult.INVALID, store.chooseMakruhMinutes(-15))
        val s = store.settings.first()
        assertEquals("أسوان", s.place.label)
        assertEquals(10, s.makruhMinutes)
    }

    @Test
    fun savedValuesThatAreNoLongerValidFallBackPartByPart() = runBlocking {
        val ds = dataStore()
        ds.edit {
            it[stringPreferencesKey("city")] = "مدينة_لم_تعد_موجودة"
            it[intPreferencesKey("makruh_minutes")] = 99
        }
        val s = SettingsStore(ds).settings.first()
        assertEquals("القاهرة", s.place.label)
        assertEquals(25, s.makruhMinutes)
        ds.edit {
            it[stringPreferencesKey("city")] = "أسوان"
            it[doublePreferencesKey("point_lat")] = 51.5     // a point outside Egypt: ignored, the city stays
            it[doublePreferencesKey("point_lon")] = -0.1
            it[intPreferencesKey("makruh_minutes")] = 20
        }
        val t = SettingsStore(ds).settings.first()
        assertEquals("أسوان", t.place.label)
        assertEquals(20, t.makruhMinutes)
    }

    @Test
    fun anInvalidCityDoesNotTouchAValidDuration() = runBlocking {
        val ds = dataStore()
        ds.edit {
            it[stringPreferencesKey("city")] = "مدينة_لم_تعد_موجودة"
            it[intPreferencesKey("makruh_minutes")] = 15
        }
        val s = SettingsStore(ds).settings.first()
        assertEquals("القاهرة", s.place.label)
        assertEquals(15, s.makruhMinutes)
    }

    @Test
    fun anInvalidDurationDoesNotTouchAValidCity() = runBlocking {
        val ds = dataStore()
        ds.edit {
            it[stringPreferencesKey("city")] = "أسوان"
            it[intPreferencesKey("makruh_minutes")] = 99
        }
        val s = SettingsStore(ds).settings.first()
        assertEquals("أسوان", s.place.label)
        assertEquals(25, s.makruhMinutes)
    }

    /** A store whose file cannot be read the first [failures] times, and cannot be written when [writes] is false. */
    private class FlakyStore(private var failures: Int, private val writes: Boolean = true) : DataStore<Preferences> {
        var readAttempts = 0
        private val state = MutableStateFlow(preferencesOf(stringPreferencesKey("city") to "أسوان"))
        override val data = flow {
            readAttempts++
            if (failures-- > 0) throw IOException("cannot read")
            emitAll(state)
        }
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            if (!writes) throw IOException("disk full")
            return transform(state.value).also { state.value = it }
        }
    }

    @Test
    fun anUnreadableFileShowsDefaultsAtOnceAndThenTheSavedChoicesWhenReadingWorks() = runBlocking {
        val flaky = FlakyStore(failures = 2)
        val seen = SettingsStore(flaky, retryPauseMillis = 5).settings.take(2).toList()
        assertEquals("القاهرة", seen[0].place.label)     // defaults while it cannot be read
        assertEquals("أسوان", seen[1].place.label)       // the saved choice, once it can
        assertEquals(3, flaky.readAttempts)
    }

    @Test
    fun anErrorThatIsNotAReadFailureIsNotHidden() {
        val broken = object : DataStore<Preferences> {
            override val data = flow<Preferences> { throw IllegalStateException("bug") }
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = error("unused")
        }
        assertThrows(IllegalStateException::class.java) { runBlocking { SettingsStore(broken, 5).settings.first() } }
    }

    @Test
    fun aFailedWriteIsReportedAndTheOldChoiceStays() = runBlocking {
        val flaky = FlakyStore(failures = 0, writes = false)
        val store = SettingsStore(flaky, retryPauseMillis = 5)
        assertEquals(SaveResult.STORAGE_FAILED, store.chooseCity("القاهرة"))
        assertEquals(SaveResult.STORAGE_FAILED, store.choosePoint(30.46, 31.18))
        assertEquals(SaveResult.STORAGE_FAILED, store.chooseMakruhMinutes(10))
        assertEquals("أسوان", store.settings.first().place.label)
        assertEquals(25, store.settings.first().makruhMinutes)
    }

    @Test
    fun placeFactories() {
        assertEquals("القاهرة", Place.DEFAULT.label)
        assertNull(Place.ofCity("لندن"))
        assertNull(Place.ofPoint(0.0, 0.0))
        assertEquals("أسوان", Place.ofCity("أسوان")!!.label)
    }
}
