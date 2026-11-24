package work.ahmedhatem.mawaqit.engine

import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
 * always within one day) and marked not certain. Estimated months are chained from the nearest official month so that
 * every month is 29 or 30 days long: each estimated start is limited to 29 or 30 days after the month before it, and,
 * when an official month follows the gap, to what still leaves 29 or 30 days per month up to that official start.
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
    // Caches, guarded by `this`.
    private val anchorLunations = HashMap<Int, Long>()
    private val rawStarts = HashMap<Int, LocalDate>()
    private val chained = HashMap<Int, LocalDate>()

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

    private fun rawStart(index: Int): LocalDate = rawStarts.getOrPut(index) {
        val anchor = nearestByIndex(index)
        val anchorLunation = anchorLunations.getOrPut(anchor.index) { CrescentEstimator.lunationOfStart(anchor.start) }
        rawEstimate(anchorLunation + (index - anchor.index))
    }

    private fun limit(raw: LocalDate, low: LocalDate, high: LocalDate, index: Int): LocalDate {
        check(!low.isAfter(high)) { "no valid start for month number $index between $low and $high" }
        return if (raw.isBefore(low)) low else if (raw.isAfter(high)) high else raw
    }

    /**
     * First day of a month: official, or estimated and chained from the nearest official month. Estimates that
     * already fit (29 or 30 days after the month before, and, before an official month, 29 or 30 days per month up to
     * it) are kept as they are.
     */
    @Synchronized
    private fun startOf(index: Int): LocalDate {
        knownByIndex[index]?.let { return it }
        chained[index]?.let { return it }
        val before = known.lastOrNull { it.index < index }
        val after = known.firstOrNull { it.index > index }
        if (before != null) {
            var previous = before.start
            for (i in before.index + 1..index) {
                val cached = chained[i]
                if (cached != null) {
                    previous = cached
                    continue
                }
                var low = previous.plusDays(29)
                var high = previous.plusDays(30)
                if (after != null) {
                    val monthsLeft = (after.index - i).toLong()
                    val lowFromAfter = after.start.minusDays(30 * monthsLeft)
                    val highFromAfter = after.start.minusDays(29 * monthsLeft)
                    if (lowFromAfter.isAfter(low)) low = lowFromAfter
                    if (highFromAfter.isBefore(high)) high = highFromAfter
                }
                previous = limit(rawStart(i), low, high, i)
                chained[i] = previous
            }
        } else {
            var following = after!!.start
            for (i in after.index - 1 downTo index) {
                val cached = chained[i]
                if (cached != null) {
                    following = cached
                    continue
                }
                following = limit(rawStart(i), following.minusDays(30), following.minusDays(29), i)
                chained[i] = following
            }
        }
        return chained.getValue(index)
    }

    /** First day of a Hijri month, and whether it is official. */
    fun startOfMonth(year: Int, month: Int): Pair<LocalDate, Boolean> {
        require(month in 1..12) { "Hijri month out of range: $month" }
        val index = year * 12 + month - 1
        return Pair(startOf(index), knownByIndex.containsKey(index))
    }

    /**
     * The number of the last month whose start is not after [date]. Every month lasts 29 or 30 days, so the answer
     * lies between two numbers that follow from the nearest official start, and month starts only grow with the number:
     * a binary search between those two always finds it, however far the date is from the official months.
     */
    private fun monthIndexOf(date: LocalDate): Int {
        val first = known.first()
        var low: Int
        var high: Int
        if (date.isBefore(first.start)) {
            val days = ChronoUnit.DAYS.between(date, first.start)  // at least 1
            low = first.index - (-Math.floorDiv(-days, 29L)).toInt()  // ceiling of days/29: its start is at most days/29 months before: not after the date
            high = first.index                                      // an official start, after the date
        } else {
            val official = known.last { !it.start.isAfter(date) }
            val days = ChronoUnit.DAYS.between(official.start, date)
            low = official.index + (days / 30).toInt()               // 30 days per month at most: not after the date
            high = official.index + (days / 29).toInt() + 1          // 29 days per month at least: after the date
            known.firstOrNull { it.index > official.index }?.let { high = minOf(high, it.index) }
        }
        while (high - low > 1) {
            val middle = low + (high - low) / 2
            if (startOf(middle).isAfter(date)) high = middle else low = middle
        }
        return low
    }

    fun dateOf(date: LocalDate): HijriDate {
        val index = monthIndexOf(date)
        val day = ChronoUnit.DAYS.between(startOf(index), date).toInt() + 1
        val certain = knownByIndex.containsKey(index) && (knownByIndex.containsKey(index + 1) || day <= 29)
        return HijriDate(Math.floorDiv(index, 12), Math.floorMod(index, 12) + 1, day, certain)
    }

    companion object {
        /** The calendar with the official months known when this version of the app was built. */
        fun bundled(): HijriCalendar = HijriCalendar(BundledHijriMonths.all)
    }
}
