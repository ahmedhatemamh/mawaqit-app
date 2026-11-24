package work.ahmedhatem.mawaqit.engine

import java.time.LocalDate
import java.util.zip.GZIPInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Kotlin lunar model and crescent estimator against the research code (research/hijri-estimate), value by value.
 * The reference files hold only our own computed values.
 */
class GoldenHijriTest {
    private fun lines(name: String): List<List<String>> {
        val stream = javaClass.getResourceAsStream(name) ?: error("$name is missing")
        GZIPInputStream(stream).bufferedReader(Charsets.UTF_8).useLines { all ->
            return all.drop(1).filter { it.isNotBlank() }.map { it.split(',') }.toList()
        }
    }

    @Test
    fun moonPositionsMatchTheResearchCode() {
        val rows = lines("/golden_moon.csv.gz")
        assertEquals(400, rows.size)
        var worst = 0.0
        for (r in rows) {
            val p = Lunar.moon(r[0].toDouble())
            val d = maxOf(Math.abs(p.longitude - r[1].toDouble()), Math.abs(p.latitude - r[2].toDouble()))
            worst = maxOf(worst, d)
            assertTrue("jde ${r[0]}: longitude/latitude differ by $d degrees", d < 1e-6)
            assertEquals("jde ${r[0]} distance", r[3].toDouble(), p.distanceKm, 1e-3)
        }
        println("golden moon: ${rows.size} positions, worst angular difference $worst degrees")
    }

    @Test
    fun estimatedMonthStartsMatchTheResearchCode() {
        val rows = lines("/golden_hijri.csv.gz")
        assertTrue("golden file is unexpectedly small: ${rows.size}", rows.size > 3000)
        var skipped = 0
        for (r in rows) {
            if (r[2].toDouble() < 1e-5) { skipped++; continue }  // a last-digit difference could flip the threshold
            assertEquals("lunation ${r[0]}", LocalDate.parse(r[1]), CrescentEstimator.estimatedStart(r[0].toLong()))
        }
        println("golden hijri: ${rows.size} months compared, $skipped skipped on the threshold edge")
        assertTrue("too many edge cases: $skipped", skipped < 5)
    }

    @Test
    fun theEstimateAgreesWithMostOfficialMonthStartsAndIsNeverMoreThanADayOff() {
        // Measured in research/hijri-estimate: 122 of 133 exactly, the others one day off.
        val months = BundledHijriMonths.all
        var exact = 0
        for (m in months) {
            val estimate = CrescentEstimator.estimatedStart(CrescentEstimator.lunationOfStart(m.start))
            val off = java.time.temporal.ChronoUnit.DAYS.between(m.start, estimate)
            assertTrue("${m.year}-${m.month}: estimate $estimate vs official ${m.start}", Math.abs(off) <= 1)
            if (off == 0L) exact++
        }
        println("official month starts reproduced exactly: $exact of ${months.size}")
        assertTrue("only $exact of ${months.size} reproduced", exact >= 120)
    }
}
