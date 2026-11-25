package work.ahmedhatem.mawaqit.search

import java.time.DateTimeException
import java.time.LocalDate
import java.time.YearMonth
import work.ahmedhatem.mawaqit.engine.HijriCalendar
import work.ahmedhatem.mawaqit.year.YearModel

/** Which calendar the typed numbers belong to. */
enum class CalendarKind { GREGORIAN, HIJRI }

/** What was typed, as text (the fields are free text so any digits can be typed). */
data class DateQuery(val kind: CalendarKind, val day: String, val month: String, val year: String)

enum class SearchError {
    /** Nothing typed, or the year is missing. */
    MISSING_YEAR,
    /** A day without a month. */
    DAY_WITHOUT_MONTH,
    /** Something that is not a whole number. */
    NOT_A_NUMBER,
    BAD_MONTH,
    /** A day that does not exist in that month. */
    BAD_DAY,
    /** A year the app does not cover (Gregorian 1950 to 2100, or the Hijri years inside them). */
    YEAR_OUT_OF_RANGE,
}

sealed interface SearchResult {
    /** One day: show its times. */
    data class Day(val date: LocalDate) : SearchResult

    /** A month (or the first month of a year) of the year table. */
    data class Month(val month: YearMonth) : SearchResult

    data class Invalid(val error: SearchError) : SearchResult
}

/**
 * Turns what the user typed into a day or a month of the table (spec 3.1):
 * - year, month and day: that day;
 * - year and month: that month of the table (a Hijri month shows the Gregorian month where it starts);
 * - year only: the first month of that year (January, or the month where 1 Muharram falls).
 */
object DateSearch {
    fun resolve(query: DateQuery, calendar: HijriCalendar): SearchResult {
        val day = query.day.trim()
        val month = query.month.trim()
        val year = query.year.trim()
        if (year.isEmpty()) return SearchResult.Invalid(SearchError.MISSING_YEAR)
        if (day.isNotEmpty() && month.isEmpty()) return SearchResult.Invalid(SearchError.DAY_WITHOUT_MONTH)
        val y = number(year) ?: return SearchResult.Invalid(SearchError.NOT_A_NUMBER)
        val m = if (month.isEmpty()) null else number(month) ?: return SearchResult.Invalid(SearchError.NOT_A_NUMBER)
        val d = if (day.isEmpty()) null else number(day) ?: return SearchResult.Invalid(SearchError.NOT_A_NUMBER)
        if (m != null && m !in 1..12) return SearchResult.Invalid(SearchError.BAD_MONTH)
        return when (query.kind) {
            CalendarKind.GREGORIAN -> gregorian(y, m, d)
            CalendarKind.HIJRI -> hijri(y, m, d, calendar)
        }
    }

    /** Whole numbers only, in western or Arabic-Indic digits (٠١٢٣٤٥٦٧٨٩ and ۰۱۲۳۴۵۶۷۸۹). Null for anything else. */
    internal fun number(text: String): Int? {
        if (text.isEmpty() || text.length > 6) return null
        var value = 0
        for (c in text) {
            val digit = when (c) {
                in '0'..'9' -> c - '0'
                in '٠'..'٩' -> c - '٠'
                in '۰'..'۹' -> c - '۰'
                else -> return null
            }
            value = value * 10 + digit
        }
        return value
    }

    private fun gregorian(y: Int, m: Int?, d: Int?): SearchResult {
        if (y !in YearModel.FIRST_YEAR..YearModel.LAST_YEAR) return SearchResult.Invalid(SearchError.YEAR_OUT_OF_RANGE)
        if (m == null) return SearchResult.Month(YearMonth.of(y, 1))
        if (d == null) return SearchResult.Month(YearMonth.of(y, m))
        return try {
            SearchResult.Day(LocalDate.of(y, m, d))
        } catch (e: DateTimeException) {
            SearchResult.Invalid(SearchError.BAD_DAY)
        }
    }

    private fun hijri(y: Int, m: Int?, d: Int?, calendar: HijriCalendar): SearchResult {
        // The Hijri years whose first day is inside the table's years: about 1370 to 1520.
        if (y !in 1300..1600) return SearchResult.Invalid(SearchError.YEAR_OUT_OF_RANGE)
        val start = calendar.startOfMonth(y, m ?: 1).first
        if (d == null) {
            if (start.year !in YearModel.FIRST_YEAR..YearModel.LAST_YEAR) return SearchResult.Invalid(SearchError.YEAR_OUT_OF_RANGE)
            return SearchResult.Month(YearMonth.from(start))
        }
        // The month ends where the next one starts, so its length is the distance between the two starts.
        val next = if (m == 12) calendar.startOfMonth(y + 1, 1).first else calendar.startOfMonth(y, m!! + 1).first
        val length = (next.toEpochDay() - start.toEpochDay()).toInt()
        if (d !in 1..length) return SearchResult.Invalid(SearchError.BAD_DAY)
        val date = start.plusDays((d - 1).toLong())
        if (date.year !in YearModel.FIRST_YEAR..YearModel.LAST_YEAR) return SearchResult.Invalid(SearchError.YEAR_OUT_OF_RANGE)
        return SearchResult.Day(date)
    }
}
