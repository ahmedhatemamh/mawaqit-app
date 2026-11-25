package work.ahmedhatem.mawaqit.engine.alerts

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import work.ahmedhatem.mawaqit.engine.CityFinder
import work.ahmedhatem.mawaqit.engine.EgyptClock
import work.ahmedhatem.mawaqit.engine.Prayer
import work.ahmedhatem.mawaqit.engine.PrayerCalculator

class AlertPlannerTest {
    private val cairo = CityFinder.DEFAULT
    private fun times(date: LocalDate) = PrayerCalculator.timesFor(date, cairo)
    private fun instant(date: LocalDate, minute: Int) = EgyptClock.toInstant(date.atStartOfDay().plusMinutes(minute.toLong()))

    private fun upcoming(
        rules: List<AlertRule>,
        from: Instant,
        count: Int = 50,
        makruh: Int = 25,
        prayed: Set<Pair<LocalDate, Prayer>> = emptySet(),
        maxDays: Int = 14,
    ) = AlertPlanner.upcoming(rules, from, count, cairo.latitude, cairo.longitude, makruh, prayed, maxDays)

    private fun rule(
        id: String = "r",
        kind: AlertKind = AlertKind.ALARM,
        targets: Set<AlertTarget> = setOf(AlertTarget.ASR),
        offset: Int = -30,
        recurrence: Recurrence = Recurrence.Daily,
        enabled: Boolean = true,
    ) = AlertRule(id, kind, targets, offset, recurrence, enabled)

    private val day = LocalDate.of(2026, 11, 25)
    private val morning = instant(day, 60) // 01:00 local

    @Test
    fun thirtyMinutesBeforeAsrIsExactlyThirtyMinutesBeforeAsr() {
        val o = upcoming(listOf(rule()), morning, count = 1).single()
        assertEquals(instant(day, times(day)[Prayer.ASR]), o.targetAt)
        assertEquals(instant(day, times(day)[Prayer.ASR] - 30), o.at)
        assertEquals(AlertRole.BEFORE, o.role)
    }

    @Test
    fun anAlertThatAlreadyFiredIsNotListedAgain() {
        val asr = instant(day, times(day)[Prayer.ASR])
        val first = upcoming(listOf(rule()), asr.minusSeconds(30 * 60 + 1), count = 1).single()
        assertEquals(day, EgyptClock.localDateTime(first.at).toLocalDate())
        val next = upcoming(listOf(rule()), asr.minusSeconds(30 * 60), count = 1).single()
        assertEquals(day.plusDays(1), EgyptClock.localDateTime(next.at).toLocalDate())
    }

    @Test
    fun atTheTimeItselfIsAnAfterAlertWithOffsetZero() {
        val o = upcoming(listOf(rule(offset = 0)), morning, count = 1).single()
        assertEquals(AlertRole.AFTER, o.role)
        assertEquals(o.targetAt, o.at)
        assertEquals(Prayer.ASR, o.prayerToPray)
        assertNull(o.question)
    }

    @Test
    fun everyOccurrenceLandsOnTheRightLocalMinuteAcrossTheSummerTimeChanges() {
        val dayTargets = setOf(AlertTarget.FAJR, AlertTarget.DHUHR, AlertTarget.MAGHRIB)
        val rules = listOf(rule(targets = dayTargets, offset = -45))
        val start = instant(LocalDate.of(2026, 4, 1), 0)
        val list = upcoming(rules, start, count = 3000, maxDays = 400)
        assertTrue(list.size > 1000)
        for (o in list) {
            // before the time by 45 real minutes
            assertEquals(Duration.ofMinutes(45), Duration.between(o.at, o.targetAt))
            // and the target is the calculator's minute on its own local day
            val local = EgyptClock.localDateTime(o.targetAt)
            val prayer = AlertPlanner.prayerOf(o.target)!!
            assertEquals("${o.target} ${local.toLocalDate()}", times(local.toLocalDate())[prayer], local.hour * 60 + local.minute)
        }
    }

    @Test
    fun theHourOfTheClockChangeIsKept() {
        // Summer time ends on Thursday 29 October 2026: Fajr of the 30th is on winter time, one real hour later on the
        // clock than if the offset had not changed. The alert 30 minutes before it is still 30 minutes before.
        val d = LocalDate.of(2026, 10, 30)
        val o = upcoming(listOf(rule(targets = setOf(AlertTarget.FAJR))), instant(LocalDate.of(2026, 10, 29), 600), count = 1).single()
        assertEquals(instant(d, times(d)[Prayer.FAJR]), o.targetAt)
        assertEquals(Duration.ofMinutes(30), Duration.between(o.at, o.targetAt))
    }

    @Test
    fun theMakruhEndFollowsTheChosenMinutes() {
        val r = rule(targets = setOf(AlertTarget.MAKRUH_END), offset = -5, kind = AlertKind.REMINDER)
        for (m in listOf(10, 15, 20, 25)) {
            val o = upcoming(listOf(r), morning, count = 1, makruh = m).single()
            assertEquals(instant(day, times(day)[Prayer.SUNRISE] + m), o.targetAt)
        }
    }

    @Test
    fun recurrenceDailyRangeAndOnce() {
        val range = rule(id = "range", recurrence = Recurrence.Range(day.plusDays(2), day.plusDays(4)))
        val once = rule(id = "once", targets = setOf(AlertTarget.ISHA), recurrence = Recurrence.Once(day.plusDays(1)))
        val days = upcoming(listOf(range, once), morning, count = 20, maxDays = 10)
            .map { it.ruleId to EgyptClock.localDateTime(it.targetAt).toLocalDate() }
        assertEquals(
            listOf("once" to day.plusDays(1), "range" to day.plusDays(2), "range" to day.plusDays(3), "range" to day.plusDays(4)),
            days.sortedBy { it.second },
        )
    }

    @Test
    fun aDisabledOrBrokenRuleNeverFires() {
        assertTrue(upcoming(listOf(rule(enabled = false)), morning).isEmpty())
        assertTrue(upcoming(listOf(rule(offset = -181)), morning).isEmpty())
        assertTrue(upcoming(listOf(rule(targets = emptySet())), morning).isEmpty())
        assertTrue(upcoming(listOf(rule(recurrence = Recurrence.Range(day.plusDays(3), day))), morning).isEmpty())
    }

    @Test
    fun aBrokenRuleDoesNotSwitchOffAValidRuleThatComesBeforeItWithTheSameId() {
        val valid = rule(id = "r", offset = -30)
        val brokenSameId = rule(id = "r", offset = -181)
        val list = upcoming(listOf(valid, brokenSameId), morning, count = 1)
        assertEquals(1, list.size)
        assertEquals(instant(day, times(day)[Prayer.ASR] - 30), list.single().at)
    }

    @Test
    fun anAfterAlertPastTheNextPrayerIsDroppedForThatDay() {
        val asr = times(day)[Prayer.ASR]
        val maghrib = times(day)[Prayer.MAGHRIB]
        val gap = maghrib - asr
        val keep = rule(id = "keep", offset = gap - 1)
        val drop = rule(id = "drop", offset = gap)
        val list = upcoming(listOf(keep, drop), morning, count = 5, maxDays = 1).filter {
            EgyptClock.localDateTime(it.targetAt).toLocalDate() == day
        }
        assertEquals(listOf("keep"), list.map { it.ruleId })
    }

    @Test
    fun anAfterAlertAfterIshaIsKeptEvenPastMidnight() {
        // Isha to the next Fajr is always more than 180 minutes, so the longest alert (180) after Isha is never dropped.
        var date = LocalDate.of(2026, 1, 1)
        repeat(365) {
            val gap = (1440 - times(date)[Prayer.ISHA]) + times(date.plusDays(1))[Prayer.FAJR]
            assertTrue("$date gap $gap", gap > AlertRule.MAX_MINUTES)
            date = date.plusDays(1)
        }
        val r = rule(targets = setOf(AlertTarget.ISHA), offset = 180)
        val o = upcoming(listOf(r), morning, count = 1).single()
        assertEquals(instant(day, times(day)[Prayer.ISHA] + 180), o.at)
        assertEquals(Prayer.ISHA, o.prayerToPray)
    }

    @Test
    fun theQuestionBeforeEachTimeIsAboutThePreviousPrayer() {
        val all = AlertTarget.entries.toSet()
        val list = upcoming(listOf(rule(targets = all, offset = -10)), morning, count = 100, maxDays = 1)
            .filter { EgyptClock.localDateTime(it.targetAt).toLocalDate() == day }
            .associate { it.target to it.question }
        assertEquals(
            mapOf(
                AlertTarget.FAJR to Question.ISHA,
                AlertTarget.SUNRISE to Question.FAJR,
                AlertTarget.MAKRUH_END to null,
                AlertTarget.DHUHR to Question.FAJR_AND_DUHA,
                AlertTarget.ASR to Question.DHUHR,
                AlertTarget.MAGHRIB to Question.ASR,
                AlertTarget.ISHA to Question.MAGHRIB,
            ),
            list,
        )
    }

    @Test
    fun theQuestionBeforeFajrIsAboutYesterdaysIsha() {
        val r = rule(targets = setOf(AlertTarget.FAJR))
        val o = upcoming(listOf(r), morning, count = 1).single()
        assertEquals(day.minusDays(1), o.questionDate)
        assertEquals(Question.ISHA, o.question)
    }

    @Test
    fun answeringIPrayedSkipsTheNextQuestionAboutThatPrayer() {
        val r = rule(id = "maghribQ", targets = setOf(AlertTarget.MAGHRIB), offset = -20)
        assertEquals(day, EgyptClock.localDateTime(upcoming(listOf(r), morning, count = 1).single().targetAt).toLocalDate())
        // "I prayed Asr" today: the question before Maghrib today is skipped, tomorrow's is kept
        val skipped = upcoming(listOf(r), morning, count = 1, prayed = setOf(day to Prayer.ASR)).single()
        assertEquals(day.plusDays(1), EgyptClock.localDateTime(skipped.targetAt).toLocalDate())
    }

    @Test
    fun remindersAskNothingAndAreBeforeOnly() {
        val r = rule(kind = AlertKind.REMINDER, offset = -15)
        val o = upcoming(listOf(r), morning, count = 1).single()
        assertNull(o.question)
        assertNull(o.prayerToPray)
        assertTrue(AlertRules.problems(listOf(rule(kind = AlertKind.REMINDER, offset = 0))).any { it.problem == RuleProblem.REMINDER_MUST_BE_BEFORE })
    }

    @Test
    fun resultIsSortedAndLimited() {
        val rules = listOf(
            rule(id = "a", targets = setOf(AlertTarget.ASR, AlertTarget.FAJR)),
            rule(id = "b", targets = setOf(AlertTarget.ISHA), offset = 0),
        )
        val list = upcoming(rules, morning, count = 7)
        assertEquals(7, list.size)
        assertEquals(list.map { it.at }.sorted(), list.map { it.at })
        assertTrue(list.all { it.at > morning })
    }
}
