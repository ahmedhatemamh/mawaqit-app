package work.ahmedhatem.mawaqit.ui

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import work.ahmedhatem.mawaqit.engine.HijriDate

class ArabicFormatTest {
    @Test
    fun timeIsTwelveHourWithWesternDigits() {
        assertEquals("12:00 ص", ArabicFormat.time(0))
        assertEquals("12:05 ص", ArabicFormat.time(5))
        assertEquals("11:59 ص", ArabicFormat.time(11 * 60 + 59))
        assertEquals("12:00 م", ArabicFormat.time(12 * 60))
        assertEquals("4:35 م", ArabicFormat.time(16 * 60 + 35))
        assertEquals("11:59 م", ArabicFormat.time(23 * 60 + 59))
        assertEquals("4:05 ص", ArabicFormat.time(4 * 60 + 5))
    }

    @Test
    fun timeOfNextDayWrapsAround() {
        assertEquals("12:00 ص", ArabicFormat.time(24 * 60))
        assertEquals("1:30 ص", ArabicFormat.time(24 * 60 + 90))
    }

    @Test
    fun timeDoesNotDependOnThePhoneLanguage() {
        val saved = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale("ar", "EG"))
            assertEquals("4:35 م", ArabicFormat.time(16 * 60 + 35))
        } finally {
            java.util.Locale.setDefault(saved)
        }
    }

    @Test
    fun countdownWords() {
        assertEquals("1 دقيقة", ArabicFormat.countdown(1))
        assertEquals("45 دقيقة", ArabicFormat.countdown(45))
        assertEquals("1 ساعة", ArabicFormat.countdown(60))
        assertEquals("2 ساعة و5 دقيقة", ArabicFormat.countdown(125))
        assertEquals("23 ساعة و59 دقيقة", ArabicFormat.countdown(23 * 60 + 59))
    }

    @Test
    fun datesInArabic() {
        val d = LocalDate.of(2026, 11, 25)   // a Wednesday
        assertEquals("الأربعاء", ArabicFormat.weekday(d))
        assertEquals("25 نوفمبر 2026", ArabicFormat.gregorian(d))
        assertEquals("13 جمادى الآخرة 1448", ArabicFormat.hijri(HijriDate(1448, 6, 13, true)))
    }

    @Test
    fun everyWeekdayAndMonthHasAName() {
        val names = (0L until 7).map { ArabicFormat.weekday(LocalDate.of(2026, 11, 22).plusDays(it)) }
        assertEquals(7, names.toSet().size)
        assertEquals(12, (1..12).map { ArabicFormat.gregorian(LocalDate.of(2026, it, 1)) }.toSet().size)
        assertEquals(12, (1..12).map { ArabicFormat.hijriMonthName(it) }.toSet().size)
        assertTrue(names.all { it.isNotBlank() })
    }
}
