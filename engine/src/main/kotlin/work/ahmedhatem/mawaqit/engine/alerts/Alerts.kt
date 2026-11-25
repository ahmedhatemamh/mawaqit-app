package work.ahmedhatem.mawaqit.engine.alerts

import java.time.LocalDate

/** A time an alert can be tied to: the six daily times, plus the end of the discouraged period after sunrise. */
enum class AlertTarget(val arabicName: String) {
    FAJR("الفجر"),
    SUNRISE("الشروق"),
    MAKRUH_END("نهاية وقت الكراهة"),
    DHUHR("الظهر"),
    ASR("العصر"),
    MAGHRIB("المغرب"),
    ISHA("العشاء"),
}

enum class AlertKind {
    /** A plain notification (short tone, light vibration): it only tells that a time is near. Nothing to answer. */
    REMINDER,

    /** An alarm that asks "did you pray?" before the time and says "get up to pray" after it. */
    ALARM,
}

/** Before the time (negative offset) or at and after it (offset 0 or more). */
enum class AlertRole { BEFORE, AFTER }

/** The prayer a "did you pray?" question is about. [FAJR_AND_DUHA] is the question asked before Dhuhr. */
enum class Question { FAJR, DHUHR, ASR, MAGHRIB, ISHA, FAJR_AND_DUHA }

sealed interface Recurrence {
    /** Every day, with no end. */
    data object Daily : Recurrence

    /** Every day from [from] to [to], both included. */
    data class Range(val from: LocalDate, val to: LocalDate) : Recurrence

    /** One day only. */
    data class Once(val date: LocalDate) : Recurrence

    fun covers(date: LocalDate): Boolean = when (this) {
        Daily -> true
        is Range -> date >= from && date <= to
        is Once -> date == this.date
    }
}

/**
 * One alert the user set up: [offsetMinutes] before (negative) or after (0 or more) each of [targets], on the days
 * [recurrence] covers. One rule can hold several targets, so the same alert applies to some prayers or all of them.
 */
data class AlertRule(
    val id: String,
    val kind: AlertKind,
    val targets: Set<AlertTarget>,
    val offsetMinutes: Int,
    val recurrence: Recurrence = Recurrence.Daily,
    val enabled: Boolean = true,
    val flash: Boolean = false,
    /** Null: the phone's notification sound. Otherwise the address of a tone, a song or the built-in call to prayer. */
    val soundUri: String? = null,
    /** Alarms only: how often the question repeats while the answer is "no". */
    val repeatEveryMinutes: Int = DEFAULT_REPEAT_MINUTES,
) {
    val role: AlertRole get() = if (offsetMinutes < 0) AlertRole.BEFORE else AlertRole.AFTER

    companion object {
        const val MAX_MINUTES = 180
        const val MAX_PER_SIDE = 3
        const val DEFAULT_REPEAT_MINUTES = 10
        val REPEAT_RANGE = 1..60
    }
}

enum class RuleProblem {
    NO_TARGETS,
    OFFSET_OUT_OF_RANGE,
    REMINDER_MUST_BE_BEFORE,
    BAD_DATE_RANGE,
    BAD_REPEAT,

    /** Another rule earlier in the list has the same id (ids name the alert on the phone, so each must be unique). */
    DUPLICATE_ID,

    /** More than three alerts of one kind before the same time. */
    TOO_MANY_BEFORE,

    /** More than three alerts of one kind at or after the same time. */
    TOO_MANY_AFTER,
}

data class RuleError(val ruleId: String, val problem: RuleProblem)

object AlertRules {
    /**
     * Every problem in [rules], in list order. A target counts its rules in order: the rule that makes the fourth alert
     * of one kind on one side of one time is the one reported (a disabled rule still holds its place).
     */
    fun problems(rules: List<AlertRule>): List<RuleError> = problemsByIndex(rules).map { (i, p) -> RuleError(rules[i].id, p) }

    /** The same problems, with the position of the rule in [rules] instead of its id, so a rule can be told from one that shares its id. */
    fun problemsByIndex(rules: List<AlertRule>): List<Pair<Int, RuleProblem>> {
        val errors = mutableListOf<Pair<Int, RuleProblem>>()
        val counts = HashMap<Triple<AlertTarget, AlertKind, AlertRole>, Int>()
        val seen = HashSet<String>()
        for ((index, rule) in rules.withIndex()) {
            fun add(problem: RuleProblem) { errors += index to problem }
            if (!seen.add(rule.id)) add(RuleProblem.DUPLICATE_ID)
            if (rule.targets.isEmpty()) add(RuleProblem.NO_TARGETS)
            if (rule.offsetMinutes !in -AlertRule.MAX_MINUTES..AlertRule.MAX_MINUTES) add(RuleProblem.OFFSET_OUT_OF_RANGE)
            if (rule.kind == AlertKind.REMINDER && rule.offsetMinutes >= 0) add(RuleProblem.REMINDER_MUST_BE_BEFORE)
            val range = rule.recurrence
            if (range is Recurrence.Range && range.from > range.to) add(RuleProblem.BAD_DATE_RANGE)
            if (rule.repeatEveryMinutes !in AlertRule.REPEAT_RANGE) add(RuleProblem.BAD_REPEAT)
            var tooMany = false
            for (target in rule.targets) {
                val key = Triple(target, rule.kind, rule.role)
                val n = (counts[key] ?: 0) + 1
                counts[key] = n
                if (n > AlertRule.MAX_PER_SIDE) tooMany = true
            }
            if (tooMany) add(if (rule.role == AlertRole.BEFORE) RuleProblem.TOO_MANY_BEFORE else RuleProblem.TOO_MANY_AFTER)
        }
        return errors
    }
}
