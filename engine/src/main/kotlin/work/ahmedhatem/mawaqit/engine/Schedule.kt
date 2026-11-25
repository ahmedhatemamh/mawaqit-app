package work.ahmedhatem.mawaqit.engine

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

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

    /** The instant of a local time, read with the offset of its own date (the hour that repeats in October is its first). */
    fun toInstant(local: LocalDateTime): Instant =
        local.toInstant(ZoneOffset.ofHours(EgyptDst.offsetFor(local.toLocalDate()).hours))
}

/** The next of the six daily times after a moment. */
data class NextTime(val prayer: Prayer, val date: LocalDate, val minute: Int, val secondsUntil: Int) {
    /** Whole minutes left, rounded up, so the display never shows 0 before the time itself. */
    val minutesUntil: Int get() = (secondsUntil + 59) / 60
}

object Schedule {
    /** [next] for a local time, read with the rule of its date (for tests and for callers that only have local time). */
    fun next(now: LocalDateTime, latitude: Double, longitude: Double): NextTime =
        next(EgyptClock.toInstant(now), latitude, longitude)

    /**
     * The first daily time that is still ahead of [instant]. A time counts as reached from the start of its minute,
     * because the tables have no seconds. After the last time of the day it is the next day's first.
     *
     * The wait is the real time between the two instants, so it stays right when the clock changes in between
     * (the night summer time starts or ends).
     */
    fun next(instant: Instant, latitude: Double, longitude: Double): NextTime {
        val now = EgyptClock.localDateTime(instant)
        val nowSeconds = now.toLocalTime().toSecondOfDay()
        val today = PrayerCalculator.timesFor(now.toLocalDate(), latitude, longitude)
        for (prayer in Prayer.entries) {
            if (today[prayer] * 60 > nowSeconds) return make(prayer, now.toLocalDate(), today[prayer], instant)
        }
        val tomorrow = now.toLocalDate().plusDays(1)
        val first = Prayer.entries.first()
        return make(first, tomorrow, PrayerCalculator.timesFor(tomorrow, latitude, longitude)[first], instant)
    }

    private fun make(prayer: Prayer, date: LocalDate, minute: Int, now: Instant): NextTime {
        val target = EgyptClock.toInstant(date.atStartOfDay().plusMinutes(minute.toLong()))
        val seconds = Duration.between(now.truncatedTo(ChronoUnit.SECONDS), target).seconds
        return NextTime(prayer, date, minute, seconds.toInt())
    }
}
