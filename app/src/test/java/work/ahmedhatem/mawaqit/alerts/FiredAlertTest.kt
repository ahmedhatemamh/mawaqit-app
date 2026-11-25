package work.ahmedhatem.mawaqit.alerts

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import work.ahmedhatem.mawaqit.engine.Prayer
import work.ahmedhatem.mawaqit.engine.alerts.AlertKind
import work.ahmedhatem.mawaqit.engine.alerts.AlertRole
import work.ahmedhatem.mawaqit.engine.alerts.AlertTarget
import work.ahmedhatem.mawaqit.engine.alerts.Occurrence
import work.ahmedhatem.mawaqit.engine.alerts.Question

class FiredAlertTest {
    private val good = FiredAlert("a1", AlertKind.ALARM, AlertTarget.ASR, AlertRole.BEFORE, -30, Question.DHUHR, 1_700_000_000_000L)

    @Test
    fun everyFieldSurvivesTheTripThroughStrings() {
        assertEquals(good, FiredAlert.fromMap(good.toMap()))
        val noQuestion = good.copy(kind = AlertKind.REMINDER, question = null)
        assertEquals(noQuestion, FiredAlert.fromMap(noQuestion.toMap()))
        val after = good.copy(role = AlertRole.AFTER, offsetMinutes = 0, question = null)
        assertEquals(after, FiredAlert.fromMap(after.toMap()))
    }

    @Test
    fun aDamagedAlertGivesNull() {
        val m = good.toMap()
        for (key in listOf(FiredAlert.ID, FiredAlert.KIND, FiredAlert.TARGET, FiredAlert.ROLE, FiredAlert.OFFSET, FiredAlert.AT)) {
            assertNull("missing $key", FiredAlert.fromMap(m - key))
        }
        assertNull(FiredAlert.fromMap(m + (FiredAlert.KIND to "LOUD")))
        assertNull(FiredAlert.fromMap(m + (FiredAlert.TARGET to "NOON")))
        assertNull(FiredAlert.fromMap(m + (FiredAlert.QUESTION to "WHY")))
        assertNull(FiredAlert.fromMap(m + (FiredAlert.OFFSET to "soon")))
        assertNull(FiredAlert.fromMap(m + (FiredAlert.AT to "yesterday")))
        assertNull(FiredAlert.fromMap(m + (FiredAlert.ID to "")))
    }

    @Test
    fun aRoleThatContradictsTheOffsetIsRefused() {
        assertNull(FiredAlert.fromMap(good.toMap() + (FiredAlert.OFFSET to "5")))
        assertNull(FiredAlert.fromMap(good.copy(role = AlertRole.AFTER).toMap()))
    }

    @Test
    fun anOccurrenceBecomesAnAlertWithTheMinutesBetweenTheFireAndTheTime() {
        val target = Instant.parse("2026-11-25T13:00:00Z")
        val o = Occurrence(
            "r", AlertKind.ALARM, AlertTarget.ASR, AlertRole.BEFORE, target.minus(Duration.ofMinutes(45)), target,
            Question.DHUHR, null, null, 10, false, null,
        )
        val a = FiredAlert.of(o)
        assertEquals(-45, a.offsetMinutes)
        assertEquals(o.at.toEpochMilli(), a.atMillis)
        assertEquals(Question.DHUHR, a.question)
        val after = o.copy(role = AlertRole.AFTER, at = target.plusSeconds(20 * 60), question = null, prayerToPray = Prayer.ASR)
        assertEquals(20, FiredAlert.of(after).offsetMinutes)
    }
}
