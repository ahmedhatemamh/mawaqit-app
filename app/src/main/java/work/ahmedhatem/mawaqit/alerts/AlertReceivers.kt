package work.ahmedhatem.mawaqit.alerts

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
suspend fun rescheduleFromStores(context: Context, now: Instant = Instant.now()): Plan {
    val app = context.applicationContext
    val rules = alertStore(app).rules.first().rules
    val prayed = alertStore(app).prayed.first()
    val settings = settingsStore(app).settings.first()
    return AlertScheduler(SystemAlarmBackend(app)).reschedule(rules, settings, prayed, now)
}

/** The alarm went off: plan the next one first (so a failure while showing cannot stop the chain), then show this one. */
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
                rescheduleFromStores(context, from)
                if (alert == null) {
                    Log.w(TAG, "Alarm fired with a damaged alert; nothing shown")
                } else if (!AlertNotifications.show(context, alert)) {
                    Log.w(TAG, "Notifications are not allowed; alert ${alert.ruleId} not shown")
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AlertReceiver"
    }
}

/** The phone restarted, the app was updated, or the clock or time zone changed: the pending alarm is gone or wrong, plan again. */
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
        )
    }
}
