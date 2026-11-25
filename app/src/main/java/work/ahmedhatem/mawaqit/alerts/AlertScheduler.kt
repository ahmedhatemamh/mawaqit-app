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
        val next = AlertPlanner.upcoming(
            rules, now, 1, settings.place.latitude, settings.place.longitude, settings.makruhMinutes, prayed,
        ).firstOrNull()
        if (next == null) {
            backend.cancel()
            return Plan.None
        }
        return Plan.Scheduled(next, backend.schedule(FiredAlert.of(next), next.at))
    }
}
