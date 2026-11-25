package work.ahmedhatem.mawaqit.alerts

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import work.ahmedhatem.mawaqit.engine.Prayer
import work.ahmedhatem.mawaqit.engine.alerts.AlertRule
import work.ahmedhatem.mawaqit.engine.alerts.AlertRules
import work.ahmedhatem.mawaqit.settings.SaveResult

/**
 * The user's alert rules and the "I prayed" answers, kept in a DataStore file.
 *
 * Saving checks the whole list with [AlertRules.problems] first: a list with any problem is refused (INVALID) and the
 * old list stays. Reading checks each entry again and leaves out the ones that are damaged, saying how many.
 * A file that cannot be read shows no rules until reading works again (nothing is written meanwhile).
 */
class AlertStore(
    private val store: DataStore<Preferences>,
    private val retryPauseMillis: Long = 1_000,
) {
    val rules: Flow<DecodedRules> = readFlow { AlertCodec.decode(it[RULES]) }

    /** The answers "I prayed", only the last [KEEP_DAYS] days of them. */
    val prayed: Flow<Set<Pair<LocalDate, Prayer>>> = readFlow { AlertCodec.decodePrayed(it[PRAYED]) }

    /** Replaces the whole list. INVALID (nothing changed) when [rules] has any problem. */
    suspend fun save(rules: List<AlertRule>): SaveResult {
        if (AlertRules.problems(rules).isNotEmpty()) return SaveResult.INVALID
        return write { it[RULES] = AlertCodec.encode(rules) }
    }

    /** Remembers "I prayed [prayer] on [date]" and forgets answers older than [KEEP_DAYS] days before [today]. */
    suspend fun markPrayed(date: LocalDate, prayer: Prayer, today: LocalDate): SaveResult = write { p ->
        val kept = AlertCodec.decodePrayed(p[PRAYED]).filter { it.first >= today.minusDays(KEEP_DAYS.toLong()) }.toMutableSet()
        if (date >= today.minusDays(KEEP_DAYS.toLong())) kept += date to prayer
        p[PRAYED] = AlertCodec.encodePrayed(kept)
    }

    private fun <T> readFlow(read: (Preferences) -> T): Flow<T> = flow { emitAll(store.data.map(read)) }
        .retryWhen { cause, attempt ->
            if (cause !is IOException) return@retryWhen false
            if (attempt == 0L) emit(read(androidx.datastore.preferences.core.emptyPreferences()))
            delay(minOf(retryPauseMillis * (attempt + 1), MAX_RETRY_PAUSE_MILLIS))
            true
        }

    private suspend fun write(change: (androidx.datastore.preferences.core.MutablePreferences) -> Unit): SaveResult =
        try {
            store.edit(change)
            SaveResult.SAVED
        } catch (e: IOException) {
            SaveResult.STORAGE_FAILED
        }

    companion object {
        const val KEEP_DAYS = 7
        private const val MAX_RETRY_PAUSE_MILLIS = 30_000L
        private val RULES = stringPreferencesKey("rules")
        private val PRAYED = stringPreferencesKey("prayed")
    }
}
