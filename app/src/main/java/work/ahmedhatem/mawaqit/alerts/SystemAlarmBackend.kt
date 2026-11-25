package work.ahmedhatem.mawaqit.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.time.Instant

/** The real alarm clock. One fixed PendingIntent, so setting a new alarm replaces the old one. */
class SystemAlarmBackend(private val context: Context) : AlarmBackend {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    override fun schedule(alert: FiredAlert, at: Instant): Boolean {
        val intent = baseIntent()
        alert.toMap().forEach { (k, v) -> intent.putExtra(k, v) }
        val pending = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (canScheduleExact(context)) {
            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pending)
                return true
            } catch (e: SecurityException) {
                // The permission was taken away between the check and the call: fall through to the approximate alarm.
                Log.w(TAG, "Exact alarm refused, using an approximate one", e)
            }
        }
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pending)
        return false
    }

    override fun cancel() {
        val pending = PendingIntent.getBroadcast(context, REQUEST_CODE, baseIntent(), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        Log.i(TAG, "cancel: pending intent " + if (pending != null) "found" else "not found, nothing to cancel")
        pending?.let { alarms.cancel(it); it.cancel() }
    }

    private fun baseIntent() = Intent(context, AlertReceiver::class.java).setAction(ACTION_FIRE)

    companion object {
        const val ACTION_FIRE = "work.ahmedhatem.mawaqit.ALERT_FIRE"
        private const val REQUEST_CODE = 1
        private const val TAG = "AlertAlarm"

        /** Before Android 12 exact alarms need no permission. */
        fun canScheduleExact(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }
}
