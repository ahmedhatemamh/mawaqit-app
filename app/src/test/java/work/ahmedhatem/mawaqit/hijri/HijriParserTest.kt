package work.ahmedhatem.mawaqit.hijri

import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Test
import work.ahmedhatem.mawaqit.engine.HijriCalendar
import work.ahmedhatem.mawaqit.engine.HijriMonthStart

class HijriParserTest {
    private fun rejected(text: String): String =
        assertThrows(HijriDataException::class.java) { HijriParser.parse(text) }.message ?: ""

    @Test
    fun normalFileGivesAllMonths() {
        val months = HijriParser.parse(hijriJson())
        assertEquals(HijriCalendar.bundledMonths(), months)
        assertEquals(LocalDate.of(2026, 11, 11), months.last().start)
    }

    @Test
    fun extraFieldsAreIgnored() {
        val text = hijriJson().replace("\"schema\":1", "\"schema\":1,\"future\":{\"a\":[1,2]}")
        assertEquals(133, HijriParser.parse(text).size)
    }

    @Test
    fun notJson() {
        rejected("")
        rejected("<html>maintenance</html>")
        rejected("[]")
        rejected("{\"schema\":1,\"months\":[")
    }

    @Test
    fun wrongOrMissingSchema() {
        rejected(hijriJson(schema = 2))
        rejected(hijriJson(schema = 0))
        rejected("{\"months\":[]}")
        rejected(hijriJson().replace("\"schema\":1", "\"schema\":\"1\""))
    }

    @Test
    fun monthsMissingOrWrongType() {
        rejected("{\"schema\":1}")
        rejected("{\"schema\":1,\"months\":{}}")
        rejected("{\"schema\":1,\"months\":[]}")
        rejected("{\"schema\":1,\"months\":[5]}")
    }

    private fun one(year: String, month: String, start: String) =
        "{\"schema\":1,\"months\":[{\"year\":$year,\"month\":$month,\"start\":$start}]}"

    @Test
    fun badFieldsOfAMonth() {
        assertEquals(1, HijriParser.parse(one("1446", "9", "\"2025-03-01\"")).size)
        rejected(one("\"1446\"", "9", "\"2025-03-01\""))   // text instead of number
        rejected(one("1446.5", "9", "\"2025-03-01\""))     // not a whole number
        rejected(one("null", "9", "\"2025-03-01\""))
        rejected(one("1446", "13", "\"2025-03-01\""))
        rejected(one("1446", "0", "\"2025-03-01\""))
        rejected(one("99999999999", "9", "\"2025-03-01\""))
        rejected(one("1100", "9", "\"2025-03-01\""))
        rejected(one("1446", "9", "\"2025-02-30\""))
        rejected(one("1446", "9", "\"01/03/2025\""))
        rejected(one("1446", "9", "20250301"))
        rejected("{\"schema\":1,\"months\":[{\"year\":1446,\"start\":\"2025-03-01\"}]}")
    }

    @Test
    fun duplicateMonthAndImpossibleSpacing() {
        val first = HijriMonthStart(1446, 9, LocalDate.of(2025, 3, 1))
        rejected(hijriJson(listOf(first, first)))
        rejected(hijriJson(listOf(first, HijriMonthStart(1446, 10, LocalDate.of(2025, 3, 5)))))
        rejected(hijriJson(listOf(first, HijriMonthStart(1446, 10, LocalDate.of(2025, 5, 1)))))
        assertEquals(2, HijriParser.parse(hijriJson(listOf(first, HijriMonthStart(1446, 10, LocalDate.of(2025, 3, 30))))).size)
    }

    @Test
    fun replacementMayCorrectAKnownStartButNotLoseData() {
        val current = HijriCalendar.bundledMonths()
        HijriParser.checkReplaces(current, current)
        HijriParser.checkReplaces(current, bundledPlusOne())
        // a corrected start of an existing month is accepted (decision 2a)
        val corrected = current.map { if (it == current[10]) it.copy(start = it.start.plusDays(1)) else it }
        HijriParser.checkReplaces(current, corrected)
        // a missing month, an older file, a cut-down file are refused
        assertTrue(assertThrows(HijriDataException::class.java) {
            HijriParser.checkReplaces(current, current.filterIndexed { i, _ -> i != 50 })
        }.message!!.contains("lacks"))
        assertThrows(HijriDataException::class.java) { HijriParser.checkReplaces(bundledPlusOne(), current) }
        assertThrows(HijriDataException::class.java) { HijriParser.checkReplaces(current, current.take(10)) }
    }

    /** Run against the real published file: MAWAQIT_HIJRI_JSON=/path/to/hijri.json ./gradlew :app:testDebugUnitTest */
    @Test
    fun realPublishedFileIfProvided() {
        val path = System.getenv("MAWAQIT_HIJRI_JSON")
        assumeNotNull(path)
        val months = HijriParser.parse(File(path).readText())
        HijriParser.checkReplaces(HijriCalendar.bundledMonths(), months)
        assertTrue(months.size >= 133)
    }
}
