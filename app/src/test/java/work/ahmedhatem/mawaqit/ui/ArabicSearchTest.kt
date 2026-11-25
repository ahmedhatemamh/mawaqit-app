package work.ahmedhatem.mawaqit.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import work.ahmedhatem.mawaqit.engine.Cities

class ArabicSearchTest {
    @Test
    fun spellingVariantsMatch() {
        assertTrue(ArabicSearch.matches("الأسكندرية", "اسكندرية"))
        assertTrue(ArabicSearch.matches("الأسكندرية", "الإسكندرية"))
        assertTrue(ArabicSearch.matches("القاهرة", "القاهره"))
        assertTrue(ArabicSearch.matches("القاهرة", "  قاه  "))
        assertTrue(ArabicSearch.matches("القاهرة", ""))
        assertFalse(ArabicSearch.matches("القاهرة", "أسوان"))
    }

    @Test
    fun everyCityFindsItselfAndTheListShrinks() {
        for (c in Cities.all) assertTrue(c.name, Cities.all.filter { ArabicSearch.matches(it.name, c.name) }.any { it.name == c.name })
        assertEquals(78, Cities.all.count { ArabicSearch.matches(it.name, "") })
        assertTrue(Cities.all.count { ArabicSearch.matches(it.name, "اس") } in 1..77)
    }

    @Test
    fun nothingMatchesAnUnknownName() {
        assertEquals(0, Cities.all.count { ArabicSearch.matches(it.name, "لندن") })
    }
}
