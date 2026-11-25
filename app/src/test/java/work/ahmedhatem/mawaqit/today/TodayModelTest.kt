package work.ahmedhatem.mawaqit.today

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import work.ahmedhatem.mawaqit.engine.CityFinder
import work.ahmedhatem.mawaqit.engine.HijriCalendar
import work.ahmedhatem.mawaqit.engine.Prayer
import work.ahmedhatem.mawaqit.engine.PrayerCalculator
import work.ahmedhatem.mawaqit.settings.Place
import work.ahmedhatem.mawaqit.settings.Settings
import work.ahmedhatem.mawaqit.ui.dataAgeDays

class TodayModelTest {
    private val calendar = HijriCalendar.bundled()
    private val settings = Settings()
    private fun build(now: LocalDateTime, s: Settings = settings) = TodayModel.build(now, s, calendar)

    @Test
    fun middayShowsSixTimesWithTheNextHighlighted() {
        val m = build(LocalDateTime.of(2026, 11, 25, 12, 30))
        assertEquals(6, m.rows.size)
        assertEquals(Prayer.entries, m.rows.map { it.prayer })
        assertEquals(listOf(Prayer.ASR), m.rows.filter { it.isNext }.map { it.prayer })
        assertEquals(m.next!!.prayer, Prayer.ASR)
        assertEquals(m.rows.map { it.minute }.sorted(), m.rows.map { it.minute })
        assertEquals("القاهرة", m.placeLabel)
        assertTrue(m.dstConfirmed)
    }

    @Test
    fun afterIshaNothingIsHighlightedAndTheNextIsTomorrowsFajr() {
        val m = build(LocalDateTime.of(2026, 11, 25, 23, 0))
        assertTrue(m.rows.none { it.isNext })
        assertEquals(Prayer.FAJR, m.next!!.prayer)
        assertEquals(LocalDate.of(2026, 11, 26), m.next!!.date)
    }

    @Test
    fun hijriDateMatchesTheOfficialOne() {
        val m = build(LocalDateTime.of(2026, 11, 23, 12, 0))
        assertEquals(13, m.hijri.day)
        assertEquals(6, m.hijri.month)
        assertEquals(1448, m.hijri.year)
        assertTrue(m.hijri.certain)
        assertFalse(build(LocalDateTime.of(2030, 3, 1, 12, 0)).hijri.certain)
    }

    @Test
    fun makruhEndsAfterSunriseByTheChosenMinutes() {
        val sunrise = PrayerCalculator.timesFor(LocalDate.of(2026, 11, 25), CityFinder.DEFAULT)[Prayer.SUNRISE]
        for (minutes in Settings.MAKRUH_CHOICES) {
            val m = build(LocalDateTime.of(2026, 11, 25, 9, 0), Settings(makruhMinutes = minutes))
            assertEquals(sunrise + minutes, m.makruhEndsAt)
        }
    }

    @Test
    fun anotherPlaceChangesTheTimes() {
        val aswan = Settings(place = Place.ofCity("أسوان")!!)
        val a = build(LocalDateTime.of(2026, 11, 25, 9, 0), aswan)
        val c = build(LocalDateTime.of(2026, 11, 25, 9, 0))
        assertEquals("أسوان", a.placeLabel)
        assertTrue(a.rows.map { it.minute } != c.rows.map { it.minute })
        val point = Settings(place = Place.ofPoint(30.46, 31.18)!!)
        assertTrue(build(LocalDateTime.of(2026, 11, 25, 9, 0), point).placeLabel.startsWith("قريب من"))
    }

    @Test
    fun summerRuleOfAnUnconfirmedYearIsFlagged() {
        assertFalse(build(LocalDateTime.of(2031, 7, 1, 12, 0)).dstConfirmed)
        assertTrue(build(LocalDateTime.of(2026, 7, 1, 12, 0)).dstConfirmed)
    }

    @Test
    fun anInstantIsReadInEgyptTimeAndIsNotTiedToThePhoneZone() {
        val saved = java.util.TimeZone.getDefault()
        try {
            for (zone in listOf("UTC", "America/Los_Angeles", "Asia/Tokyo")) {
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(zone))
                val m = TodayModel.build(Instant.parse("2026-11-25T22:30:00Z"), settings, calendar)   // 00:30 on the 26th in Egypt
                assertEquals(zone, LocalDate.of(2026, 11, 26), m.date)
            }
        } finally {
            java.util.TimeZone.setDefault(saved)
        }
    }

    @Test
    fun theDayChangesAtMidnightWithItsOwnTimes() {
        val before = build(LocalDateTime.of(2026, 11, 25, 23, 59, 59))
        val after = build(LocalDateTime.of(2026, 11, 26, 0, 0, 0))
        assertEquals(LocalDate.of(2026, 11, 25), before.date)
        assertEquals(LocalDate.of(2026, 11, 26), after.date)
        assertEquals(Prayer.FAJR, after.next!!.prayer)
        assertTrue(after.rows.first().isNext)
    }

    @Test
    fun dataAgeShowsOnlyWhenOlderThanAMonth() {
        val day = 24L * 60 * 60 * 1000
        assertEquals(null, dataAgeDays(null, 5 * day))
        assertEquals(null, dataAgeDays(0, 30 * day))
        assertEquals(31, dataAgeDays(0, 31 * day))
        assertEquals(100, dataAgeDays(0, 100 * day))
        assertEquals(null, dataAgeDays(10 * day, 0))   // the clock went back
    }

    @Test
    fun aChosenDayHasNoCountdownAndNothingHighlighted() {
        val m = TodayModel.forDate(LocalDate.of(2025, 3, 1), settings, calendar)
        assertNull(m.next)
        assertTrue(m.rows.none { it.isNext })
        assertEquals(6, m.rows.size)
        assertEquals(1, m.hijri.day)
        assertEquals(9, m.hijri.month)
        assertEquals(1446, m.hijri.year)
    }

    @Test
    fun countdownAcrossTheRepeatedOctoberHourUsesRealElapsedTime() {
        // 2026-10-29 is the last Thursday of October: clocks go back at 24:00 -> 23:00 local, so that hour happens twice.
        // 20:30Z is 23:30 summer time (first pass); 21:30Z is 23:30 winter time (second pass).
        for (text in listOf("2026-10-29T20:30:00Z", "2026-10-29T21:30:00Z")) {
            val before = Instant.parse(text)
            val n = TodayModel.build(before, settings, calendar).next!!
            val target = work.ahmedhatem.mawaqit.engine.EgyptClock.toInstant(n.date.atStartOfDay().plusMinutes(n.minute.toLong()))
            assertEquals(Prayer.FAJR, n.prayer)
            assertEquals(text, java.time.Duration.between(before, target).seconds, n.secondsUntil.toLong())
        }
    }
}
