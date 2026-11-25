package work.ahmedhatem.mawaqit.today

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import work.ahmedhatem.mawaqit.engine.EgyptClock
import work.ahmedhatem.mawaqit.engine.HijriCalendar
import work.ahmedhatem.mawaqit.engine.HijriDate
import work.ahmedhatem.mawaqit.engine.NextTime
import work.ahmedhatem.mawaqit.engine.Prayer
import work.ahmedhatem.mawaqit.engine.PrayerCalculator
import work.ahmedhatem.mawaqit.engine.Schedule
import work.ahmedhatem.mawaqit.settings.Settings

/** One row of the day: a daily time in minutes after midnight. [isNext] marks the one the countdown is for. */
data class TimeRow(val prayer: Prayer, val minute: Int, val isNext: Boolean)

/** Everything the "today" screen shows, computed from the instant and the settings. */
data class TodayModel(
    val date: LocalDate,
    val hijri: HijriDate,
    val rows: List<TimeRow>,
    val next: NextTime,
    /** The end of the period when prayer is discouraged after sunrise, in minutes after midnight. */
    val makruhEndsAt: Int,
    val dstConfirmed: Boolean,
    /** The label of the place the times are for. */
    val placeLabel: String,
) {
    companion object {
        fun build(instant: Instant, settings: Settings, calendar: HijriCalendar): TodayModel =
            build(EgyptClock.localDateTime(instant), settings, calendar)

        fun build(now: LocalDateTime, settings: Settings, calendar: HijriCalendar): TodayModel {
            val place = settings.place
            val times = PrayerCalculator.timesFor(now.toLocalDate(), place.latitude, place.longitude)
            val next = Schedule.next(now, place.latitude, place.longitude)
            // The highlighted row is the next time of today; after the last time of the day it is tomorrow's first,
            // which is not on this list, so nothing is highlighted.
            val nextToday = next.takeIf { it.date == now.toLocalDate() }?.prayer
            val rows = Prayer.entries.map { TimeRow(it, times[it], it == nextToday) }
            return TodayModel(
                date = now.toLocalDate(),
                hijri = calendar.dateOf(now.toLocalDate()),
                rows = rows,
                next = next,
                makruhEndsAt = times[Prayer.SUNRISE] + settings.makruhMinutes,
                dstConfirmed = times.dstConfirmed,
                placeLabel = place.label,
            )
        }
    }
}
