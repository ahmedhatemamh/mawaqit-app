package work.ahmedhatem.mawaqit.engine

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Egypt's clock offset from UTC for a day, and whether the rule for that year is confirmed. */
data class UtcOffset(val hours: Int, val confirmed: Boolean)

/**
 * Egypt's summer-time rule, kept inside the app so the answer is the same on every phone, whatever its
 * time-zone data says.
 *
 * Confirmed years are [FIRST_CONFIRMED_YEAR]..[LAST_CONFIRMED_YEAR] (checked against the IANA tz database on
 * 2026-11-24, which follows the official decrees):
 * - 2015 to 2022: no summer time.
 * - 2023 onward: UTC+3 from the last Friday of April to the last Thursday of October, both inclusive.
 *
 * Any other year gets the rule of the nearest confirmed year, marked as not confirmed. Moving
 * [LAST_CONFIRMED_YEAR] forward is a yearly manual step after the rule for that year is announced.
 */
object EgyptDst {
    const val FIRST_CONFIRMED_YEAR = 2015
    const val LAST_CONFIRMED_YEAR = 2026
    private const val SUMMER_RULE_STARTS = 2023

    fun offsetFor(date: LocalDate): UtcOffset {
        val confirmed = date.year in FIRST_CONFIRMED_YEAR..LAST_CONFIRMED_YEAR
        val ruleYear = date.year.coerceIn(FIRST_CONFIRMED_YEAR, LAST_CONFIRMED_YEAR)
        val summerRule = ruleYear >= SUMMER_RULE_STARTS
        if (!summerRule) return UtcOffset(2, confirmed)
        val start = lastDay(date.year, 4, DayOfWeek.FRIDAY)
        val end = lastDay(date.year, 10, DayOfWeek.THURSDAY)
        return UtcOffset(if (date >= start && date <= end) 3 else 2, confirmed)
    }

    private fun lastDay(year: Int, month: Int, day: DayOfWeek): LocalDate =
        LocalDate.of(year, month, 1).with(TemporalAdjusters.lastInMonth(day))
}
