package work.ahmedhatem.mawaqit

import android.app.Application
import work.ahmedhatem.mawaqit.hijri.HijriSyncWorker

class MawaqitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        HijriSyncWorker.scheduleDaily(this)
    }
}
