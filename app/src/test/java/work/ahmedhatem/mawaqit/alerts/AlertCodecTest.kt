package work.ahmedhatem.mawaqit.alerts

import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import work.ahmedhatem.mawaqit.engine.Prayer
import work.ahmedhatem.mawaqit.engine.alerts.AlertKind
import work.ahmedhatem.mawaqit.engine.alerts.AlertRule
import work.ahmedhatem.mawaqit.engine.alerts.AlertTarget
import work.ahmedhatem.mawaqit.engine.alerts.Recurrence

class AlertCodecTest {
    private val day = LocalDate.of(2026, 11, 25)

    private val everything = listOf(
        AlertRule("a", AlertKind.ALARM, setOf(AlertTarget.FAJR, AlertTarget.ISHA), -30, Recurrence.Daily, true, true, "content://media/1", 15),
        AlertRule("b", AlertKind.REMINDER, AlertTarget.entries.toSet(), -15, Recurrence.Range(day, day.plusDays(9)), false, false, null, 10),
        AlertRule("c", AlertKind.ALARM, setOf(AlertTarget.MAKRUH_END), 0, Recurrence.Once(day), true, false, null, 1),
    )

    @Test
    fun everyFieldSurvivesAWriteAndRead() {
        val back = AlertCodec.decode(AlertCodec.encode(everything))
        assertEquals(everything, back.rules)
        assertEquals(0, back.skipped)
    }

    @Test
    fun nothingSavedGivesNoRules() {
        assertEquals(DecodedRules(emptyList(), 0), AlertCodec.decode(null))
        assertEquals(DecodedRules(emptyList(), 0), AlertCodec.decode(""))
        assertEquals(DecodedRules(emptyList(), 0), AlertCodec.decode("[]"))
    }

    @Test
    fun aTextThatIsNotAListCountsAsOneSkippedEntry() {
        assertEquals(DecodedRules(emptyList(), 1), AlertCodec.decode("not json"))
        assertEquals(DecodedRules(emptyList(), 1), AlertCodec.decode("{\"a\":1}"))
    }

    private fun json(change: (JSONObject) -> Unit): String {
        val o = JSONObject(AlertCodec.encode(listOf(everything[0])).let { JSONArray(it).getJSONObject(0).toString() })
        change(o)
        return JSONArray().put(o).toString()
    }

    private fun skipped(change: (JSONObject) -> Unit) {
        val back = AlertCodec.decode(json(change))
        assertTrue(back.rules.isEmpty())
        assertEquals(1, back.skipped)
    }

    @Test
    fun aDamagedEntryIsLeftOutAndTheGoodOnesStay() {
        val array = JSONArray(AlertCodec.encode(everything))
        array.put(JSONObject().put("id", "bad"))
        array.put("junk")
        array.put(42)
        val back = AlertCodec.decode(array.toString())
        assertEquals(everything, back.rules)
        assertEquals(3, back.skipped)
    }

    @Test fun unknownKind() = skipped { it.put("kind", "SIREN") }
    @Test fun unknownTarget() = skipped { it.put("targets", JSONArray(listOf("FAJR", "NOON"))) }
    @Test fun targetsNotAList() = skipped { it.put("targets", "FAJR") }
    @Test fun missingOffset() = skipped { it.remove("offset") }
    @Test fun offsetNotANumber() = skipped { it.put("offset", "soon") }
    @Test fun missingEnabled() = skipped { it.remove("enabled") }
    @Test fun emptyId() = skipped { it.put("id", "") }
    @Test fun missingRecurrence() = skipped { it.remove("recurrence") }
    @Test fun unknownRecurrence() = skipped { it.put("recurrence", JSONObject().put("type", "weekly")) }
    @Test fun badDate() = skipped { it.put("recurrence", JSONObject().put("type", "once").put("date", "2026-13-40")) }
    @Test fun missingRepeat() = skipped { it.remove("repeat") }

    // A value must be exactly the type that was written: no quiet conversion of a fraction, a text or a number.
    @Test fun fractionalOffset() = skipped { it.put("offset", -0.5) }
    @Test fun offsetTooBigForANumber() = skipped { it.put("offset", 99999999999L) }
    @Test fun enabledAsText() = skipped { it.put("enabled", "true") }
    @Test fun flashAsNumber() = skipped { it.put("flash", 1) }
    @Test fun idAsNumber() = skipped { it.put("id", 5) }
    @Test fun kindAsNumber() = skipped { it.put("kind", 3) }
    @Test fun soundAsNumber() = skipped { it.put("sound", 7) }
    @Test fun targetAsNumber() = skipped { it.put("targets", JSONArray().put(1)) }
    @Test fun recurrenceAsText() = skipped { it.put("recurrence", "daily") }
    @Test fun dateAsNumber() = skipped { it.put("recurrence", JSONObject().put("type", "once").put("date", 20261125)) }
    @Test fun missingRangeEnd() = skipped { it.put("recurrence", JSONObject().put("type", "range").put("from", "2026-11-25")) }

    @Test
    fun rulesTheEngineRefusesDoNotComeBack() {
        val ok = AlertRule("ok", AlertKind.ALARM, setOf(AlertTarget.ASR), -30)
        val tooFar = AlertCodec.encode(listOf(ok.copy(id = "far", offsetMinutes = -30)))
        val array = JSONArray(AlertCodec.encode(listOf(ok)))
        array.put(JSONArray(tooFar).getJSONObject(0).put("offset", 181))
        array.put(JSONArray(AlertCodec.encode(listOf(ok))).getJSONObject(0)) // same id as the first
        val back = AlertCodec.decode(array.toString())
        assertEquals(listOf(ok), back.rules)
        assertEquals(2, back.skipped)
    }

    @Test
    fun theFourthAlertBeforeTheSameTimeDoesNotComeBack() {
        val four = (1..4).map { AlertRule("r$it", AlertKind.ALARM, setOf(AlertTarget.ASR), -it * 10) }
        val back = AlertCodec.decode(AlertCodec.encode(four))
        assertEquals(four.take(3), back.rules)
        assertEquals(1, back.skipped)
    }

    @Test
    fun extraFieldsAreIgnoredAndTheSoundIsOptional() {
        val back = AlertCodec.decode(json { it.put("future", "x"); it.remove("sound") })
        assertEquals(1, back.rules.size)
        assertEquals(null, back.rules.single().soundUri)
    }

    @Test
    fun theAnswersISPrayedSurviveAndBadOnesAreDropped() {
        val set = setOf(day to Prayer.ASR, day.minusDays(1) to Prayer.FAJR)
        assertEquals(set, AlertCodec.decodePrayed(AlertCodec.encodePrayed(set)))
        val mixed = JSONArray().put("2026-11-25:ASR").put("2026-11-25:NOON").put("nope").put("2026-99-99:ASR").put(7).toString()
        assertEquals(setOf(day to Prayer.ASR), AlertCodec.decodePrayed(mixed))
        assertEquals(emptySet<Pair<LocalDate, Prayer>>(), AlertCodec.decodePrayed("not json"))
        assertEquals(emptySet<Pair<LocalDate, Prayer>>(), AlertCodec.decodePrayed(null))
    }
}
