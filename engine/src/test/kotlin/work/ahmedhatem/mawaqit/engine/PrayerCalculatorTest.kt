package work.ahmedhatem.mawaqit.engine

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerCalculatorTest {
    private val cairo = Cities.all.first { it.name == "القاهرة" }

    @Test
    fun cairoOnTwoKnownDays() {
        // Whole minutes from the research code. The official table agrees on the first day; on 1 July its Fajr is
        // 251, one of the known one-minute differences (see research/esa-formula/README.md).
        val winter = PrayerCalculator.timesFor(LocalDate.of(2026, 1, 1), cairo)
        assertEquals(listOf(318, 411, 718, 887, 1026, 1109), winter.minutes)
        val summer = PrayerCalculator.timesFor(LocalDate.of(2026, 7, 1), cairo)
        assertEquals(listOf(252, 357, 779, 995, 1200, 1293), summer.minutes)
    }

    @Test
    fun timesAreInDailyOrderAndInsideTheDayForEveryCityAndSeason() {
        val dates = generateSequence(LocalDate.of(1950, 1, 1)) { it.plusDays(37) }.takeWhile { it.year <= 2100 }.toList()
        for (city in Cities.all) {
            for (date in dates) {
                val m = PrayerCalculator.timesFor(date, city).exactMinutes
                for (k in 1 until 6) assertTrue("${city.name} $date: ${m}", m[k] > m[k - 1])
                assertTrue("${city.name} $date: ${m}", m.first() > 0 && m.last() < 1440)
            }
        }
    }

    @Test
    fun dayTimesRoundToTheNearestMinute() {
        val exact = listOf(0.49, 0.5, 719.5, 100.51, 1000.0, 1439.4999)
        val day = DayTimes(LocalDate.of(2026, 1, 1), exact, 2, true)
        assertEquals(listOf(0, 1, 720, 101, 1000, 1439), day.minutes)
        assertEquals(720, day[Prayer.DHUHR])
    }

    @Test
    fun summerDaysAreMarkedAndShiftedByOneHour() {
        val winter = PrayerCalculator.timesFor(LocalDate.of(2026, 4, 23), cairo)
        val summer = PrayerCalculator.timesFor(LocalDate.of(2026, 4, 24), cairo)
        assertEquals(2, winter.utcOffsetHours)
        assertEquals(3, summer.utcOffsetHours)
        assertTrue(summer.dstConfirmed)
        // Consecutive days differ by about a minute, so the clock change dominates.
        assertTrue(summer[Prayer.DHUHR] - winter[Prayer.DHUHR] in 55..65)
        assertFalse(PrayerCalculator.timesFor(LocalDate.of(2027, 7, 1), cairo).dstConfirmed)
    }

    @Test
    fun coordinatesOutsideEgyptAreRejected() {
        val day = LocalDate.of(2026, 1, 1)
        assertThrows(IllegalArgumentException::class.java) { PrayerCalculator.timesFor(day, 21.4225, 39.8262) }  // Mecca
        assertThrows(IllegalArgumentException::class.java) { PrayerCalculator.timesFor(day, 0.0, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { PrayerCalculator.timesFor(day, Double.NaN, 31.0) }
        assertThrows(IllegalArgumentException::class.java) { PrayerCalculator.timesFor(day, 30.0, Double.POSITIVE_INFINITY) }
    }

    @Test
    fun theBorderValuesOfTheAcceptedAreaAreAccepted() {
        val day = LocalDate.of(2026, 6, 21)
        PrayerCalculator.timesFor(day, 22.0, 25.0)
        PrayerCalculator.timesFor(day, 31.9, 36.9)
    }

    @Test
    fun cityListIsCompleteAndInsideEgypt() {
        assertEquals(78, Cities.all.size)
        assertEquals(78, Cities.all.map { it.name }.toSet().size)
        for (c in Cities.all) {
            assertTrue(c.name, c.latitude in 21.9..31.9 && c.longitude in 24.9..36.9)
        }
    }
}
