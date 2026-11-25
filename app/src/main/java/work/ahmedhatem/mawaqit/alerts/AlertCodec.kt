package work.ahmedhatem.mawaqit.alerts

import java.time.LocalDate
import java.time.format.DateTimeParseException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import work.ahmedhatem.mawaqit.engine.Prayer
import work.ahmedhatem.mawaqit.engine.alerts.AlertKind
import work.ahmedhatem.mawaqit.engine.alerts.AlertRule
import work.ahmedhatem.mawaqit.engine.alerts.AlertRules
import work.ahmedhatem.mawaqit.engine.alerts.AlertTarget
import work.ahmedhatem.mawaqit.engine.alerts.Recurrence

/**
 * What was read back: only rules that pass [AlertRules.problems] together as one list, and how many entries were left
 * out (damaged ones and ones that break a rule, never guessed or repaired).
 */
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
        val accepted = ArrayList<AlertRule>()
        var skipped = 0
        for (i in 0 until array.length()) {
            val rule = (array.opt(i) as? JSONObject)?.let { decodeRule(it) }
            if (rule == null) {
                skipped++
                continue
            }
            // Each rule is checked against the rules already accepted, so a refused rule never takes a place in the
            // limit of three or holds an id that a later good rule needs.
            val refused = AlertRules.problemsByIndex(accepted + rule).any { it.first == accepted.size }
            if (refused) skipped++ else accepted += rule
        }
        return DecodedRules(accepted, skipped)
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

    // The org.json getters change types quietly (a fraction becomes 0, the text "true" becomes true, a number becomes
    // text). A stored value must be exactly the type that was written, so each field is read raw and checked.
    private fun JSONObject.text(key: String): String? = opt(key) as? String

    private fun JSONObject.whole(key: String): Int? = when (val v = opt(key)) {
        is Int -> v
        is Long -> if (v in Int.MIN_VALUE..Int.MAX_VALUE) v.toInt() else null
        else -> null
    }

    private fun JSONObject.flag(key: String): Boolean? = opt(key) as? Boolean

    /** Null for anything that is not exactly a rule: a missing or wrong-typed field, an unknown name, a bad date. */
    private fun decodeRule(o: JSONObject): AlertRule? {
        return try {
            val id = o.text("id")?.takeIf { it.isNotEmpty() } ?: return null
            val kind = AlertKind.entries.firstOrNull { it.name == o.text("kind") } ?: return null
            val names = o.opt("targets") as? JSONArray ?: return null
            val targets = LinkedHashSet<AlertTarget>()
            for (i in 0 until names.length()) {
                val name = names.opt(i) as? String ?: return null
                targets += AlertTarget.entries.firstOrNull { it.name == name } ?: return null
            }
            val rec = o.opt("recurrence") as? JSONObject ?: return null
            val recurrence = when (rec.text("type")) {
                "daily" -> Recurrence.Daily
                "range" -> Recurrence.Range(LocalDate.parse(rec.text("from") ?: return null), LocalDate.parse(rec.text("to") ?: return null))
                "once" -> Recurrence.Once(LocalDate.parse(rec.text("date") ?: return null))
                else -> return null
            }
            val sound = if (o.has("sound")) (o.text("sound") ?: return null) else null
            AlertRule(
                id = id,
                kind = kind,
                targets = targets,
                offsetMinutes = o.whole("offset") ?: return null,
                recurrence = recurrence,
                enabled = o.flag("enabled") ?: return null,
                flash = o.flag("flash") ?: return null,
                soundUri = sound,
                repeatEveryMinutes = o.whole("repeat") ?: return null,
            )
        } catch (e: DateTimeParseException) {
            null
        }
    }
}
