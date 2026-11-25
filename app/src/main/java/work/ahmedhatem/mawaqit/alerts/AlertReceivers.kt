package work.ahmedhatem.mawaqit.alerts

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import work.ahmedhatem.mawaqit.settings.settingsStore

/** Plans the next alarm from what is saved on the phone. */
suspend fun rescheduleFromStores(context: Context, now: Instant = Instant.now()): Plan = planFromStores(context, now, null).first

/** Plans the next alarm, and finds the other alerts due at the same minute as [fired] (empty when [fired] is null). */
private suspend fun planFromStores(context: Context, now: Instant, fired: FiredAlert?): Pair<Plan, List<FiredAlert>> {
    val app = context.applicationContext
    val rules = alertStore(app).rules.first().rules
    val prayed = alertStore(app).prayed.first()
    val settings = settingsStore(app).settings.first()
    val scheduler = AlertScheduler(SystemAlarmBackend(app))
    val plan = scheduler.reschedule(rules, settings, prayed, now)
    return plan to (fired?.let { scheduler.companionsOf(it, rules, settings, prayed) } ?: emptyList())
}

/**
 * The alarm went off: plan the next one first (so a failure while showing cannot stop the chain), then show this one and
 * any other alert due at the same minute. If planning fails the alerts are still shown, and then the failure is thrown
 * (not hidden), so a problem in planning never costs a prayer's notification.
 */
class AlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SystemAlarmBackend.ACTION_FIRE) return
        val alert = FiredAlert.fromMap(
            listOf(FiredAlert.ID, FiredAlert.KIND, FiredAlert.TARGET, FiredAlert.ROLE, FiredAlert.OFFSET, FiredAlert.QUESTION, FiredAlert.AT)
                .associateWith { intent.getStringExtra(it) },
        )
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val from = maxOf(Instant.now(), alert?.let { Instant.ofEpochMilli(it.atMillis) } ?: Instant.EPOCH)
                val planning = runCatching { planFromStores(context, from, alert) }
                if (alert == null) Log.w(TAG, "Alarm fired with a damaged alert; nothing shown")
                val toShow = listOfNotNull(alert) + (planning.getOrNull()?.second ?: emptyList())
                for (a in toShow) {
                    if (!AlertNotifications.show(context, a)) Log.w(TAG, "Notifications are not allowed; alert ${a.ruleId} not shown")
                }
                planning.exceptionOrNull()?.let { throw it }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AlertReceiver"
    }
}

/** The phone restarted, the app was updated, the clock or time zone changed, or exact alarms were allowed: the pending alarm is gone or wrong, plan again. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                rescheduleFromStores(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            // Sent when the user allows exact alarms: an approximate alarm set meanwhile is replaced by an exact one.
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
