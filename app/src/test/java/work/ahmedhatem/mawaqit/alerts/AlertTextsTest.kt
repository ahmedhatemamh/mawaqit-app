package work.ahmedhatem.mawaqit.alerts

import org.junit.Assert.assertEquals
import org.junit.Test
import work.ahmedhatem.mawaqit.engine.alerts.AlertKind
import work.ahmedhatem.mawaqit.engine.alerts.AlertRole
import work.ahmedhatem.mawaqit.engine.alerts.AlertTarget
import work.ahmedhatem.mawaqit.engine.alerts.Question

class AlertTextsTest {
    private fun alert(kind: AlertKind, target: AlertTarget, offset: Int, question: Question? = null) =
        FiredAlert("r", kind, target, if (offset < 0) AlertRole.BEFORE else AlertRole.AFTER, offset, question, 0L)

    private fun text(a: FiredAlert) = AlertTexts.of(a).let { it.title to it.body }

    @Test
    fun reminderBeforeAPrayer() {
        assertEquals("تذكير" to "صلاة الظهر بعد 30 دقيقة", text(alert(AlertKind.REMINDER, AlertTarget.DHUHR, -30)))
        assertEquals("تذكير" to "صلاة العصر بعد 15 دقيقة", text(alert(AlertKind.REMINDER, AlertTarget.ASR, -15)))
    }

    @Test
    fun reminderBeforeSunriseAndTheEndOfTheDiscouragedPeriodHasNoPrayerWord() {
        assertEquals("تذكير" to "الشروق بعد 5 دقائق", text(alert(AlertKind.REMINDER, AlertTarget.SUNRISE, -5)))
        assertEquals("تذكير" to "نهاية وقت الكراهة بعد دقيقة", text(alert(AlertKind.REMINDER, AlertTarget.MAKRUH_END, -1)))
    }

    @Test
    fun alarmBeforeAsksAboutThePreviousPrayer() {
        assertEquals("هل صليت الظهر؟" to "صلاة العصر بعد 20 دقيقة", text(alert(AlertKind.ALARM, AlertTarget.ASR, -20, Question.DHUHR)))
        assertEquals("هل صليت العشاء؟" to "صلاة الفجر بعد 10 دقائق", text(alert(AlertKind.ALARM, AlertTarget.FAJR, -10, Question.ISHA)))
    }

    @Test
    fun theQuestionBeforeDhuhrAsksAboutFajrAndDuha() {
        assertEquals(
            "هل صليت الفجر؟ والضحى؟" to "صلاة الظهر بعد دقيقتين",
            text(alert(AlertKind.ALARM, AlertTarget.DHUHR, -2, Question.FAJR_AND_DUHA)),
        )
    }

    @Test
    fun alarmBeforeTheEndOfTheDiscouragedPeriodAsksNothing() {
        assertEquals("تنبيه" to "نهاية وقت الكراهة بعد 12 دقيقة", text(alert(AlertKind.ALARM, AlertTarget.MAKRUH_END, -12)))
    }

    @Test
    fun alarmAtAndAfterAPrayerSaysGetUp() {
        assertEquals("قم للصلاة" to "حان وقت صلاة المغرب", text(alert(AlertKind.ALARM, AlertTarget.MAGHRIB, 0)))
        assertEquals("قم للصلاة" to "مر 20 دقيقة على صلاة العشاء", text(alert(AlertKind.ALARM, AlertTarget.ISHA, 20)))
        assertEquals("قم للصلاة" to "مر 180 دقيقة على صلاة العشاء", text(alert(AlertKind.ALARM, AlertTarget.ISHA, 180)))
    }

    @Test
    fun alarmAfterSunriseIsAPlainNotice() {
        assertEquals("تنبيه" to "حان وقت الشروق", text(alert(AlertKind.ALARM, AlertTarget.SUNRISE, 0)))
    }

    @Test
    fun minutesFollowArabicNumberAgreement() {
        val expected = mapOf(
            0 to "0 دقيقة", 1 to "دقيقة", 2 to "دقيقتين", 3 to "3 دقائق", 10 to "10 دقائق",
            11 to "11 دقيقة", 30 to "30 دقيقة", 180 to "180 دقيقة",
        )
        for ((n, words) in expected) assertEquals("$n", words, AlertTexts.minutes(n))
    }

    private val plannedAt = 1_000_000_000_000L
    private fun alertAt(role: AlertRole, offset: Int) = FiredAlert("r", AlertKind.ALARM, AlertTarget.ASR, role, offset, if (role == AlertRole.BEFORE) Question.DHUHR else null, plannedAt)
    private fun shown(a: FiredAlert, secondsLate: Long) = AlertTexts.of(a, java.time.Instant.ofEpochMilli(a.atMillis + secondsLate * 1000)).body

    @Test
    fun minutesAreCountedFromTheMomentOfShowingNotFromPlanning() {
        val before = alertAt(AlertRole.BEFORE, -29)
        assertEquals("صلاة العصر بعد 29 دقيقة", shown(before, 0))
        assertEquals("صلاة العصر بعد 29 دقيقة", shown(before, 20)) // a few seconds late is still 29
        assertEquals("صلاة العصر بعد 24 دقيقة", shown(before, 5 * 60))
        assertEquals("حان وقت صلاة العصر", shown(before, 29 * 60)) // delivered at the time itself
        assertEquals("حان وقت صلاة العصر", shown(before, 40 * 60)) // delivered after it
    }

    @Test
    fun afterTheTimeTheMinutesPassedGrowWithTheDelay() {
        val after = alertAt(AlertRole.AFTER, 10)
        assertEquals("مر 10 دقائق على صلاة العصر", shown(after, 0))
        assertEquals("مر 15 دقيقة على صلاة العصر", shown(after, 5 * 60))
        val atTime = alertAt(AlertRole.AFTER, 0)
        assertEquals("حان وقت صلاة العصر", shown(atTime, 0))
        assertEquals("مر 5 دقائق على صلاة العصر", shown(atTime, 5 * 60))
    }
}
