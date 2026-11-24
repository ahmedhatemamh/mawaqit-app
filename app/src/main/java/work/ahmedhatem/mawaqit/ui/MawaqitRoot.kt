package work.ahmedhatem.mawaqit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import work.ahmedhatem.mawaqit.R
import work.ahmedhatem.mawaqit.engine.HijriCalendar
import work.ahmedhatem.mawaqit.hijri.HijriStore
import work.ahmedhatem.mawaqit.settings.Settings
import work.ahmedhatem.mawaqit.settings.SettingsStore
import work.ahmedhatem.mawaqit.today.TodayModel

private enum class Tab(val label: Int, val icon: ImageVector) {
    TODAY(R.string.tab_today, Icons.Filled.Home),
    SETTINGS(R.string.tab_settings, Icons.Filled.Settings),
}

private const val MONTH_MILLIS = 30L * 24 * 60 * 60 * 1000

/** The current instant, refreshed at the start of every minute. */
@Composable
private fun rememberNow(): State<Instant> = produceState(Instant.now()) {
    while (true) {
        value = Instant.now()
        delay(60_000 - System.currentTimeMillis() % 60_000 + 50)
    }
}

/** Days since the last good Hijri sync when it is more than a month, otherwise null (spec 3.6). */
internal fun dataAgeDays(lastSuccessMillis: Long?, nowMillis: Long): Int? {
    if (lastSuccessMillis == null) return null
    val age = nowMillis - lastSuccessMillis
    return if (age > MONTH_MILLIS) (age / (24L * 60 * 60 * 1000)).toInt() else null
}

@Composable
fun MawaqitRoot(store: SettingsStore, hijriStore: HijriStore) {
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    val settings: Settings? by store.settings.collectAsState(initial = null)
    val now by rememberNow()
    val calendar by produceState<HijriCalendar?>(null) {
        value = withContext(Dispatchers.IO) { HijriCalendar(hijriStore.loadMonths()) }
    }
    val syncState by produceState<work.ahmedhatem.mawaqit.hijri.HijriSyncState?>(null) {
        value = withContext(Dispatchers.IO) { hijriStore.state() }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                for (t in Tab.entries) {
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(stringResource(t.label)) },
                        modifier = Modifier.testTag("tab_${t.name}"),
                    )
                }
            }
        },
    ) { padding ->
        val current = settings
        val hijri = calendar
        if (current == null || hijri == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        val scroll = Modifier.padding(padding).verticalScroll(rememberScrollState())
        when (tab) {
            Tab.TODAY -> TodayScreen(
                model = remember(now, current, hijri) { TodayModel.build(now, current, hijri) },
                dataAgeDays = dataAgeDays(syncState?.lastSuccessMillis, now.toEpochMilli()),
                modifier = scroll,
            )
            Tab.SETTINGS -> SettingsScreen(current, store, scroll)
        }
    }
}
