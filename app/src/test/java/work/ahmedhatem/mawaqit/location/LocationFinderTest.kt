package work.ahmedhatem.mawaqit.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationFinderTest {
    private val minute = 60L * 1_000_000_000L

    @Test
    fun aSavedPositionIsUsedOnlyWhenRecent() {
        assertTrue(LocationFinder.isRecent(0))
        assertTrue(LocationFinder.isRecent(9 * minute))
        assertTrue(LocationFinder.isRecent(10 * minute))                 // the limit itself is still fine
        assertFalse(LocationFinder.isRecent(10 * minute + 1))
        assertFalse(LocationFinder.isRecent(60 * minute))
        assertFalse(LocationFinder.isRecent(-1))                          // a position "from the future" is wrong data
        assertFalse(LocationFinder.isRecent(Long.MAX_VALUE))
    }
}
