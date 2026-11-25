package work.ahmedhatem.mawaqit

import android.app.Application
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import work.ahmedhatem.mawaqit.alerts.AlertNotifications
import work.ahmedhatem.mawaqit.alerts.AlertScheduler
import work.ahmedhatem.mawaqit.alerts.SystemAlarmBackend
import work.ahmedhatem.mawaqit.alerts.alertStore
import work.ahmedhatem.mawaqit.hijri.HijriSyncWorker
import work.ahmedhatem.mawaqit.settings.settingsStore

class MawaqitApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        HijriSyncWorker.scheduleDaily(this)
        AlertNotifications.createChannels(this)
        keepNextAlertPlanned()
    }

    /**
     * Whenever the rules, the answers "I prayed" or the place change (and once when the app starts), the next alert is
     * planned again. That is the only place the alarm is changed while the app runs; the receivers do the rest.
     */
    private fun keepNextAlertPlanned() {
        val alerts = alertStore(this)
        val scheduler = AlertScheduler(SystemAlarmBackend(this))
        scope.launch {
            combine(alerts.rules, alerts.prayed, settingsStore(this@MawaqitApp).settings) { rules, prayed, settings ->
                Triple(rules.rules, prayed, settings)
            }.collectLatest { (rules, prayed, settings) ->
                scheduler.reschedule(rules, settings, prayed, Instant.now())
            }
        }
    }
}
