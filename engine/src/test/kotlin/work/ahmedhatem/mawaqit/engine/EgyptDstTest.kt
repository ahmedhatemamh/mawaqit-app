package work.ahmedhatem.mawaqit.engine

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EgyptDstTest {
    private fun offset(y: Int, m: Int, d: Int) = EgyptDst.offsetFor(LocalDate.of(y, m, d))

    @Test
    fun ruleChangesOnTheLastFridayOfAprilAndEndsAfterTheLastThursdayOfOctober() {
        // (last day without summer time, first summer day, last summer day, first day without)
        val years = mapOf(
            2023 to listOf(LocalDate.of(2023, 4, 27), LocalDate.of(2023, 4, 28), LocalDate.of(2023, 10, 26), LocalDate.of(2023, 10, 27)),
            2024 to listOf(LocalDate.of(2024, 4, 25), LocalDate.of(2024, 4, 26), LocalDate.of(2024, 10, 31), LocalDate.of(2024, 11, 1)),
            2025 to listOf(LocalDate.of(2025, 4, 24), LocalDate.of(2025, 4, 25), LocalDate.of(2025, 10, 30), LocalDate.of(2025, 10, 31)),
            2026 to listOf(LocalDate.of(2026, 4, 23), LocalDate.of(2026, 4, 24), LocalDate.of(2026, 10, 29), LocalDate.of(2026, 10, 30)),
        )
        for ((year, d) in years) {
            assertEquals("$year before start", 2, EgyptDst.offsetFor(d[0]).hours)
            assertEquals("$year first summer day", 3, EgyptDst.offsetFor(d[1]).hours)
            assertEquals("$year last summer day", 3, EgyptDst.offsetFor(d[2]).hours)
            assertEquals("$year after end", 2, EgyptDst.offsetFor(d[3]).hours)
        }
    }

    @Test
    fun noSummerTimeFrom2015To2022() {
        for (year in 2015..2022) {
            for (month in 1..12) {
                val o = offset(year, month, 15)
                assertEquals("$year-$month", 2, o.hours)
                assertTrue("$year-$month", o.confirmed)
            }
        }
    }

    @Test
    fun yearsOutsideTheConfirmedRangeUseTheNearestRuleAndAreMarkedUnconfirmed() {
        assertFalse(offset(2014, 12, 31).confirmed)
        assertTrue(offset(2015, 1, 1).confirmed)
        assertTrue(offset(2026, 12, 31).confirmed)
        assertFalse(offset(2027, 1, 1).confirmed)
        assertEquals(2, offset(2010, 7, 1).hours)   // nearest rule is 2015: none
        assertEquals(3, offset(2027, 7, 1).hours)   // nearest rule is 2026: summer time
        assertEquals(2, offset(2027, 1, 1).hours)
        assertEquals(3, offset(2100, 7, 1).hours)
        assertFalse(offset(2100, 7, 1).confirmed)
    }

    @Test
    fun summerWindowIsNeverEmptyOrWholeYear() {
        for (year in 2023..2060) {
            val days = generateSequence(LocalDate.of(year, 1, 1)) { it.plusDays(1) }.takeWhile { it.year == year }.toList()
            val summer = days.count { EgyptDst.offsetFor(it).hours == 3 }
            assertTrue("$year has $summer summer days", summer in 180..190)
        }
    }
}
