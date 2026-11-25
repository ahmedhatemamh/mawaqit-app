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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import work.ahmedhatem.mawaqit.settings.Settings
import work.ahmedhatem.mawaqit.settings.settingsStore

/**
 * The one place that plans the alarm. The receivers and the running app all come through here, one at a time, and each
 * reads what is saved inside the lock, so the last plan always comes from the latest saved rules and answers.
 */
object AlertPlanning {
    private val lock = Mutex()

    /** [companions] are the alerts due together with the fired one; they are found before the alarm is set, so a failure to set it does not lose them. */
    class Outcome(val settings: Settings, val firedStillWanted: Boolean, val companions: List<FiredAlert>, val plan: Result<Plan>)

    suspend fun run(context: Context, now: Instant, fired: FiredAlert?): Outcome = lock.withLock {
        val app = context.applicationContext
        val rules = alertStore(app).rules.first().rules
        val prayed = alertStore(app).prayed.first()
        val settings = settingsStore(app).settings.first()
        val scheduler = AlertScheduler(SystemAlarmBackend(app))
        val companions = fired?.let { scheduler.companionsOf(it, rules, settings, prayed, until = now) } ?: emptyList()
        val wanted = fired == null || !scheduler.isAnswered(fired, prayed)
        Outcome(settings, wanted, companions, runCatching { scheduler.reschedule(rules, settings, prayed, now) })
    }
}

/** Plans the next alarm from what is saved on the phone. */
suspend fun rescheduleFromStores(context: Context, now: Instant = Instant.now()): Plan =
    AlertPlanning.run(context, now, null).plan.getOrThrow()

/**
 * The alarm went off: plan the next one first (so a failure while showing cannot stop the chain), then show this one and
 * the other alerts that are due (same minute, or fell due while the alarm was late). If planning fails the alerts are
 * still shown, and then the failure is thrown (not hidden), so a problem in planning never costs a prayer's notification.
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
                val now = Instant.now()
                val from = maxOf(now, alert?.let { Instant.ofEpochMilli(it.atMillis) } ?: Instant.EPOCH)
                val outcome = runCatching { AlertPlanning.run(context, from, alert) }
                if (alert == null) Log.w(TAG, "Alarm fired with a damaged alert; nothing shown")
                // Counted from the moment of showing, and each notification goes away at the next prayer time (never sooner than
                // the minimum). If the saved settings could not be read, the default place is used so it still goes away.
                val expiresAt = AlertScheduler.expiryAfter(from, outcome.getOrNull()?.settings ?: Settings())
                // The fired alert is dropped only when the saved answers say its question was already answered.
                val firedToShow = alert.takeIf { outcome.getOrNull()?.firedStillWanted ?: true }
                for (a in listOfNotNull(firedToShow) + (outcome.getOrNull()?.companions ?: emptyList())) {
                    if (!AlertNotifications.show(context, a, from, expiresAt)) Log.w(TAG, "Notifications are not allowed; alert ${a.ruleId} not shown")
                }
                (outcome.exceptionOrNull() ?: outcome.getOrNull()?.plan?.exceptionOrNull())?.let { throw it }
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
