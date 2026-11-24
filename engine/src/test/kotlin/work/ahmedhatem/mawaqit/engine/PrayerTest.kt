package work.ahmedhatem.mawaqit.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerTest {
    @Test
    fun sixTimesInDailyOrder() {
        assertEquals(
            listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"),
            Prayer.entries.map { it.arabicName },
        )
    }

    @Test
    fun arabicNamesAreUniqueAndNotBlank() {
        val names = Prayer.entries.map { it.arabicName }
        assertEquals(names.size, names.toSet().size)
        assertTrue(names.all { it.isNotBlank() })
    }
}
