package work.ahmedhatem.mawaqit.alerts

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.alertsDataStore: DataStore<Preferences> by preferencesDataStore(name = "alerts")

fun alertStore(context: Context): AlertStore = AlertStore(context.applicationContext.alertsDataStore)
