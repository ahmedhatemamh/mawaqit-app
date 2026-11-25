package work.ahmedhatem.mawaqit.engine.alerts

import java.time.Instant
import java.time.LocalDate
import work.ahmedhatem.mawaqit.engine.DayTimes
import work.ahmedhatem.mawaqit.engine.EgyptClock
import work.ahmedhatem.mawaqit.engine.Prayer
import work.ahmedhatem.mawaqit.engine.PrayerCalculator

/** One moment an alert should fire. */
data class Occurrence(
    val ruleId: String,
    val kind: AlertKind,
    val target: AlertTarget,
    val role: AlertRole,
    /** When it fires. */
    val at: Instant,
    /** The time the alert is about (the prayer time itself). */
    val targetAt: Instant,
    /** The "did you pray?" question, for alarms before a time; null for reminders, after-alerts and times with none. */
    val question: Question?,
    /** The day the question is about (the day before, for the question asked before Fajr). */
    val questionDate: LocalDate?,
    /** The prayer to get up for, for alarms at or after a prayer time. */
    val prayerToPray: Prayer?,
    val repeatEveryMinutes: Int,
    val flash: Boolean,
    val soundUri: String?,
)

/**
 * Works out which alert fires next. It only decides the moments: ringing, notifications and the alarm clock are the
 * phone's job and read this list. A rule with any problem ([AlertRules.problems]) or switched off never fires.
 */
object AlertPlanner {
    const val DEFAULT_MAX_DAYS = 14

    private val ORDER = listOf(
        AlertTarget.FAJR, AlertTarget.SUNRISE, AlertTarget.DHUHR, AlertTarget.ASR, AlertTarget.MAGHRIB, AlertTarget.ISHA,
    )

    /**
     * The first [count] occurrences strictly after [from], earliest first, looking at most [maxDays] days ahead.
     * [prayed] holds the answers "I prayed" the user already gave, as (day, prayer): the question about that prayer is
     * not asked again.
     */
    fun upcoming(
        rules: List<AlertRule>,
        from: Instant,
        count: Int,
        latitude: Double,
        longitude: Double,
        makruhMinutes: Int,
        prayed: Set<Pair<LocalDate, Prayer>> = emptySet(),
        maxDays: Int = DEFAULT_MAX_DAYS,
    ): List<Occurrence> {
        require(count > 0 && maxDays > 0)
        val broken = AlertRules.problemsByIndex(rules).map { it.first }.toSet()
        val live = rules.filterIndexed { i, rule -> rule.enabled && i !in broken }
        if (live.isEmpty()) return emptyList()
        val first = EgyptClock.localDateTime(from).toLocalDate().minusDays(1)
        val found = mutableListOf<Occurrence>()
        for (i in 0 until maxDays + 1) {
            val date = first.plusDays(i.toLong())
            val times = PrayerCalculator.timesFor(date, latitude, longitude)
            for (rule in live) {
                if (!rule.recurrence.covers(date)) continue
                for (target in rule.targets) {
                    val o = occurrence(rule, target, date, times, latitude, longitude, makruhMinutes) ?: continue
                    if (o.at <= from) continue
                    val q = o.question
                    val qd = o.questionDate
                    if (q != null && qd != null && (qd to questionPrayer(q)) in prayed) continue
                    found += o
                }
            }
        }
        return found.sortedWith(compareBy({ it.at }, { it.ruleId }, { it.target })).take(count)
    }

    private fun occurrence(
        rule: AlertRule,
        target: AlertTarget,
        date: LocalDate,
        times: DayTimes,
        latitude: Double,
        longitude: Double,
        makruhMinutes: Int,
    ): Occurrence? {
        val minute = minuteOf(target, times, makruhMinutes)
        val targetAt = EgyptClock.toInstant(date.atStartOfDay().plusMinutes(minute.toLong()))
        // "Minutes before" is real elapsed time from the prayer, also when the clock changes in between.
        val at = targetAt.plusSeconds(rule.offsetMinutes * 60L)
        if (rule.role == AlertRole.AFTER && at >= boundary(target, minute, date, times, latitude, longitude)) return null
        val role = rule.role
        val alarm = rule.kind == AlertKind.ALARM
        val question = if (alarm && role == AlertRole.BEFORE) questionBefore(target) else null
        val questionDate = when {
            question == null -> null
            target == AlertTarget.FAJR -> date.minusDays(1)
            else -> date
        }
        val toPray = if (alarm && role == AlertRole.AFTER) prayerOf(target) else null
        return Occurrence(
            rule.id, rule.kind, target, role, at, targetAt, question, questionDate, toPray,
            rule.repeatEveryMinutes, rule.flash, rule.soundUri,
        )
    }

    private fun minuteOf(target: AlertTarget, times: DayTimes, makruhMinutes: Int): Int = when (target) {
        AlertTarget.FAJR -> times[Prayer.FAJR]
        AlertTarget.SUNRISE -> times[Prayer.SUNRISE]
        AlertTarget.MAKRUH_END -> times[Prayer.SUNRISE] + makruhMinutes
        AlertTarget.DHUHR -> times[Prayer.DHUHR]
        AlertTarget.ASR -> times[Prayer.ASR]
        AlertTarget.MAGHRIB -> times[Prayer.MAGHRIB]
        AlertTarget.ISHA -> times[Prayer.ISHA]
    }

    /** The next daily time after [minute]: an alert after a time must come before it, or it is dropped for that day. */
    private fun boundary(
        target: AlertTarget,
        minute: Int,
        date: LocalDate,
        times: DayTimes,
        latitude: Double,
        longitude: Double,
    ): Instant {
        for (t in ORDER) {
            val m = minuteOf(t, times, 0)
            if (m > minute && t != target) return EgyptClock.toInstant(date.atStartOfDay().plusMinutes(m.toLong()))
        }
        val tomorrow = date.plusDays(1)
        val m = PrayerCalculator.timesFor(tomorrow, latitude, longitude)[Prayer.FAJR]
        return EgyptClock.toInstant(tomorrow.atStartOfDay().plusMinutes(m.toLong()))
    }

    /** The question an alarm asks before a time: about the prayer whose period ends there. */
    internal fun questionBefore(target: AlertTarget): Question? = when (target) {
        AlertTarget.FAJR -> Question.ISHA
        AlertTarget.SUNRISE -> Question.FAJR
        AlertTarget.MAKRUH_END -> null
        AlertTarget.DHUHR -> Question.FAJR_AND_DUHA
        AlertTarget.ASR -> Question.DHUHR
        AlertTarget.MAGHRIB -> Question.ASR
        AlertTarget.ISHA -> Question.MAGHRIB
    }

    internal fun prayerOf(target: AlertTarget): Prayer? = when (target) {
        AlertTarget.FAJR -> Prayer.FAJR
        AlertTarget.DHUHR -> Prayer.DHUHR
        AlertTarget.ASR -> Prayer.ASR
        AlertTarget.MAGHRIB -> Prayer.MAGHRIB
        AlertTarget.ISHA -> Prayer.ISHA
        AlertTarget.SUNRISE, AlertTarget.MAKRUH_END -> null
    }

    /** The prayer a question is about, for matching against the answers already given. */
    fun questionPrayer(question: Question): Prayer = when (question) {
        Question.FAJR, Question.FAJR_AND_DUHA -> Prayer.FAJR
        Question.DHUHR -> Prayer.DHUHR
        Question.ASR -> Prayer.ASR
        Question.MAGHRIB -> Prayer.MAGHRIB
        Question.ISHA -> Prayer.ISHA
    }
}
