package work.ahmedhatem.mawaqit.alerts

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Duration
import java.time.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import work.ahmedhatem.mawaqit.UI_WAIT_MS
import work.ahmedhatem.mawaqit.engine.alerts.AlertKind
import work.ahmedhatem.mawaqit.engine.alerts.AlertRole
import work.ahmedhatem.mawaqit.engine.alerts.AlertTarget
import work.ahmedhatem.mawaqit.engine.alerts.Question

/** The parts that only a real phone can show: the alarm clock, the notification channels and the receiver. */
@RunWith(AndroidJUnit4::class)
class AlertsOnDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val notifications = context.getSystemService(NotificationManager::class.java)

    // A made-up alert: nothing here depends on today's date or on the bundled data.
    private val alert = FiredAlert("device-test", AlertKind.ALARM, AlertTarget.ASR, AlertRole.BEFORE, -30, Question.DHUHR, 1_000L)

    @Before
    fun allowNotifications() {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        AlertNotifications.createChannels(context)
    }

    @After
    fun clean() {
        SystemAlarmBackend(context).cancel()
        notifications.cancel(AlertNotifications.notificationId(alert))
    }

    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().readText()

    private fun ourAlarmPending() = shell("dumpsys alarm").contains(SystemAlarmBackend.ACTION_FIRE)

    @Test
    fun bothChannelsExistWithTheRightImportance() {
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, notifications.getNotificationChannel(AlertNotifications.REMINDERS).importance)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, notifications.getNotificationChannel(AlertNotifications.ALARMS).importance)
    }

    @Test
    fun theAlarmClockHoldsOneAlarmUntilItIsCancelled() {
        val backend = SystemAlarmBackend(context)
        assertFalse("no alarm of ours before the test", ourAlarmPending())
        val exact = backend.schedule(alert, Instant.now().plus(Duration.ofHours(2)))
        // Android 12 and later: exact only when the user allowed it. Either way an alarm is pending.
        assertEquals(SystemAlarmBackend.canScheduleExact(context), exact)
        assertTrue("the alarm is pending", ourAlarmPending())
        backend.cancel()
        assertFalse("the alarm is gone after cancel", ourAlarmPending())
    }

    @Test
    fun theReceiverShowsTheNotificationOfAnAlarmThatFires() {
        val intent = Intent(context, AlertReceiver::class.java).setAction(SystemAlarmBackend.ACTION_FIRE)
        alert.toMap().forEach { (k, v) -> intent.putExtra(k, v) }
        context.sendBroadcast(intent)
        val id = AlertNotifications.notificationId(alert)
        val deadline = System.currentTimeMillis() + UI_WAIT_MS
        var shown = notifications.activeNotifications.firstOrNull { it.id == id }
        while (shown == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(200)
            shown = notifications.activeNotifications.firstOrNull { it.id == id }
        }
        assertTrue("the notification appeared", shown != null)
        val text = AlertTexts.of(alert)
        assertEquals(text.title, shown!!.notification.extras.getString("android.title"))
        assertEquals(text.body, shown.notification.extras.getString("android.text"))
        assertEquals(AlertNotifications.ALARMS, shown.notification.channelId)
    }
}
