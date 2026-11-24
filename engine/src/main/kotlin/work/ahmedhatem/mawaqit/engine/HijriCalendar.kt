package work.ahmedhatem.mawaqit.engine

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/** The official first day of one Hijri month (year, month 1 to 12). */
data class HijriMonthStart(val year: Int, val month: Int, val start: LocalDate) {
    init {
        require(month in 1..12) { "Hijri month out of range: $month" }
    }

    internal val index: Int get() = year * 12 + month - 1
}

/** A Hijri date. [certain] is false when it depends on a month start that is only estimated. */
data class HijriDate(val year: Int, val month: Int, val day: Int, val certain: Boolean)

/**
 * Converts any civil date to a Hijri date.
 *
 * Month starts come from the official statements ([months]). Every other month start, whether in the gaps, before the
 * first official month or after the last, is estimated from the crescent (see [CrescentEstimator], about 92% right,
 * always within one day) and marked not certain. Months are kept 29 or 30 days long even when the estimate would give
 * 31, by limiting each estimated start to 29 or 30 days after the one before it.
 *
 * A month whose start is official is certain; if the following start is not yet known only its first 29 days are
 * certain, because the 30th day may already belong to the next month.
 */
class HijriCalendar internal constructor(
    months: List<HijriMonthStart>,
    private val rawEstimate: (Long) -> LocalDate,
) {
    constructor(months: List<HijriMonthStart>) : this(months, CrescentEstimator::estimatedStart)

    private val known: List<HijriMonthStart> = months.sortedBy { it.index }
    private val knownByIndex: Map<Int, LocalDate> = known.associate { it.index to it.start }
    private val anchorLunations = ConcurrentHashMap<Int, Long>()
    private val rawStarts = ConcurrentHashMap<Int, LocalDate>()

    init {
        require(known.isNotEmpty()) { "at least one official month start is needed" }
        require(knownByIndex.size == known.size) { "two official starts for the same month" }
        for ((a, b) in known.zipWithNext()) {
            val n = b.index - a.index
            val days = ChronoUnit.DAYS.between(a.start, b.start)
            require(days in 29L * n..30L * n) {
                "$n month(s) from ${a.year}-${a.month} to ${b.year}-${b.month} cannot last $days days"
            }
        }
    }

    /** The official month start nearest to a month number. */
    private fun nearestByIndex(index: Int): HijriMonthStart =
        known.minByOrNull { Math.abs(it.index - index) }!!

    private fun nearestByDate(date: LocalDate): HijriMonthStart =
        known.minByOrNull { Math.abs(ChronoUnit.DAYS.between(it.start, date)) }!!

    private fun rawStart(index: Int): LocalDate = rawStarts.getOrPut(index) {
        val anchor = nearestByIndex(index)
        val anchorLunation = anchorLunations.getOrPut(anchor.index) { CrescentEstimator.lunationOfStart(anchor.start) }
        rawEstimate(anchorLunation + (index - anchor.index))
    }

    /** First day of a month: official, or estimated and limited to 29 or 30 days after the month before it. */
    private fun startOf(index: Int, depth: Int = CLAMP_DEPTH): LocalDate {
        knownByIndex[index]?.let { return it }
        val raw = rawStart(index)
        val previous = knownByIndex[index - 1] ?: if (depth == 0) rawStart(index - 1) else startOf(index - 1, depth - 1)
        return when {
            raw.isBefore(previous.plusDays(29)) -> previous.plusDays(29)
            raw.isAfter(previous.plusDays(30)) -> previous.plusDays(30)
            else -> raw
        }
    }

    /** First day of a Hijri month, and whether it is official. */
    fun startOfMonth(year: Int, month: Int): Pair<LocalDate, Boolean> {
        require(month in 1..12) { "Hijri month out of range: $month" }
        val index = year * 12 + month - 1
        return Pair(startOf(index), knownByIndex.containsKey(index))
    }

    fun dateOf(date: LocalDate): HijriDate {
        val anchor = nearestByDate(date)
        var index = anchor.index + (ChronoUnit.DAYS.between(anchor.start, date) / CrescentEstimator.SYNODIC_MONTH).roundToInt()
        var steps = 0
        while (startOf(index).isAfter(date)) {
            index--
            check(++steps < MAX_STEPS) { "month search did not settle for $date" }
        }
        while (!startOf(index + 1).isAfter(date)) {
            index++
            check(++steps < MAX_STEPS) { "month search did not settle for $date" }
        }
        val day = ChronoUnit.DAYS.between(startOf(index), date).toInt() + 1
        val certain = knownByIndex.containsKey(index) && (knownByIndex.containsKey(index + 1) || day <= 29)
        return HijriDate(Math.floorDiv(index, 12), Math.floorMod(index, 12) + 1, day, certain)
    }

    companion object {
        private const val CLAMP_DEPTH = 3
        private const val MAX_STEPS = 12

        /** The calendar with the official months known when this version of the app was built. */
        fun bundled(): HijriCalendar = HijriCalendar(BundledHijriMonths.all)
    }
}
