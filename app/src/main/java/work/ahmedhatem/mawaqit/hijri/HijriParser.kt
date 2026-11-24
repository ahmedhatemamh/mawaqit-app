package work.ahmedhatem.mawaqit.hijri

import java.time.LocalDate
import java.time.format.DateTimeParseException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import work.ahmedhatem.mawaqit.engine.HijriCalendar
import work.ahmedhatem.mawaqit.engine.HijriMonthStart

/** A downloaded or stored Hijri file that must not be used. The message says why (for logs and tests). */
class HijriDataException(message: String) : Exception(message)

/**
 * Reads the official Hijri month starts published at data.ahmedhatem.work/hijri.json.
 *
 * The file is untrusted input: anything unexpected is rejected, never repaired. The month spacing rules (29 to 30 days
 * per month, no duplicates, not empty) are the engine's own, applied by building a [HijriCalendar].
 */
object HijriParser {
    const val MAX_BYTES = 1_048_576
    private const val SCHEMA = 1

    fun parse(text: String): List<HijriMonthStart> {
        val root = try {
            JSONObject(text)
        } catch (e: JSONException) {
            throw HijriDataException("not a JSON object: ${e.message}")
        }
        if (root.opt("schema") != SCHEMA) throw HijriDataException("unknown schema: ${root.opt("schema")}")
        val array = root.opt("months") as? JSONArray ?: throw HijriDataException("months is missing or not a list")
        val months = ArrayList<HijriMonthStart>(array.length())
        for (i in 0 until array.length()) {
            val item = array.opt(i) as? JSONObject ?: throw HijriDataException("month #$i is not an object")
            months.add(month(item, i))
        }
        try {
            HijriCalendar(months)  // empty list, duplicate month, impossible spacing: IllegalArgumentException
        } catch (e: IllegalArgumentException) {
            throw HijriDataException(e.message ?: "invalid months")
        }
        return months
    }

    private fun month(item: JSONObject, i: Int): HijriMonthStart {
        val year = integer(item, "year", i)
        val month = integer(item, "month", i)
        if (year !in 1300..1600) throw HijriDataException("month #$i: year out of range: $year")
        if (month !in 1..12) throw HijriDataException("month #$i: month out of range: $month")
        val start = item.opt("start") as? String ?: throw HijriDataException("month #$i: start is missing or not text")
        val date = try {
            LocalDate.parse(start)
        } catch (e: DateTimeParseException) {
            throw HijriDataException("month #$i: bad start date: $start")
        }
        return HijriMonthStart(year, month, date)
    }

    private fun integer(item: JSONObject, key: String, i: Int): Int {
        val value = item.opt(key)
        if (value !is Int) throw HijriDataException("month #$i: $key is missing or not a whole number")
        return value
    }

    /**
     * Whether [new] may replace [current]. A changed start of a known month is accepted (an official correction), but
     * no known month may disappear and the file may not end earlier than what is already known, so an older or cut-down
     * file never replaces newer data.
     */
    fun checkReplaces(current: List<HijriMonthStart>, new: List<HijriMonthStart>) {
        val newKeys = new.map { it.year * 12 + it.month - 1 }.toSet()
        val missing = current.firstOrNull { it.year * 12 + it.month - 1 !in newKeys }
        if (missing != null) throw HijriDataException("the file lacks the known month ${missing.year}-${missing.month}")
        val currentLast = current.maxOf { it.year * 12 + it.month - 1 }
        if (newKeys.max() < currentLast) throw HijriDataException("the file ends earlier than the known data")
    }
}
