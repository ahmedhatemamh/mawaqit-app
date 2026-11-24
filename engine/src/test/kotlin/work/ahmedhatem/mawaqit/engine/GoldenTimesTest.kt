package work.ahmedhatem.mawaqit.engine

import java.time.LocalDate
import java.util.zip.GZIPInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Kotlin engine against the research code (research/esa-formula), value by value.
 * The reference file holds only our own computed values: every day of 2026 for the 78 cities, plus a sample of years
 * from 1950 to 2100 (including the years around the summer-time rule changes) for six cities.
 */
class GoldenTimesTest {
    private class Row(val lat: Double, val lon: Double, val date: LocalDate, val exact: List<Double>, val minutes: List<Int>)

    private fun rows(): List<Row> {
        val stream = javaClass.getResourceAsStream("/golden_times.csv.gz") ?: error("golden_times.csv.gz is missing")
        GZIPInputStream(stream).bufferedReader(Charsets.UTF_8).useLines { lines ->
            return lines.drop(1).filter { it.isNotBlank() }.map { line ->
                val f = line.split(',')
                check(f.size == 15) { "bad golden line: $line" }
                Row(f[0].toDouble(), f[1].toDouble(), LocalDate.parse(f[2]),
                    f.subList(3, 9).map { it.toDouble() }, f.subList(9, 15).map { it.toInt() })
            }.toList()
        }
    }

    @Test
    fun exactMinutesMatchTheResearchCode() {
        val all = rows()
        assertTrue("golden file is unexpectedly small: ${all.size}", all.size > 30000)
        var worst = 0.0
        for (r in all) {
            val got = PrayerCalculator.timesFor(r.date, r.lat, r.lon).exactMinutes
            for (k in 0 until 6) {
                val diff = Math.abs(got[k] - r.exact[k])
                if (diff > worst) worst = diff
                assertTrue("${r.date} (${r.lat}, ${r.lon}) time #$k: kotlin ${got[k]} vs research ${r.exact[k]}", diff < 1e-4)
            }
        }
        println("golden: ${all.size} days compared, worst difference $worst minutes")
    }

    @Test
    fun roundedMinutesMatchExceptWhenAValueSitsOnAHalfMinute() {
        var onTheEdge = 0
        var compared = 0
        for (r in rows()) {
            val day = PrayerCalculator.timesFor(r.date, r.lat, r.lon)
            for (k in 0 until 6) {
                val distanceToHalf = Math.abs((r.exact[k] - Math.floor(r.exact[k])) - 0.5)
                if (distanceToHalf < 2e-6) { onTheEdge++; continue }  // a last-digit difference may round either way
                compared++
                assertEquals("${r.date} (${r.lat}, ${r.lon}) time #$k", r.minutes[k], day.minutes[k])
            }
        }
        println("golden rounding: $compared compared, $onTheEdge skipped on a half-minute edge")
        assertTrue("too many edge cases: $onTheEdge", onTheEdge < 20)
    }
}
