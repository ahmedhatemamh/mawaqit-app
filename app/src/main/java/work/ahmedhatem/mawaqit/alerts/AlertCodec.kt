package work.ahmedhatem.mawaqit.alerts

import java.time.LocalDate
import java.time.format.DateTimeParseException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import work.ahmedhatem.mawaqit.engine.Prayer
import work.ahmedhatem.mawaqit.engine.alerts.AlertKind
import work.ahmedhatem.mawaqit.engine.alerts.AlertRule
import work.ahmedhatem.mawaqit.engine.alerts.AlertTarget
import work.ahmedhatem.mawaqit.engine.alerts.Recurrence

/** What was read back: the rules that are fine, and how many entries were not (they are left out, never guessed). */
data class DecodedRules(val rules: List<AlertRule>, val skipped: Int)

/** The alert rules as text for the phone's storage. Everything read back is checked again, entry by entry. */
object AlertCodec {
    fun encode(rules: List<AlertRule>): String {
        val array = JSONArray()
        for (r in rules) array.put(encodeRule(r))
        return array.toString()
    }

    /** A text that is not a JSON list at all gives no rules and counts as one skipped entry, so the caller can tell. */
    fun decode(text: String?): DecodedRules {
        if (text.isNullOrEmpty()) return DecodedRules(emptyList(), 0)
        val array = try {
            JSONArray(text)
        } catch (e: JSONException) {
            return DecodedRules(emptyList(), 1)
        }
        val rules = ArrayList<AlertRule>()
        var skipped = 0
        for (i in 0 until array.length()) {
            val rule = (array.opt(i) as? JSONObject)?.let { decodeRule(it) }
            if (rule == null) skipped++ else rules += rule
        }
        return DecodedRules(rules, skipped)
    }

    fun encodePrayed(prayed: Set<Pair<LocalDate, Prayer>>): String {
        val array = JSONArray()
        for ((date, prayer) in prayed.sortedWith(compareBy({ it.first }, { it.second }))) array.put("$date:${prayer.name}")
        return array.toString()
    }

    fun decodePrayed(text: String?): Set<Pair<LocalDate, Prayer>> {
        if (text.isNullOrEmpty()) return emptySet()
        val array = try { JSONArray(text) } catch (e: JSONException) { return emptySet() }
        val out = LinkedHashSet<Pair<LocalDate, Prayer>>()
        for (i in 0 until array.length()) {
            val item = array.opt(i) as? String ?: continue
            val parts = item.split(':')
            if (parts.size != 2) continue
            val date = try { LocalDate.parse(parts[0]) } catch (e: DateTimeParseException) { continue }
            val prayer = Prayer.entries.firstOrNull { it.name == parts[1] } ?: continue
            out += date to prayer
        }
        return out
    }

    private fun encodeRule(r: AlertRule): JSONObject = JSONObject().apply {
        put("id", r.id)
        put("kind", r.kind.name)
        put("targets", JSONArray(r.targets.map { it.name }.sorted()))
        put("offset", r.offsetMinutes)
        put("recurrence", when (val c = r.recurrence) {
            Recurrence.Daily -> JSONObject().put("type", "daily")
            is Recurrence.Range -> JSONObject().put("type", "range").put("from", c.from.toString()).put("to", c.to.toString())
            is Recurrence.Once -> JSONObject().put("type", "once").put("date", c.date.toString())
        })
        put("enabled", r.enabled)
        put("flash", r.flash)
        if (r.soundUri != null) put("sound", r.soundUri)
        put("repeat", r.repeatEveryMinutes)
    }

    /** Null for anything that is not exactly a rule: a missing or wrong-typed field, an unknown name, a bad date. */
    private fun decodeRule(o: JSONObject): AlertRule? {
        return try {
            val id = o.getString("id").takeIf { it.isNotEmpty() } ?: return null
            val kind = AlertKind.entries.firstOrNull { it.name == o.getString("kind") } ?: return null
            val names = o.getJSONArray("targets")
            val targets = LinkedHashSet<AlertTarget>()
            for (i in 0 until names.length()) {
                targets += AlertTarget.entries.firstOrNull { it.name == names.getString(i) } ?: return null
            }
            val rec = o.getJSONObject("recurrence")
            val recurrence = when (rec.getString("type")) {
                "daily" -> Recurrence.Daily
                "range" -> Recurrence.Range(LocalDate.parse(rec.getString("from")), LocalDate.parse(rec.getString("to")))
                "once" -> Recurrence.Once(LocalDate.parse(rec.getString("date")))
                else -> return null
            }
            AlertRule(
                id = id,
                kind = kind,
                targets = targets,
                offsetMinutes = o.getInt("offset"),
                recurrence = recurrence,
                enabled = o.getBoolean("enabled"),
                flash = o.getBoolean("flash"),
                soundUri = if (o.has("sound")) o.getString("sound") else null,
                repeatEveryMinutes = o.getInt("repeat"),
            )
        } catch (e: JSONException) {
            null
        } catch (e: DateTimeParseException) {
            null
        }
    }
}
