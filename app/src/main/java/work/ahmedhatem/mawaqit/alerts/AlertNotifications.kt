package work.ahmedhatem.mawaqit.alerts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import work.ahmedhatem.mawaqit.R
import work.ahmedhatem.mawaqit.engine.alerts.AlertKind

/** Two channels (the user can change each one in the phone's settings): reminders are light, alarms are loud. */
object AlertNotifications {
    const val REMINDERS = "reminders"
    const val ALARMS = "alarms"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(REMINDERS, "تذكيرات المواقيت", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "إشعار عادي قبل الصلاة: نغمة قصيرة واهتزاز خفيف"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(ALARMS, "منبهات الصلاة", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "منبه «هل صليت؟» و«قم للصلاة»"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
            },
        )
    }

    /** False (nothing shown) when the user has not allowed notifications: the caller reports it, it is not hidden. */
    fun show(context: Context, alert: FiredAlert): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        val text = AlertTexts.of(alert)
        val alarm = alert.kind == AlertKind.ALARM
        val notification = NotificationCompat.Builder(context, if (alarm) ALARMS else REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(text.title)
            .setContentText(text.body)
            .setCategory(if (alarm) Notification.CATEGORY_ALARM else Notification.CATEGORY_REMINDER)
            .setPriority(if (alarm) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission") // checked above with areNotificationsEnabled()
        manager.notify(notificationId(alert), notification)
        return true
    }

    fun notificationId(a: FiredAlert): Int = (a.ruleId + a.target.name + a.atMillis).hashCode()
}
