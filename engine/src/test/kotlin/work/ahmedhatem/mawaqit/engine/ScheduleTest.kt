package work.ahmedhatem.mawaqit.engine

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleTest {
    private val cairo = CityFinder.DEFAULT
    private fun next(now: LocalDateTime) = Schedule.next(now, cairo.latitude, cairo.longitude)
    private fun times(date: LocalDate) = PrayerCalculator.timesFor(date, cairo)

    @Test
    fun beforeFajrTheNextIsFajrOfTheSameDay() {
        val day = LocalDate.of(2026, 11, 25)
        val fajr = times(day)[Prayer.FAJR]
        val n = next(day.atStartOfDay().plusMinutes(10))
        assertEquals(Prayer.FAJR, n.prayer)
        assertEquals(day, n.date)
        assertEquals(fajr, n.minute)
        assertEquals(fajr * 60 - 600, n.secondsUntil)
    }

    @Test
    fun everyTimeIsReachedFromTheStartOfItsMinute() {
        val day = LocalDate.of(2026, 11, 25)
        val t = times(day)
        for (prayer in Prayer.entries) {
            val atMinute = day.atStartOfDay().plusMinutes(t[prayer].toLong())
            // one second before: this one is next, with one second left
            val before = next(atMinute.minusSeconds(1))
            assertEquals(prayer, before.prayer)
            assertEquals(1, before.secondsUntil)
            assertEquals(1, before.minutesUntil)
            // at the first second of the minute and at its last second: the following one
            for (now in listOf(atMinute, atMinute.plusSeconds(59))) {
                val after = next(now)
                assertTrue("$prayer $now", after.prayer != prayer || after.date != day)
            }
        }
    }

    @Test
    fun afterIshaTheNextIsTomorrowsFajr() {
        val day = LocalDate.of(2026, 11, 25)
        val isha = times(day)[Prayer.ISHA]
        val n = next(day.atStartOfDay().plusMinutes(isha.toLong()))
        assertEquals(Prayer.FAJR, n.prayer)
        assertEquals(day.plusDays(1), n.date)
        assertEquals(times(day.plusDays(1))[Prayer.FAJR], n.minute)
        val expected = 24 * 3600 - isha * 60 + n.minute * 60
        assertEquals(expected, n.secondsUntil)
        val lateNight = next(day.atTime(23, 59, 30))
        assertEquals(Prayer.FAJR, lateNight.prayer)
        assertEquals(day.plusDays(1), lateNight.date)
        assertEquals(30 + n.minute * 60, lateNight.secondsUntil)
    }

    @Test
    fun midnightSwitchesToTheNewDayWithItsOwnTimes() {
        val n = next(LocalDateTime.of(2026, 11, 26, 0, 0, 0))
        assertEquals(LocalDate.of(2026, 11, 26), n.date)
        assertEquals(Prayer.FAJR, n.prayer)
    }

    @Test
    fun outsideEgyptIsRefused() {
        assertThrows(IllegalArgumentException::class.java) {
            Schedule.next(LocalDateTime.of(2026, 11, 25, 12, 0), 51.5, -0.1)
        }
    }

    @Test
    fun clockFollowsTheAppsOwnSummerTimeRule() {
        // winter: UTC+2
        assertEquals(LocalDateTime.of(2026, 1, 15, 14, 0), EgyptClock.localDateTime(Instant.parse("2026-01-15T12:00:00Z")))
        // summer: UTC+3 (the last Friday of April 2026 is the 24th, so 24 April is already summer)
        assertEquals(LocalDateTime.of(2026, 7, 1, 15, 0), EgyptClock.localDateTime(Instant.parse("2026-07-01T12:00:00Z")))
        // 2022 had no summer time
        assertEquals(LocalDateTime.of(2022, 7, 1, 14, 0), EgyptClock.localDateTime(Instant.parse("2022-07-01T12:00:00Z")))
    }

    @Test
    fun clockAtTheSummerTimeBoundaries() {
        // Summer 2026 is the whole of Friday 24 April to Thursday 29 October. The clock jumps forward at midnight
        // going into the 24th (00:00 becomes 01:00) and back at midnight going into the 30th (00:00 becomes 23:00).
        assertEquals(LocalDateTime.of(2026, 4, 23, 23, 59, 59), EgyptClock.localDateTime(Instant.parse("2026-04-23T21:59:59Z")))
        assertEquals(LocalDateTime.of(2026, 4, 24, 1, 0), EgyptClock.localDateTime(Instant.parse("2026-04-23T22:00:00Z")))
        assertEquals(LocalDateTime.of(2026, 10, 29, 23, 59, 59), EgyptClock.localDateTime(Instant.parse("2026-10-29T20:59:59Z")))
        assertEquals(LocalDateTime.of(2026, 10, 29, 23, 0), EgyptClock.localDateTime(Instant.parse("2026-10-29T21:00:00Z")))  // the hour repeats
        assertEquals(LocalDateTime.of(2026, 10, 30, 0, 0), EgyptClock.localDateTime(Instant.parse("2026-10-29T22:00:00Z")))
    }

    @Test
    fun nearestCity() {
        assertEquals("القاهرة", CityFinder.nearest(cairo.latitude + 0.001, cairo.longitude).name)
        val aswan = CityFinder.byName("أسوان")!!
        assertEquals("أسوان", CityFinder.nearest(aswan.latitude, aswan.longitude).name)
        assertEquals(78, Cities.all.size)
        for (c in Cities.all) assertEquals(c.name, CityFinder.nearest(c.latitude, c.longitude).name)
        assertNull(CityFinder.byName("لندن"))
        assertNotNull(CityFinder.byName("الأسكندرية"))
    }

    @Test
    fun insideEgypt() {
        assertTrue(PrayerCalculator.isInsideEgypt(30.0, 31.0))
        assertFalse(PrayerCalculator.isInsideEgypt(51.5, -0.1))
        assertFalse(PrayerCalculator.isInsideEgypt(30.0, 40.0))
        assertFalse(PrayerCalculator.isInsideEgypt(Double.NaN, 31.0))
    }
}
