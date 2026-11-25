package work.ahmedhatem.mawaqit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import work.ahmedhatem.mawaqit.hijri.HijriSyncWorker
import work.ahmedhatem.mawaqit.settings.settingsStore
import work.ahmedhatem.mawaqit.ui.MawaqitRoot
import work.ahmedhatem.mawaqit.ui.MawaqitTheme

class MainActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        HijriSyncWorker.syncIfStale(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = settingsStore(this)
        val hijriStore = HijriSyncWorker.store(this)
        setContent {
            MawaqitTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MawaqitRoot(store, hijriStore)
                }
            }
        }
    }
}
