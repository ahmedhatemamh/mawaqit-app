package work.ahmedhatem.mawaqit.search

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import work.ahmedhatem.mawaqit.engine.HijriCalendar

class DateSearchTest {
    private val calendar = HijriCalendar.bundled()
    private fun q(kind: CalendarKind, d: String, m: String, y: String) = DateSearch.resolve(DateQuery(kind, d, m, y), calendar)
    private fun greg(d: String, m: String, y: String) = q(CalendarKind.GREGORIAN, d, m, y)
    private fun hijri(d: String, m: String, y: String) = q(CalendarKind.HIJRI, d, m, y)
    private fun invalid(e: SearchError) = SearchResult.Invalid(e)

    @Test
    fun gregorianFullDate() = assertEquals(SearchResult.Day(LocalDate.of(2026, 11, 25)), greg("25", "11", "2026"))

    @Test
    fun gregorianMonthAndYear() = assertEquals(SearchResult.Month(YearMonth.of(2026, 3)), greg("", "3", "2026"))

    @Test
    fun gregorianYearOnlyGoesToJanuary() = assertEquals(SearchResult.Month(YearMonth.of(2030, 1)), greg("", "", "2030"))

    @Test
    fun arabicIndicAndPersianDigitsAreAccepted() {
        assertEquals(SearchResult.Day(LocalDate.of(2026, 11, 25)), greg("٢٥", "١١", "٢٠٢٦"))
        assertEquals(SearchResult.Day(LocalDate.of(2026, 11, 25)), greg("۲۵", "۱۱", "۲۰۲۶"))
        assertEquals(SearchResult.Day(LocalDate.of(2026, 11, 25)), greg(" 25 ", "١١", "2026"))
    }

    @Test
    fun leapDayOnlyInLeapYears() {
        assertEquals(SearchResult.Day(LocalDate.of(2028, 2, 29)), greg("29", "2", "2028"))
        assertEquals(invalid(SearchError.BAD_DAY), greg("29", "2", "2027"))
        assertEquals(invalid(SearchError.BAD_DAY), greg("31", "4", "2026"))
    }

    @Test
    fun rangeEdgesOfTheTable() {
        assertEquals(SearchResult.Day(LocalDate.of(1950, 1, 1)), greg("1", "1", "1950"))
        assertEquals(SearchResult.Day(LocalDate.of(2100, 12, 31)), greg("31", "12", "2100"))
        assertEquals(invalid(SearchError.YEAR_OUT_OF_RANGE), greg("", "", "1949"))
        assertEquals(invalid(SearchError.YEAR_OUT_OF_RANGE), greg("", "", "2101"))
    }

    @Test
    fun badInputIsRefusedWithTheRightReason() {
        assertEquals(invalid(SearchError.MISSING_YEAR), greg("", "", ""))
        assertEquals(invalid(SearchError.MISSING_YEAR), greg("1", "1", "   "))
        assertEquals(invalid(SearchError.DAY_WITHOUT_MONTH), greg("5", "", "2026"))
        assertEquals(invalid(SearchError.NOT_A_NUMBER), greg("", "", "20x6"))
        assertEquals(invalid(SearchError.NOT_A_NUMBER), greg("", "", "-2026"))
        assertEquals(invalid(SearchError.NOT_A_NUMBER), greg("", "", "2026.5"))
        assertEquals(invalid(SearchError.NOT_A_NUMBER), greg("", "", "99999999999"))
        assertEquals(invalid(SearchError.BAD_MONTH), greg("", "13", "2026"))
        assertEquals(invalid(SearchError.BAD_MONTH), greg("", "0", "2026"))
        assertEquals(invalid(SearchError.BAD_DAY), greg("0", "1", "2026"))
    }

    @Test
    fun hijriFullDateMatchesTheCalendar() {
        // 1 Ramadan 1446 = 1 March 2025 (official month start).
        assertEquals(SearchResult.Day(LocalDate.of(2025, 3, 1)), hijri("1", "9", "1446"))
        assertEquals(SearchResult.Day(LocalDate.of(2025, 3, 2)), hijri("٢", "٩", "١٤٤٦"))
    }

    @Test
    fun hijriMonthAndYearOnly() {
        assertEquals(SearchResult.Month(YearMonth.of(2025, 3)), hijri("", "9", "1446"))
        val firstOfYear = calendar.startOfMonth(1448, 1).first
        assertEquals(SearchResult.Month(YearMonth.from(firstOfYear)), hijri("", "", "1448"))
    }

    @Test
    fun hijriDayBeyondTheMonthLengthIsRefused() {
        val start = calendar.startOfMonth(1446, 9).first
        val length = (calendar.startOfMonth(1446, 10).first.toEpochDay() - start.toEpochDay()).toInt()
        assertEquals(SearchResult.Day(start.plusDays(length - 1L)), hijri(length.toString(), "9", "1446"))
        assertEquals(invalid(SearchError.BAD_DAY), hijri((length + 1).toString(), "9", "1446"))
        assertEquals(invalid(SearchError.BAD_DAY), hijri("31", "9", "1446"))
    }

    @Test
    fun hijriRange() {
        assertEquals(invalid(SearchError.YEAR_OUT_OF_RANGE), hijri("", "", "1299"))
        assertEquals(invalid(SearchError.YEAR_OUT_OF_RANGE), hijri("", "", "1601"))
        assertEquals(invalid(SearchError.YEAR_OUT_OF_RANGE), hijri("", "", "1360")) // starts before 1950
        assertEquals(invalid(SearchError.YEAR_OUT_OF_RANGE), hijri("", "", "1530")) // starts after 2100
    }

    @Test
    fun everyDayOfTwoYearsRoundTripsThroughTheHijriCalendar() {
        var date = LocalDate.of(2025, 1, 1)
        while (date.year < 2027) {
            val h = calendar.dateOf(date)
            val found = hijri(h.day.toString(), h.month.toString(), h.year.toString())
            assertEquals("for $date", SearchResult.Day(date), found)
            date = date.plusDays(1)
        }
    }

    @Test
    fun numberParserRejectsEmptyAndLongText() {
        assertNull(DateSearch.number(""))
        assertNull(DateSearch.number("1234567"))
        assertEquals(0, DateSearch.number("0"))
    }
}
