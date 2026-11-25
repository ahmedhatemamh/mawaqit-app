package work.ahmedhatem.mawaqit.alerts

import java.time.Instant
import java.time.LocalDate
import work.ahmedhatem.mawaqit.engine.EgyptClock
import work.ahmedhatem.mawaqit.engine.Prayer
import work.ahmedhatem.mawaqit.engine.alerts.AlertPlanner
import work.ahmedhatem.mawaqit.engine.alerts.AlertRule
import work.ahmedhatem.mawaqit.engine.alerts.AlertTarget
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
     * The other alerts due together with [fired]: those at its minute, and (when the alarm came late) those that fell
     * due before [until], the moment it was delivered, since the one alarm clock entry could not tell about them.
     * Answers given since then are respected.
     */
    fun companionsOf(
        fired: FiredAlert,
        rules: List<AlertRule>,
        settings: Settings,
        prayed: Set<Pair<LocalDate, Prayer>>,
        until: Instant = Instant.ofEpochMilli(fired.atMillis),
    ): List<FiredAlert> {
        val at = Instant.ofEpochMilli(fired.atMillis)
        return AlertPlanner.upcoming(
            rules, at.minusSeconds(1), MAX_SIMULTANEOUS, settings.place.latitude, settings.place.longitude,
            settings.makruhMinutes, prayed, NEAR_LOOK_AHEAD_DAYS,
        ).filter { it.at <= maxOf(at, until) && !(it.ruleId == fired.ruleId && it.target == fired.target) }.map { FiredAlert.of(it) }
    }

    /** True when the user already answered "I prayed" for the question [fired] asks, so it must not be shown (the answer may arrive after the alarm was set). */
    fun isAnswered(fired: FiredAlert, prayed: Set<Pair<LocalDate, Prayer>>): Boolean {
        val question = fired.question ?: return false
        val targetAt = Instant.ofEpochMilli(fired.atMillis - fired.offsetMinutes * 60_000L)
        val date = EgyptClock.localDateTime(targetAt).toLocalDate()
        val questionDate = if (fired.target == AlertTarget.FAJR) date.minusDays(1) else date
        return (questionDate to AlertPlanner.questionPrayer(question)) in prayed
    }

    private fun nextOccurrence(rules: List<AlertRule>, settings: Settings, prayed: Set<Pair<LocalDate, Prayer>>, now: Instant, maxDays: Int) =
        AlertPlanner.upcoming(
            rules, now, 1, settings.place.latitude, settings.place.longitude, settings.makruhMinutes, prayed, maxDays,
        ).firstOrNull()

    companion object {
        /** A shown alert stays at least this long, even when the next prayer is very near. */
        const val MIN_VISIBLE_MINUTES = 15L

        /** An alert shown at [shownAt] disappears at the next prayer time, but not before [MIN_VISIBLE_MINUTES]. */
        fun expiryAfter(shownAt: Instant, settings: Settings): Instant = maxOf(
            AlertPlanner.nextDailyTimeAfter(shownAt, settings.place.latitude, settings.place.longitude),
            shownAt.plusSeconds(MIN_VISIBLE_MINUTES * 60),
        )

        private const val FAR_LOOK_AHEAD_DAYS = 400
        private const val NEAR_LOOK_AHEAD_DAYS = 2

        /** More than the rules can ever put on one minute (3 per kind and side for each of the 7 times). */
        private const val MAX_SIMULTANEOUS = 100
    }
}
