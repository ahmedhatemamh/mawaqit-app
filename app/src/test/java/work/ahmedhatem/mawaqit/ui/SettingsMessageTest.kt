package work.ahmedhatem.mawaqit.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import work.ahmedhatem.mawaqit.R
import work.ahmedhatem.mawaqit.settings.SaveResult

class SettingsMessageTest {
    @Test
    fun aFailedSaveShowsTheMessageAndAFollowingSuccessClearsIt() {
        var message: Int? = null
        message = messageAfterSave(SaveResult.STORAGE_FAILED, message)
        assertEquals(R.string.save_failed, message)
        message = messageAfterSave(SaveResult.STORAGE_FAILED, message)       // failing again keeps it
        assertEquals(R.string.save_failed, message)
        message = messageAfterSave(SaveResult.SAVED, message)                // the retry works
        assertNull(message)
    }

    @Test
    fun otherMessagesAreNotTouchedByASuccessfulOrRefusedSave() {
        assertEquals(R.string.location_done, messageAfterSave(SaveResult.SAVED, R.string.location_done))
        assertEquals(R.string.location_off, messageAfterSave(SaveResult.INVALID, R.string.location_off))
        assertNull(messageAfterSave(SaveResult.SAVED, null))
        assertNull(messageAfterSave(SaveResult.INVALID, null))
        assertEquals(R.string.save_failed, messageAfterSave(SaveResult.INVALID, R.string.save_failed))
    }

    @Test
    fun aFailureReplacesAnotherMessage() {
        assertEquals(R.string.save_failed, messageAfterSave(SaveResult.STORAGE_FAILED, R.string.location_done))
    }
}
