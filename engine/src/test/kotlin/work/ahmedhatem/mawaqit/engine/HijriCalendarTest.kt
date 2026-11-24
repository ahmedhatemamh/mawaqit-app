package work.ahmedhatem.mawaqit.engine

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HijriCalendarTest {
    private val calendar = HijriCalendar.bundled()

    private fun hijri(y: Int, m: Int, d: Int) = calendar.dateOf(LocalDate.of(y, m, d))

    @Test
    fun knownOfficialDates() {
        // The Dar al-Ifta page showed 13 Jumada al-Akhirah 1448 on 23 November 2026.
        assertEquals(HijriDate(1448, 6, 13, true), hijri(2026, 11, 23))
        // Ramadan 1446 began on 1 March 2025 (official statement: the crescent was sought on the evening of the 29th of
        // Sha'ban, Friday 28 February), so Sha'ban had 29 days.
        assertEquals(HijriDate(1446, 9, 1, true), hijri(2025, 3, 1))
        assertEquals(HijriDate(1446, 8, 29, true), hijri(2025, 2, 28))
        assertEquals(HijriDate(1437, 6, 1, true), hijri(2016, 3, 10))
    }

    @Test
    fun everyDayOfTheOfficialRangeIsConsecutiveAndCertain() {
        var date = LocalDate.of(2016, 3, 10)
        var previous = calendar.dateOf(date)
        assertEquals(1, previous.day)
        val last = LocalDate.of(2026, 11, 23)
        var months = 1
        while (date.isBefore(last)) {
            date = date.plusDays(1)
            val h = calendar.dateOf(date)
            assertTrue("$date -> $h should be certain", h.certain)
            if (h.day == 1) {
                assertTrue("$date: month must follow ${previous.month}/${previous.year}",
                    (h.year * 12 + h.month) == (previous.year * 12 + previous.month) + 1)
                assertTrue("$date: month ended after ${previous.day} days", previous.day in 29..30)
                months++
            } else {
                assertEquals("$date", previous.day + 1, h.day)
                assertEquals("$date", previous.month, h.month)
                assertEquals("$date", previous.year, h.year)
            }
            previous = h
        }
        assertEquals(133, months)
    }

    @Test
    fun theLastOfficialMonthIsCertainForItsFirst29DaysOnly() {
        val start = LocalDate.of(2026, 11, 11)  // 1 Jumada al-Akhirah 1448, no following start known yet
        for (day in 1..29) assertTrue("day $day", calendar.dateOf(start.plusDays(day - 1L)).certain)
        val thirtieth = calendar.dateOf(start.plusDays(29))  // the 30th day may already be 1 Rajab
        assertFalse(thirtieth.certain)
        assertTrue(thirtieth.day == 30 || thirtieth.day == 1)
    }

    private fun checkConsecutive(from: LocalDate, days: Int, expectCertain: Boolean) {
        var date = from
        var previous = calendar.dateOf(date)
        var monthLengths = 0
        repeat(days) {
            date = date.plusDays(1)
            val h = calendar.dateOf(date)
            assertTrue("$date -> $h day out of range", h.day in 1..30)
            assertEquals("$date -> $h certain", expectCertain, h.certain)
            if (h.day == 1) {
                assertEquals("$date month order", previous.year * 12 + previous.month + 1, h.year * 12 + h.month)
                assertTrue("$date: month ended after ${previous.day} days", previous.day in 29..30)
                monthLengths++
            } else {
                assertEquals("$date", previous.day + 1, h.day)
                assertEquals("$date", previous.year * 12 + previous.month, h.year * 12 + h.month)
            }
            previous = h
        }
        assertTrue("no month boundary found", monthLengths >= days / 31)
    }

    @Test
    fun futureDatesAreEstimatedConsecutiveAndNeverCertain() {
        checkConsecutive(LocalDate.of(2026, 12, 20), 3 * 365, expectCertain = false)
    }

    @Test
    fun farFutureAndFarPastWork() {
        checkConsecutive(LocalDate.of(2100, 1, 1), 400, expectCertain = false)
        checkConsecutive(LocalDate.of(1950, 1, 1), 400, expectCertain = false)
        checkConsecutive(LocalDate.of(2010, 1, 1), 2200, expectCertain = false)  // stops before the first official month (2016-03-10)
    }

    @Test
    fun estimatedMonthsAreNeverFlaggedCertainEvenRightAfterTheOfficialRange() {
        assertFalse(hijri(2026, 12, 11).certain)
        assertFalse(hijri(2027, 1, 15).certain)
        assertFalse(hijri(2016, 3, 9).certain)
    }

    @Test
    fun startOfMonthTellsWhetherItIsOfficial() {
        assertEquals(Pair(LocalDate.of(2026, 11, 11), true), calendar.startOfMonth(1448, 6))
        val estimated = calendar.startOfMonth(1448, 8)
        assertFalse(estimated.second)
        assertTrue(ChronoUnit.DAYS.between(LocalDate.of(2026, 11, 11), estimated.first) in 58..60)
        assertThrows(IllegalArgumentException::class.java) { calendar.startOfMonth(1448, 13) }
    }

    @Test
    fun anEstimateOf31DaysIsLimitedTo30() {
        val anchor = HijriMonthStart(1448, 6, LocalDate.of(2026, 11, 11))
        val anchorLunation = CrescentEstimator.lunationOfStart(anchor.start)
        // The stub makes the month after the anchor start 31 days later, then 30 days apart.
        val offsets = mapOf(0L to 0L, 1L to 31L, 2L to 61L, 3L to 91L, 4L to 121L, 5L to 151L)
        val stubbed = HijriCalendar(listOf(anchor)) { k ->
            anchor.start.plusDays(offsets[k - anchorLunation] ?: error("unexpected lunation $k"))
        }
        val first = stubbed.startOfMonth(1448, 7).first
        assertEquals(LocalDate.of(2026, 11, 11).plusDays(30), first)
        var date = LocalDate.of(2026, 11, 11)
        var previous = stubbed.dateOf(date)
        repeat(100) {
            date = date.plusDays(1)
            val h = stubbed.dateOf(date)
            if (h.day == 1) assertTrue("month ended after ${previous.day} days", previous.day in 29..30)
            else assertEquals(previous.day + 1, h.day)
            assertTrue(h.day in 1..30)
            previous = h
        }
    }

    @Test
    fun badOfficialDataIsRejected() {
        val a = HijriMonthStart(1448, 5, LocalDate.of(2026, 10, 12))
        val b = HijriMonthStart(1448, 6, LocalDate.of(2026, 11, 11))
        HijriCalendar(listOf(a, b))
        HijriCalendar(listOf(b, a))  // order does not matter
        assertThrows(IllegalArgumentException::class.java) { HijriCalendar(emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { HijriCalendar(listOf(a, a)) }
        assertThrows(IllegalArgumentException::class.java) { HijriCalendar(listOf(a, b.copy(start = LocalDate.of(2026, 11, 9)))) }  // 28 days
        assertThrows(IllegalArgumentException::class.java) { HijriCalendar(listOf(a, b.copy(start = LocalDate.of(2026, 11, 13)))) }  // 32 days
        assertThrows(IllegalArgumentException::class.java) { HijriMonthStart(1448, 13, LocalDate.of(2026, 11, 11)) }
        assertThrows(IllegalArgumentException::class.java) { HijriMonthStart(1448, 0, LocalDate.of(2026, 11, 11)) }
    }

    @Test
    fun gapsBetweenOfficialMonthsAreEstimatedAndNotCertain() {
        val a = HijriMonthStart(1448, 4, LocalDate.of(2026, 9, 13))
        val c = HijriMonthStart(1448, 6, LocalDate.of(2026, 11, 11))
        val gapped = HijriCalendar(listOf(a, c))
        assertTrue(gapped.dateOf(LocalDate.of(2026, 9, 20)).certain)
        val inGap = gapped.dateOf(LocalDate.of(2026, 10, 20))
        assertFalse(inGap.certain)
        assertEquals(1448, inGap.year)
        assertEquals(5, inGap.month)
    }
}
