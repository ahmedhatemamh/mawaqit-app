package work.ahmedhatem.mawaqit.year

import java.time.LocalDate
import java.time.YearMonth
import work.ahmedhatem.mawaqit.engine.HijriCalendar
import work.ahmedhatem.mawaqit.engine.HijriDate
import work.ahmedhatem.mawaqit.engine.Prayer
import work.ahmedhatem.mawaqit.engine.PrayerCalculator
import work.ahmedhatem.mawaqit.settings.Place

/** One day of the table: the date in both calendars and its six times (minutes after midnight, in [Prayer] order). */
data class DayRow(val date: LocalDate, val hijri: HijriDate, val minutes: List<Int>) {
    fun minute(prayer: Prayer): Int = minutes[prayer.ordinal]
}

/** A Gregorian month of the table. */
data class MonthTable(val month: YearMonth, val rows: List<DayRow>, val dstConfirmed: Boolean) {
    /** Hijri months the table touches, in order, as (year, month), for the header. */
    val hijriMonths: List<Pair<Int, Int>> = rows.map { it.hijri.year to it.hijri.month }.distinct()
}

object YearModel {
    const val FIRST_YEAR = 1950
    const val LAST_YEAR = 2100

    /** Moves by [delta] months, staying inside the supported years. */
    fun shift(month: YearMonth, delta: Int): YearMonth =
        month.plusMonths(delta.toLong()).let { clamp(it) }

    fun clamp(month: YearMonth): YearMonth = when {
        month.year < FIRST_YEAR -> YearMonth.of(FIRST_YEAR, 1)
        month.year > LAST_YEAR -> YearMonth.of(LAST_YEAR, 12)
        else -> month
    }

    fun build(month: YearMonth, place: Place, calendar: HijriCalendar): MonthTable {
        require(month.year in FIRST_YEAR..LAST_YEAR) { "year outside $FIRST_YEAR..$LAST_YEAR: ${month.year}" }
        var confirmed = true
        val rows = (1..month.lengthOfMonth()).map { day ->
            val date = month.atDay(day)
            val times = PrayerCalculator.timesFor(date, place.latitude, place.longitude)
            confirmed = confirmed && times.dstConfirmed
            DayRow(date, calendar.dateOf(date), times.minutes)
        }
        return MonthTable(month, rows, confirmed)
    }
}
