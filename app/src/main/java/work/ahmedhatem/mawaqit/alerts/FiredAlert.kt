package work.ahmedhatem.mawaqit.alerts

import java.time.Duration
import work.ahmedhatem.mawaqit.engine.alerts.AlertKind
import work.ahmedhatem.mawaqit.engine.alerts.AlertRole
import work.ahmedhatem.mawaqit.engine.alerts.AlertTarget
import work.ahmedhatem.mawaqit.engine.alerts.Occurrence
import work.ahmedhatem.mawaqit.engine.alerts.Question

/**
 * What the alarm clock hands back when an alert fires: just enough to show the notification without reading the stores.
 * [offsetMinutes] is negative before the time and 0 or more after it. It travels as plain strings so it can be checked.
 */
data class FiredAlert(
    val ruleId: String,
    val kind: AlertKind,
    val target: AlertTarget,
    val role: AlertRole,
    val offsetMinutes: Int,
    val question: Question?,
    /** When the alert was meant to fire, in milliseconds since 1970. */
    val atMillis: Long,
) {
    fun toMap(): Map<String, String> = buildMap {
        put(ID, ruleId)
        put(KIND, kind.name)
        put(TARGET, target.name)
        put(ROLE, role.name)
        put(OFFSET, offsetMinutes.toString())
        question?.let { put(QUESTION, it.name) }
        put(AT, atMillis.toString())
    }

    companion object {
        const val ID = "rule"
        const val KIND = "kind"
        const val TARGET = "target"
        const val ROLE = "role"
        const val OFFSET = "offset"
        const val QUESTION = "question"
        const val AT = "at"

        fun of(o: Occurrence) = FiredAlert(
            o.ruleId, o.kind, o.target, o.role,
            Duration.between(o.targetAt, o.at).toMinutes().toInt(), o.question, o.at.toEpochMilli(),
        )

        /** Null when anything is missing or not one of the known values: a damaged alert shows nothing. */
        fun fromMap(m: Map<String, String?>): FiredAlert? {
            val id = m[ID]?.takeIf { it.isNotEmpty() } ?: return null
            val kind = m[KIND]?.let { v -> AlertKind.entries.firstOrNull { it.name == v } } ?: return null
            val target = m[TARGET]?.let { v -> AlertTarget.entries.firstOrNull { it.name == v } } ?: return null
            val role = m[ROLE]?.let { v -> AlertRole.entries.firstOrNull { it.name == v } } ?: return null
            val offset = m[OFFSET]?.toIntOrNull() ?: return null
            val at = m[AT]?.toLongOrNull() ?: return null
            val question = m[QUESTION]?.let { v -> Question.entries.firstOrNull { it.name == v } ?: return null }
            if ((offset < 0) != (role == AlertRole.BEFORE)) return null
            return FiredAlert(id, kind, target, role, offset, question, at)
        }
    }
}
