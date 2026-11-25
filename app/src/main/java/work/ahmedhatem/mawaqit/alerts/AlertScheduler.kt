package work.ahmedhatem.mawaqit.alerts

import java.time.Instant
import java.time.LocalDate
import work.ahmedhatem.mawaqit.engine.Prayer
import work.ahmedhatem.mawaqit.engine.alerts.AlertPlanner
import work.ahmedhatem.mawaqit.engine.alerts.AlertRule
import work.ahmedhatem.mawaqit.engine.alerts.Occurrence
import work.ahmedhatem.mawaqit.settings.Settings

/** The phone's alarm clock, as far as the scheduler needs it. One alarm is pending at any time. */
interface AlarmBackend {
    /** Sets the one pending alarm (replacing any other). True when it is exact, false when the phone only allows an approximate time. */
    fun schedule(alert: FiredAlert, at: Instant): Boolean

    fun cancel()
}

sealed interface Plan {
    /** No alert is due in the planner's look-ahead: no alarm is pending. */
    data object None : Plan

    data class Scheduled(val occurrence: Occurrence, val exact: Boolean) : Plan
}

/**
 * Keeps exactly one alarm pending: the next alert. When it fires, the receiver shows it and plans the next one, so the
 * list never has to be larger than one and a change of rules, city or answers just plans again.
 */
class AlertScheduler(private val backend: AlarmBackend) {
    fun reschedule(
        rules: List<AlertRule>,
        settings: Settings,
        prayed: Set<Pair<LocalDate, Prayer>>,
        now: Instant,
    ): Plan {
        val next = nextOccurrence(rules, settings, prayed, now, AlertPlanner.DEFAULT_MAX_DAYS)
            // A one-day or date-range rule can start further ahead than the usual look-ahead; nothing else would plan it later.
            ?: nextOccurrence(rules, settings, prayed, now, FAR_LOOK_AHEAD_DAYS)
        if (next == null) {
            backend.cancel()
            return Plan.None
        }
        return Plan.Scheduled(next, backend.schedule(FiredAlert.of(next), next.at))
    }

    /**
     * The other alerts due at the very minute of [fired] (the alarm clock holds one entry, so a reminder and an alarm
     * set for the same time arrive together and the receiver shows them all). Answers given since then are respected.
     */
    fun companionsOf(fired: FiredAlert, rules: List<AlertRule>, settings: Settings, prayed: Set<Pair<LocalDate, Prayer>>): List<FiredAlert> {
        val at = Instant.ofEpochMilli(fired.atMillis)
        return AlertPlanner.upcoming(
            rules, at.minusSeconds(1), MAX_SIMULTANEOUS, settings.place.latitude, settings.place.longitude,
            settings.makruhMinutes, prayed, NEAR_LOOK_AHEAD_DAYS,
        ).filter { it.at == at && !(it.ruleId == fired.ruleId && it.target == fired.target) }.map { FiredAlert.of(it) }
    }

    private fun nextOccurrence(rules: List<AlertRule>, settings: Settings, prayed: Set<Pair<LocalDate, Prayer>>, now: Instant, maxDays: Int) =
        AlertPlanner.upcoming(
            rules, now, 1, settings.place.latitude, settings.place.longitude, settings.makruhMinutes, prayed, maxDays,
        ).firstOrNull()

    private companion object {
        const val FAR_LOOK_AHEAD_DAYS = 400
        const val NEAR_LOOK_AHEAD_DAYS = 2

        /** More than the rules can ever put on one minute (3 per kind and side for each of the 7 times). */
        const val MAX_SIMULTANEOUS = 100
    }
}
