package work.ahmedhatem.mawaqit.engine

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Egypt's civil clock, from the app's own summer-time rule ([EgyptDst]) and not from the phone's time zone. */
object EgyptClock {
    fun localDateTime(instant: Instant): LocalDateTime {
        // The offset depends on the local date, which depends on the offset: try the standard offset first, then
        // the offset of the date it gave. The rule changes at midnight, so this settles in one more step.
        val first = LocalDateTime.ofInstant(instant, ZoneOffset.ofHours(2)).toLocalDate()
        val offset = EgyptDst.offsetFor(first).hours
        val local = LocalDateTime.ofInstant(instant, ZoneOffset.ofHours(offset))
        return if (local.toLocalDate() == first) local
        else LocalDateTime.ofInstant(instant, ZoneOffset.ofHours(EgyptDst.offsetFor(local.toLocalDate()).hours))
    }
}

/** The next of the six daily times after a moment. */
data class NextTime(val prayer: Prayer, val date: LocalDate, val minute: Int, val secondsUntil: Int) {
    /** Whole minutes left, rounded up, so the display never shows 0 before the time itself. */
    val minutesUntil: Int get() = (secondsUntil + 59) / 60
}

object Schedule {
    /**
     * The first daily time that is still ahead of [now] (Egypt local time). A time counts as reached from the start
     * of its minute, because the tables have no seconds. After the last time of the day it is the next day's first.
     */
    fun next(now: LocalDateTime, latitude: Double, longitude: Double): NextTime {
        val nowSeconds = now.toLocalTime().toSecondOfDay()
        val today = PrayerCalculator.timesFor(now.toLocalDate(), latitude, longitude)
        for (prayer in Prayer.entries) {
            val at = today[prayer] * 60
            if (at > nowSeconds) return NextTime(prayer, now.toLocalDate(), today[prayer], at - nowSeconds)
        }
        val tomorrow = now.toLocalDate().plusDays(1)
        val times = PrayerCalculator.timesFor(tomorrow, latitude, longitude)
        val first = Prayer.entries.first()
        return NextTime(first, tomorrow, times[first], 24 * 3600 - nowSeconds + times[first] * 60)
    }
}
