package work.ahmedhatem.mawaqit.engine

import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Optional local check against the official tables. It runs only when MAWAQIT_ESA_CSV points to a local file with
 * lines `city,date,fajr,sunrise,dhuhr,asr,maghrib,isha` (whole minutes). That data belongs to the Survey Authority and is
 * never stored in this repository; without the variable the test is skipped.
 */
class EsaComparisonTest {
    @Test
    fun matchesTheOfficialTablesAsTheResearchDid() {
        val path = System.getenv("MAWAQIT_ESA_CSV")
        assumeTrue("MAWAQIT_ESA_CSV is not set", path != null)
        val byName = Cities.all.associateBy { it.name }
        var total = 0
        var misses = 0
        var worst = 0
        File(path!!).useLines { lines ->
            for (line in lines.filter { it.isNotBlank() }) {
                val f = line.split(',')
                val city = byName[f[0]] ?: error("unknown city ${f[0]}")
                val day = PrayerCalculator.timesFor(LocalDate.parse(f[1]), city)
                for (k in 0 until 6) {
                    val diff = day.minutes[k] - f[2 + k].toInt()
                    total++
                    if (diff != 0) misses++
                    if (Math.abs(diff) > worst) worst = Math.abs(diff)
                }
            }
        }
        println("official comparison: $total values, $misses different, worst $worst minute(s), exact ${"%.3f".format(100.0 * (total - misses) / total)}%")
        // 78 cities x 365 days = 28470, but two city-days are missing from the downloaded tables.
        assertEquals("number of values compared", 28468 * 6, total)
        assertEquals("different values (research result: 1281)", 1281, misses)
        assertEquals(1, worst)
    }
}
