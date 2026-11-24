package work.ahmedhatem.mawaqit.ui

import java.time.DayOfWeek
import java.time.LocalDate
import work.ahmedhatem.mawaqit.engine.HijriDate

/** Arabic text for dates and times, with western digits (decision of 2026-11-25). */
object ArabicFormat {
    private val WEEKDAYS = mapOf(
        DayOfWeek.SATURDAY to "السبت",
        DayOfWeek.SUNDAY to "الأحد",
        DayOfWeek.MONDAY to "الاثنين",
        DayOfWeek.TUESDAY to "الثلاثاء",
        DayOfWeek.WEDNESDAY to "الأربعاء",
        DayOfWeek.THURSDAY to "الخميس",
        DayOfWeek.FRIDAY to "الجمعة",
    )
    private val GREGORIAN_MONTHS = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
    )
    private val HIJRI_MONTHS = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
    )

    fun weekday(date: LocalDate): String = WEEKDAYS.getValue(date.dayOfWeek)

    fun gregorian(date: LocalDate): String = "${date.dayOfMonth} ${GREGORIAN_MONTHS[date.monthValue - 1]} ${date.year}"

    fun hijriMonthName(month: Int): String = HIJRI_MONTHS[month - 1]

    fun hijri(date: HijriDate): String = "${date.day} ${hijriMonthName(date.month)} ${date.year}"

    /** Minutes after midnight as 12-hour time: "4:35 م", "12:05 ص". */
    fun time(minutes: Int): String {
        val m = Math.floorMod(minutes, 24 * 60)
        val hour24 = m / 60
        val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
        return "%d:%02d %s".format(java.util.Locale.ENGLISH, hour12, m % 60, if (hour24 < 12) "ص" else "م")
    }

    /** "بعد 5 دقيقة" style text for a wait in whole minutes (at least 1). */
    fun countdown(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> "$m دقيقة"
            m == 0 -> "$h ساعة"
            else -> "$h ساعة و$m دقيقة"
        }
    }
}
