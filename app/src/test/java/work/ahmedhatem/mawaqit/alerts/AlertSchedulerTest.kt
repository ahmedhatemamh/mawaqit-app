package work.ahmedhatem.mawaqit.alerts

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import work.ahmedhatem.mawaqit.engine.CityFinder
import work.ahmedhatem.mawaqit.engine.EgyptClock
import work.ahmedhatem.mawaqit.engine.Prayer
import work.ahmedhatem.mawaqit.engine.PrayerCalculator
import work.ahmedhatem.mawaqit.engine.alerts.AlertKind
import work.ahmedhatem.mawaqit.engine.alerts.AlertRole
import work.ahmedhatem.mawaqit.engine.alerts.AlertRule
import work.ahmedhatem.mawaqit.engine.alerts.AlertTarget
import work.ahmedhatem.mawaqit.settings.Place
import work.ahmedhatem.mawaqit.settings.Settings

class AlertSchedulerTest {
    private class FakeBackend(var exact: Boolean = true) : AlarmBackend {
        val scheduled = mutableListOf<Pair<FiredAlert, Instant>>()
        var cancels = 0
        override fun schedule(alert: FiredAlert, at: Instant): Boolean {
            scheduled += alert to at
            return exact
        }
        override fun cancel() { cancels++ }
    }

    private val day = LocalDate.of(2026, 11, 25)
    private val settings = Settings()
    private val now = EgyptClock.toInstant(day.atStartOfDay().plusMinutes(60)) // 01:00

    private fun asr(day: LocalDate, place: Place = settings.place): Instant {
        val minute = PrayerCalculator.timesFor(day, place.latitude, place.longitude)[Prayer.ASR]
        return EgyptClock.toInstant(day.atStartOfDay().plusMinutes(minute.toLong()))
    }

    private fun rule(id: String = "r", offset: Int = -30, enabled: Boolean = true, kind: AlertKind = AlertKind.ALARM) =
        AlertRule(id, kind, setOf(AlertTarget.ASR), offset, enabled = enabled)

    private fun plan(backend: AlarmBackend, rules: List<AlertRule>, s: Settings = settings, prayed: Set<Pair<LocalDate, Prayer>> = emptySet(), at: Instant = now) =
        AlertScheduler(backend).reschedule(rules, s, prayed, at)

    @Test
    fun theNextAlertIsTheOnlyAlarmSetAndItIsExactlyOnTime() {
        val backend = FakeBackend()
        val result = plan(backend, listOf(rule()))
        val (alert, at) = backend.scheduled.single()
        assertEquals(asr(day).minusSeconds(30 * 60), at)
        assertEquals(-30, alert.offsetMinutes)
        assertEquals(AlertRole.BEFORE, alert.role)
        assertTrue(result is Plan.Scheduled && result.exact)
        assertEquals(0, backend.cancels)
    }

    @Test
    fun whenThePhoneAllowsOnlyAnApproximateAlarmThePlanSaysSo() {
        val result = plan(FakeBackend(exact = false), listOf(rule()))
        assertTrue(result is Plan.Scheduled && !result.exact)
    }

    @Test
    fun noRulesDisabledOrBrokenRulesCancelThePendingAlarm() {
        for (rules in listOf(emptyList(), listOf(rule(enabled = false)), listOf(rule(offset = -181)))) {
            val backend = FakeBackend()
            assertEquals(Plan.None, plan(backend, rules))
            assertEquals(1, backend.cancels)
            assertTrue(backend.scheduled.isEmpty())
        }
    }

    @Test
    fun afterTheAlertFiredThePlanMovesToTheNextDay() {
        val backend = FakeBackend()
        plan(backend, listOf(rule()), at = asr(day).minusSeconds(30 * 60))
        assertEquals(asr(day.plusDays(1)).minusSeconds(30 * 60), backend.scheduled.single().second)
    }

    @Test
    fun anAnswerIPrayedSkipsThatQuestion() {
        val rules = listOf(rule(kind = AlertKind.ALARM, offset = -30).copy(targets = setOf(AlertTarget.MAGHRIB)))
        val backend = FakeBackend()
        plan(backend, rules, prayed = setOf(day to Prayer.ASR))
        val tomorrow = PrayerCalculator.timesFor(day.plusDays(1), settings.place.latitude, settings.place.longitude)[Prayer.MAGHRIB]
        assertEquals(
            EgyptClock.toInstant(day.plusDays(1).atStartOfDay().plusMinutes(tomorrow.toLong())).minusSeconds(30 * 60),
            backend.scheduled.single().second,
        )
    }

    @Test
    fun aDifferentCityPlansADifferentMinute() {
        val aswan = Place.ofCity(CityFinder.nearest(24.09, 32.9).name)!!
        assertNotEquals(settings.place.label, aswan.label)
        val here = FakeBackend().also { plan(it, listOf(rule())) }
        val there = FakeBackend().also { plan(it, listOf(rule()), s = Settings(aswan)) }
        assertEquals(asr(day, aswan).minusSeconds(30 * 60), there.scheduled.single().second)
        assertNotEquals(here.scheduled.single().second, there.scheduled.single().second)
    }

    @Test
    fun theEarliestOfSeveralRulesWins() {
        val backend = FakeBackend()
        plan(backend, listOf(rule("late", -10), rule("early", -90)))
        assertEquals("early", backend.scheduled.single().first.ruleId)
    }

    @Test
    fun alertsAtTheSameMomentAreAllShownNotOnlyTheFirst() {
        // A reminder and an alarm 30 minutes before Asr fall on the same minute: one alarm clock entry, two notifications.
        val rules = listOf(rule("alarm"), rule("reminder", kind = AlertKind.REMINDER))
        val backend = FakeBackend()
        plan(backend, rules)
        val first = backend.scheduled.single().first
        val others = AlertScheduler(backend).companionsOf(first, rules, settings, emptySet())
        assertEquals(1, others.size)
        assertNotEquals(first.ruleId, others.single().ruleId)
        assertEquals(first.atMillis, others.single().atMillis)
    }

    @Test
    fun aloneAlertHasNoCompanionsAndAnAnsweredQuestionIsNotAskedAgain() {
        val backend = FakeBackend()
        val rules = listOf(rule("a"), rule("b", offset = -45))
        plan(backend, rules)
        val first = backend.scheduled.single().first
        assertEquals("b", first.ruleId)
        assertTrue(AlertScheduler(backend).companionsOf(first, rules, settings, emptySet()).isEmpty())

        // An alarm (asks about Dhuhr) and a reminder at the same minute: once Dhuhr is answered only the reminder remains.
        val both = listOf(rule("alarm"), rule("reminder", kind = AlertKind.REMINDER))
        val prayed = setOf(day to Prayer.DHUHR)
        val b2 = FakeBackend()
        plan(b2, both, prayed = prayed)
        val only = b2.scheduled.single().first
        assertEquals("reminder", only.ruleId)
        assertEquals(asr(day).minusSeconds(30 * 60), b2.scheduled.single().second)
        assertTrue(AlertScheduler(b2).companionsOf(only, both, settings, prayed).isEmpty())
    }

    @Test
    fun aOneDayRuleMoreThanTwoWeeksAheadIsStillScheduled() {
        val far = day.plusDays(100)
        val rule = AlertRule("far", AlertKind.REMINDER, setOf(AlertTarget.ASR), -30, recurrence = work.ahmedhatem.mawaqit.engine.alerts.Recurrence.Once(far))
        val backend = FakeBackend()
        val result = plan(backend, listOf(rule))
        assertTrue(result is Plan.Scheduled)
        assertEquals(asr(far).minusSeconds(30 * 60), backend.scheduled.single().second)
        assertEquals(0, backend.cancels)
    }

    @Test
    fun theLimitsOfTheOffsetAreEnforcedWhenPlanning() {
        for (offset in listOf(-180, 0, 180)) {
            val backend = FakeBackend()
            assertTrue("offset $offset is allowed", plan(backend, listOf(rule(offset = offset))) is Plan.Scheduled)
        }
        for (offset in listOf(-181, 181)) {
            val backend = FakeBackend()
            assertEquals("offset $offset is refused", Plan.None, plan(backend, listOf(rule(offset = offset))))
            assertTrue(backend.scheduled.isEmpty())
        }
    }

    @Test
    fun aOneDayRuleInThePastCancelsThePendingAlarm() {
        val past = AlertRule("old", AlertKind.REMINDER, setOf(AlertTarget.ASR), -30, recurrence = work.ahmedhatem.mawaqit.engine.alerts.Recurrence.Once(day.minusDays(3)))
        val backend = FakeBackend()
        assertEquals(Plan.None, plan(backend, listOf(past)))
        assertEquals(1, backend.cancels)
        assertTrue(backend.scheduled.isEmpty())
    }

    private fun at(day: LocalDate, prayer: Prayer): Instant {
        val minute = PrayerCalculator.timesFor(day, settings.place.latitude, settings.place.longitude)[prayer]
        return EgyptClock.toInstant(day.atStartOfDay().plusMinutes(minute.toLong()))
    }

    @Test
    fun alertsThatFellDueWhileTheAlarmWasLateAreShownToo() {
        val rules = listOf(rule("first", offset = -30, kind = AlertKind.REMINDER), rule("second", offset = -29, kind = AlertKind.REMINDER))
        val backend = FakeBackend()
        plan(backend, rules)
        val first = backend.scheduled.single().first
        assertEquals("first", first.ruleId)
        val scheduler = AlertScheduler(backend)
        // On time: only the alert itself.
        assertTrue(scheduler.companionsOf(first, rules, settings, emptySet()).isEmpty())
        // Delivered five minutes late: the alert due one minute after it is not lost.
        val late = Instant.ofEpochMilli(first.atMillis).plusSeconds(5 * 60)
        assertEquals(listOf("second"), scheduler.companionsOf(first, rules, settings, emptySet(), late).map { it.ruleId })
        // Not yet due at delivery time: not shown (it is planned as the next alarm instead).
        val early = Instant.ofEpochMilli(first.atMillis).plusSeconds(30)
        assertTrue(scheduler.companionsOf(first, rules, settings, emptySet(), early).isEmpty())
    }

    @Test
    fun aShownAlertDisappearsAtTheNextPrayerTime() {
        val shownAt = at(day, Prayer.ASR).minusSeconds(60 * 60)
        assertEquals(at(day, Prayer.ASR), AlertScheduler.expiryAfter(shownAt, settings))
        // After Isha the next prayer is tomorrow's Fajr.
        val night = at(day, Prayer.ISHA).plusSeconds(60 * 60)
        assertEquals(at(day.plusDays(1), Prayer.FAJR), AlertScheduler.expiryAfter(night, settings))
    }

    @Test
    fun aShownAlertNeverDisappearsBeforeTheMinimumEvenWhenThePrayerIsNear() {
        val shownAt = at(day, Prayer.ASR).minusSeconds(60)
        assertEquals(shownAt.plusSeconds(AlertScheduler.MIN_VISIBLE_MINUTES * 60), AlertScheduler.expiryAfter(shownAt, settings))
        assertEquals(15L, AlertScheduler.MIN_VISIBLE_MINUTES)
    }

    @Test
    fun aQuestionAlreadyAnsweredIsNotShownEvenIfTheAlarmWasSetBefore() {
        val backend = FakeBackend()
        plan(backend, listOf(rule("alarm")))
        val asrBefore = backend.scheduled.single().first // asks about Dhuhr on `day`
        val scheduler = AlertScheduler(backend)
        assertTrue(!scheduler.isAnswered(asrBefore, emptySet()))
        assertTrue(scheduler.isAnswered(asrBefore, setOf(day to Prayer.DHUHR)))
        assertTrue(!scheduler.isAnswered(asrBefore, setOf(day.minusDays(1) to Prayer.DHUHR, day to Prayer.ASR)))
        // Before Fajr the question is about yesterday's Isha.
        val fajr = AlertRule("fajr", AlertKind.ALARM, setOf(AlertTarget.FAJR), -30)
        val b2 = FakeBackend()
        plan(b2, listOf(fajr))
        val fajrAlert = b2.scheduled.single().first
        assertTrue(scheduler.isAnswered(fajrAlert, setOf(day.minusDays(1) to Prayer.ISHA)))
        assertTrue(!scheduler.isAnswered(fajrAlert, setOf(day to Prayer.ISHA)))
        // A reminder or an "after" alert asks nothing.
        val reminder = FiredAlert("r", AlertKind.REMINDER, AlertTarget.ASR, AlertRole.BEFORE, -30, null, asr(day).toEpochMilli())
        assertTrue(!scheduler.isAnswered(reminder, setOf(day to Prayer.DHUHR)))
    }
}
