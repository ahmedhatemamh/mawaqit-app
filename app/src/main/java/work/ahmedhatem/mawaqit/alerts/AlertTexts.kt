package work.ahmedhatem.mawaqit.alerts

import work.ahmedhatem.mawaqit.engine.alerts.AlertKind
import work.ahmedhatem.mawaqit.engine.alerts.AlertRole
import work.ahmedhatem.mawaqit.engine.alerts.AlertTarget
import work.ahmedhatem.mawaqit.engine.alerts.Question

/** The Arabic words of a notification. Pure, so every wording is tested. */
object AlertTexts {
    class Text(val title: String, val body: String)

    fun of(a: FiredAlert): Text {
        val label = label(a.target)
        val minutes = minutes(kotlin.math.abs(a.offsetMinutes))
        return when {
            a.kind == AlertKind.REMINDER -> Text("تذكير", "$label بعد $minutes")
            a.role == AlertRole.BEFORE ->
                Text(a.question?.let { "هل صليت ${questionName(it)}؟" } ?: "تنبيه", "$label بعد $minutes")
            // At and after the time: "get up to pray" for a prayer, a plain notice for sunrise and the end of the discouraged period.
            else -> {
                val title = if (isPrayer(a.target)) "قم للصلاة" else "تنبيه"
                Text(title, if (a.offsetMinutes == 0) "حان وقت $label" else "مر $minutes على $label")
            }
        }
    }

    fun label(target: AlertTarget): String = if (isPrayer(target)) "صلاة ${target.arabicName}" else target.arabicName

    private fun isPrayer(t: AlertTarget) = t != AlertTarget.SUNRISE && t != AlertTarget.MAKRUH_END

    private fun questionName(q: Question) = when (q) {
        Question.FAJR -> "الفجر"
        Question.DHUHR -> "الظهر"
        Question.ASR -> "العصر"
        Question.MAGHRIB -> "المغرب"
        Question.ISHA -> "العشاء"
        Question.FAJR_AND_DUHA -> "الفجر؟ والضحى"
    }

    /** "دقيقة"، "دقيقتين"، "3 دقائق" ... "11 دقيقة" (Arabic number agreement), for 0..180. */
    fun minutes(n: Int): String = when {
        n == 0 -> "0 دقيقة"
        n == 1 -> "دقيقة"
        n == 2 -> "دقيقتين"
        n in 3..10 -> "$n دقائق"
        else -> "$n دقيقة"
    }
}
