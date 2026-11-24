package work.ahmedhatem.mawaqit.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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
        assertTrue(first.chooseCity("أسوان"))
        assertTrue(first.chooseMakruhMinutes(15))
        closeAll()   // the process "ends"; a new store opens the same file
        val second = SettingsStore(dataStore()).settings.first()
        assertEquals("أسوان", second.place.label)
        assertEquals(15, second.makruhMinutes)
    }

    @Test
    fun aPointKeepsItsCoordinatesAndNamesTheNearestCity() = runBlocking {
        val store = SettingsStore(dataStore())
        assertTrue(store.choosePoint(30.46, 31.18))    // near Benha
        val place = store.settings.first().place
        assertTrue(place.isPoint)
        assertEquals(30.46, place.latitude, 0.0)
        assertEquals(31.18, place.longitude, 0.0)
        assertTrue(place.label, place.label.startsWith("قريب من "))
        // choosing a city afterwards replaces the point
        assertTrue(store.chooseCity("القاهرة"))
        assertFalse(store.settings.first().place.isPoint)
    }

    @Test
    fun badChoicesAreRefusedAndTheOldOnesStay() = runBlocking {
        val store = SettingsStore(dataStore())
        store.chooseCity("أسوان")
        store.chooseMakruhMinutes(10)
        assertFalse(store.chooseCity("لندن"))
        assertFalse(store.choosePoint(51.5, -0.1))     // outside Egypt
        assertFalse(store.choosePoint(Double.NaN, 31.0))
        assertFalse(store.chooseMakruhMinutes(7))
        assertFalse(store.chooseMakruhMinutes(0))
        assertFalse(store.chooseMakruhMinutes(-15))
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
    fun placeFactories() {
        assertEquals("القاهرة", Place.DEFAULT.label)
        assertNull(Place.ofCity("لندن"))
        assertNull(Place.ofPoint(0.0, 0.0))
        assertEquals("أسوان", Place.ofCity("أسوان")!!.label)
    }
}
