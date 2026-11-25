package work.ahmedhatem.mawaqit.engine.alerts

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertRulesTest {
    private fun rule(
        id: String,
        offset: Int = -30,
        kind: AlertKind = AlertKind.ALARM,
        targets: Set<AlertTarget> = setOf(AlertTarget.ASR),
        recurrence: Recurrence = Recurrence.Daily,
        repeat: Int = 10,
        enabled: Boolean = true,
    ) = AlertRule(id, kind, targets, offset, recurrence, enabled, repeatEveryMinutes = repeat)

    private fun problems(vararg rules: AlertRule) = AlertRules.problems(rules.toList())

    @Test
    fun aGoodRuleHasNoProblem() {
        assertTrue(problems(rule("a")).isEmpty())
        assertTrue(problems(rule("a", offset = 0), rule("b", offset = 180), rule("c", offset = -180)).isEmpty())
    }

    @Test
    fun offsetLimits() {
        assertEquals(listOf(RuleError("a", RuleProblem.OFFSET_OUT_OF_RANGE)), problems(rule("a", offset = 181)))
        assertEquals(listOf(RuleError("a", RuleProblem.OFFSET_OUT_OF_RANGE)), problems(rule("a", offset = -181)))
    }

    @Test
    fun repeatLimits() {
        assertTrue(problems(rule("a", repeat = 1), rule("b", repeat = 60)).isEmpty())
        assertEquals(RuleProblem.BAD_REPEAT, problems(rule("a", repeat = 0)).single().problem)
        assertEquals(RuleProblem.BAD_REPEAT, problems(rule("a", repeat = 61)).single().problem)
    }

    @Test
    fun noTargetsAndBadDateRange() {
        assertEquals(RuleProblem.NO_TARGETS, problems(rule("a", targets = emptySet())).single().problem)
        val day = LocalDate.of(2026, 5, 5)
        assertEquals(RuleProblem.BAD_DATE_RANGE, problems(rule("a", recurrence = Recurrence.Range(day.plusDays(1), day))).single().problem)
        assertTrue(problems(rule("a", recurrence = Recurrence.Range(day, day))).isEmpty())
    }

    @Test
    fun theFourthAlertBeforeTheSameTimeIsRefused() {
        val ok = problems(rule("1", -10), rule("2", -20), rule("3", -30))
        assertTrue(ok.isEmpty())
        val four = problems(rule("1", -10), rule("2", -20), rule("3", -30), rule("4", -40))
        assertEquals(listOf(RuleError("4", RuleProblem.TOO_MANY_BEFORE)), four)
    }

    @Test
    fun theFourthAlertAfterTheSameTimeIsRefused() {
        val four = problems(rule("1", 0), rule("2", 10), rule("3", 20), rule("4", 30))
        assertEquals(listOf(RuleError("4", RuleProblem.TOO_MANY_AFTER)), four)
    }

    @Test
    fun beforeAndAfterCountSeparatelyAndTargetsCountSeparately() {
        val three = (1..3).map { rule("b$it", -it * 10) } + (1..3).map { rule("a$it", it * 10) }
        assertTrue(AlertRules.problems(three).isEmpty())
        val otherTarget = rule("x", -50, targets = setOf(AlertTarget.FAJR))
        assertTrue(AlertRules.problems(three + otherTarget).isEmpty())
    }

    @Test
    fun alarmsAndRemindersCountSeparately() {
        val alarms = (1..3).map { rule("al$it", -it * 10) }
        val reminders = (1..3).map { rule("re$it", -it * 10, kind = AlertKind.REMINDER) }
        assertTrue(AlertRules.problems(alarms + reminders).isEmpty())
        val fourthReminder = rule("re4", -50, kind = AlertKind.REMINDER)
        assertEquals(listOf(RuleError("re4", RuleProblem.TOO_MANY_BEFORE)), AlertRules.problems(alarms + reminders + fourthReminder))
    }

    @Test
    fun aRuleWithSeveralTargetsCountsOnEachAndADisabledRuleStillHoldsItsPlace() {
        val all = AlertTarget.entries.toSet()
        val rules = listOf(rule("1", -10, targets = all), rule("2", -20, targets = all, enabled = false), rule("3", -30, targets = all))
        assertTrue(AlertRules.problems(rules).isEmpty())
        val fourth = rule("4", -40, targets = setOf(AlertTarget.DHUHR))
        assertEquals(listOf(RuleError("4", RuleProblem.TOO_MANY_BEFORE)), AlertRules.problems(rules + fourth))
    }

    @Test
    fun recurrenceCoversTheRightDays() {
        val d = LocalDate.of(2026, 11, 25)
        assertTrue(Recurrence.Daily.covers(d))
        val range = Recurrence.Range(d, d.plusDays(2))
        assertTrue(range.covers(d) && range.covers(d.plusDays(2)))
        assertTrue(!range.covers(d.minusDays(1)) && !range.covers(d.plusDays(3)))
        assertTrue(Recurrence.Once(d).covers(d) && !Recurrence.Once(d).covers(d.plusDays(1)))
    }
}
