package work.ahmedhatem.mawaqit.year

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import work.ahmedhatem.mawaqit.engine.Cities
import work.ahmedhatem.mawaqit.engine.HijriCalendar
import work.ahmedhatem.mawaqit.engine.Prayer
import work.ahmedhatem.mawaqit.engine.PrayerCalculator
import work.ahmedhatem.mawaqit.settings.Place

class YearModelTest {
    private val calendar = HijriCalendar.bundled()
    private val cairo = Place.DEFAULT

    private fun month(year: Int, month: Int, place: Place = cairo) = YearModel.build(YearMonth.of(year, month), place, calendar)

    @Test
    fun everyDayOfEveryMonthOf2026HasSixOrderedTimes() {
        for (m in 1..12) {
            val t = month(2026, m)
            assertEquals(YearMonth.of(2026, m).lengthOfMonth(), t.rows.size)
            for (row in t.rows) {
                assertEquals(6, row.minutes.size)
                assertEquals("${row.date}", row.minutes.sorted(), row.minutes)
                assertEquals(6, row.minutes.toSet().size)
                assertTrue(row.minutes.all { it in 0 until 24 * 60 })
            }
        }
    }

    @Test
    fun datesAreConsecutiveAndHijriDatesNeverGoBack() {
        var previous: LocalDate? = null
        var previousHijri = 0
        for (m in 1..12) for (row in month(2026, m).rows) {
            if (previous != null) assertEquals(previous.plusDays(1), row.date)
            previous = row.date
            val key = row.hijri.year * 10000 + row.hijri.month * 100 + row.hijri.day
            assertTrue("${row.date}", key > previousHijri)
            previousHijri = key
        }
    }

    @Test
    fun rowsAgreeWithTheCalculatorAndTheCalendar() {
        val aswan = Place.ofCity("أسوان")!!
        for (place in listOf(cairo, aswan)) {
            val t = month(2026, 7, place)
            for (row in t.rows) {
                val times = PrayerCalculator.timesFor(row.date, place.latitude, place.longitude)
                for (p in Prayer.entries) assertEquals(times[p], row.minute(p))
                assertEquals(calendar.dateOf(row.date), row.hijri)
            }
        }
    }

    @Test
    fun theKnownDateAppearsWithItsOfficialHijriDate() {
        val row = month(2026, 11).rows.first { it.date == LocalDate.of(2026, 11, 23) }
        assertEquals(13, row.hijri.day)
        assertEquals(6, row.hijri.month)
        assertEquals(1448, row.hijri.year)
        assertTrue(row.hijri.certain)
    }

    @Test
    fun hijriMonthsOfTheHeader() {
        val t = month(2025, 3)       // Ramadan 1446 starts on 1 March 2025
        assertEquals(listOf(1446 to 9, 1446 to 10), t.hijriMonths)   // Shawwal starts on 30 March
        assertEquals(1, t.hijriMonths.count { it == 1446 to 9 })
    }

    @Test
    fun summerTimeShowsAsAOneHourJumpAndUnconfirmedYearsAreFlagged() {
        val april = month(2026, 4)
        val before = april.rows.first { it.date == LocalDate.of(2026, 4, 23) }
        val after = april.rows.first { it.date == LocalDate.of(2026, 4, 24) }
        // Dhuhr moves by about an hour (plus the usual daily drift of a fraction of a minute)
        val jump = after.minute(Prayer.DHUHR) - before.minute(Prayer.DHUHR)
        assertTrue("$jump", jump in 59..61)
        assertTrue(april.dstConfirmed)
        assertFalse(month(2031, 6).dstConfirmed)
        assertTrue(month(2022, 6).dstConfirmed)
    }

    @Test
    fun anyOtherCityGivesDifferentTimes() {
        val first = month(2026, 5).rows.first().minutes
        val others = Cities.all.map { month(2026, 5, Place.ofCity(it.name)!!).rows.first().minutes }
        assertTrue(others.toSet().size > 10)
        assertTrue(others.contains(first))
    }

    @Test
    fun theSupportedRangeIsBuiltAtBothEnds() {
        assertEquals(31, month(1950, 1).rows.size)
        assertEquals(31, month(2100, 12).rows.size)
        assertEquals(29, month(2024, 2).rows.size)
        assertEquals(28, month(2026, 2).rows.size)
        assertThrows(IllegalArgumentException::class.java) { month(1949, 12) }
        assertThrows(IllegalArgumentException::class.java) { month(2101, 1) }
    }

    @Test
    fun navigationMovesByMonthsAndStopsAtTheEnds() {
        assertEquals(YearMonth.of(2027, 1), YearModel.shift(YearMonth.of(2026, 12), 1))
        assertEquals(YearMonth.of(2025, 12), YearModel.shift(YearMonth.of(2026, 1), -1))
        assertEquals(YearMonth.of(2027, 6), YearModel.shift(YearMonth.of(2026, 6), 12))
        assertEquals(YearMonth.of(1950, 1), YearModel.shift(YearMonth.of(1950, 1), -1))
        assertEquals(YearMonth.of(2100, 12), YearModel.shift(YearMonth.of(2100, 12), 1))
        assertEquals(YearMonth.of(2100, 12), YearModel.shift(YearMonth.of(2099, 6), 100))
    }

    @Test
    fun aWholeYearIsFastEnoughForThePhone() {
        val start = System.nanoTime()
        for (m in 1..12) month(2027, m)
        val ms = (System.nanoTime() - start) / 1_000_000
        assertTrue("a year took $ms ms", ms < 3000)
    }
}
