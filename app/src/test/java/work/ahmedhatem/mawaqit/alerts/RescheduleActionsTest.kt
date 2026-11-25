package work.ahmedhatem.mawaqit.alerts

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RescheduleActionsTest {
    private val manifest = File("src/main/AndroidManifest.xml").readText()

    private fun actionsOfRescheduleReceiver(): Set<String> {
        val receiver = manifest.substringAfter("android:name=\".alerts.RescheduleReceiver\"").substringBefore("</receiver>")
        return Regex("<action android:name=\"([^\"]+)\"").findAll(receiver).map { it.groupValues[1] }.toSet()
    }

    @Test
    fun theReceiverAnswersExactlyTheBroadcastsTheManifestAsksFor() {
        assertEquals(RescheduleReceiver.ACTIONS, actionsOfRescheduleReceiver())
    }

    @Test
    fun allowingExactAlarmsPlansAgain() {
        assertTrue("android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" in actionsOfRescheduleReceiver())
    }
}
