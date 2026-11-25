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
}
