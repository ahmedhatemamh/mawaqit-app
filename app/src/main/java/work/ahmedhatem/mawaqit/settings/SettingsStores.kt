package work.ahmedhatem.mawaqit.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

fun settingsStore(context: Context): SettingsStore = SettingsStore(context.applicationContext.settingsDataStore)
