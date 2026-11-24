package work.ahmedhatem.mawaqit.hijri

import org.json.JSONArray
import org.json.JSONObject
import work.ahmedhatem.mawaqit.engine.HijriCalendar
import work.ahmedhatem.mawaqit.engine.HijriMonthStart

/** A valid Hijri file with the given months, in the published format. */
fun hijriJson(months: List<HijriMonthStart> = HijriCalendar.bundledMonths(), schema: Int = 1): String {
    val list = JSONArray()
    for (m in months) {
        list.put(JSONObject().put("year", m.year).put("month", m.month).put("start", m.start.toString())
            .put("length", JSONObject.NULL).put("sources", JSONArray().put("dar-alifta-bayan")))
    }
    return JSONObject().put("schema", schema).put("months", list)
        .put("latest", JSONObject().put("date", "2026-11-23")).toString()
}

/** The bundled months plus [extra] more official months after the last one (30 days each). */
fun bundledPlus(extra: Int): List<HijriMonthStart> {
    val months = HijriCalendar.bundledMonths().toMutableList()
    repeat(extra) {
        val last = months.last()
        months += if (last.month == 12) HijriMonthStart(last.year + 1, 1, last.start.plusDays(30))
        else HijriMonthStart(last.year, last.month + 1, last.start.plusDays(30))
    }
    return months
}

fun bundledPlusOne(): List<HijriMonthStart> = bundledPlus(1)
