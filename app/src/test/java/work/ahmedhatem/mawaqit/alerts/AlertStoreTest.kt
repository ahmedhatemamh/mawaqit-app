package work.ahmedhatem.mawaqit.alerts

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import work.ahmedhatem.mawaqit.engine.Prayer
import work.ahmedhatem.mawaqit.engine.alerts.AlertKind
import work.ahmedhatem.mawaqit.engine.alerts.AlertRule
import work.ahmedhatem.mawaqit.engine.alerts.AlertTarget
import work.ahmedhatem.mawaqit.settings.SaveResult

class AlertStoreTest {
    @get:Rule
    val folder = TemporaryFolder()
    private val scopes = ArrayList<CoroutineScope>()
    private val today = LocalDate.of(2026, 11, 25)

    private fun dataStore(): DataStore<Preferences> =
        CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scopes += it }.let { scope ->
            PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "alerts.preferences_pb") }
        }

    @After
    fun stop() = runBlocking {
        scopes.forEach { it.coroutineContext[Job]!!.cancelAndJoin() }
        scopes.clear()
    }

    private fun rule(id: String, offset: Int = -30, targets: Set<AlertTarget> = setOf(AlertTarget.ASR)) =
        AlertRule(id, AlertKind.ALARM, targets, offset)

    @Test
    fun startsEmpty() = runBlocking {
        val store = AlertStore(dataStore())
        assertEquals(DecodedRules(emptyList(), 0), store.rules.first())
        assertTrue(store.prayed.first().isEmpty())
    }

    @Test
    fun savedRulesComeBackAndSurviveReopeningTheFile() = runBlocking {
        val ds = dataStore()
        val rules = listOf(rule("a"), rule("b", -10))
        assertEquals(SaveResult.SAVED, AlertStore(ds).save(rules))
        assertEquals(rules, AlertStore(ds).rules.first().rules)
        stop()
        assertEquals(rules, AlertStore(dataStore()).rules.first().rules)
    }

    @Test
    fun aListWithAProblemIsRefusedAndTheOldListStays() = runBlocking {
        val store = AlertStore(dataStore())
        val good = listOf(rule("a"))
        store.save(good)
        assertEquals(SaveResult.INVALID, store.save(listOf(rule("a"), rule("a", -20))))
        assertEquals(SaveResult.INVALID, store.save(listOf(rule("x", -181))))
        assertEquals(good, store.rules.first().rules)
    }

    @Test
    fun anEmptyIdIsRefusedAndTheOldListStays() = runBlocking {
        val store = AlertStore(dataStore())
        val good = listOf(rule("a"))
        store.save(good)
        assertEquals(SaveResult.INVALID, store.save(listOf(rule("a"), rule(""))))
        assertEquals(good, store.rules.first().rules)
    }

    @Test
    fun theFourthAlertBeforeOneTimeIsRefused() = runBlocking {
        val store = AlertStore(dataStore())
        assertEquals(SaveResult.SAVED, store.save(listOf(rule("1", -10), rule("2", -20), rule("3", -30))))
        assertEquals(SaveResult.INVALID, store.save(listOf(rule("1", -10), rule("2", -20), rule("3", -30), rule("4", -40))))
        assertEquals(3, store.rules.first().rules.size)
    }

    @Test
    fun aDamagedEntryInTheFileIsSkippedAndReported() = runBlocking {
        val ds = dataStore()
        val good = rule("a")
        ds.edit { it[stringPreferencesKey("rules")] = AlertCodec.encode(listOf(good)).dropLast(1) + ",{\"id\":\"broken\"}]" }
        val read = AlertStore(ds).rules.first()
        assertEquals(listOf(good), read.rules)
        assertEquals(1, read.skipped)
    }

    @Test
    fun prayedAnswersAreKeptForSevenDaysOnly() = runBlocking {
        val store = AlertStore(dataStore())
        assertEquals(SaveResult.SAVED, store.markPrayed(today, Prayer.ASR, today))
        assertEquals(SaveResult.SAVED, store.markPrayed(today.minusDays(7), Prayer.FAJR, today))
        assertEquals(setOf(today to Prayer.ASR, today.minusDays(7) to Prayer.FAJR), store.prayed.first())
        // a day later the eight-day-old answer goes
        assertEquals(SaveResult.SAVED, store.markPrayed(today.plusDays(1), Prayer.DHUHR, today.plusDays(1)))
        assertEquals(setOf(today to Prayer.ASR, today.plusDays(1) to Prayer.DHUHR), store.prayed.first())
        // an answer older than the window is not kept at all
        store.markPrayed(today.minusDays(30), Prayer.ISHA, today.plusDays(1))
        assertEquals(2, store.prayed.first().size)
    }

    @Test
    fun aStorageFailureIsReportedAndNothingIsHidden() = runBlocking {
        val failing = object : DataStore<Preferences> {
            override val data = dataStore().data
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = throw IOException("disk full")
        }
        val store = AlertStore(failing)
        assertEquals(SaveResult.STORAGE_FAILED, store.save(listOf(rule("a"))))
        assertEquals(SaveResult.STORAGE_FAILED, store.markPrayed(today, Prayer.ASR, today))
    }
}
