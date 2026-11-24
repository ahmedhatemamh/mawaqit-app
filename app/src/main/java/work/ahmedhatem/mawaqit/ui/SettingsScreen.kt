package work.ahmedhatem.mawaqit.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import work.ahmedhatem.mawaqit.R
import work.ahmedhatem.mawaqit.location.LocationFinder
import work.ahmedhatem.mawaqit.location.LocationResult
import work.ahmedhatem.mawaqit.settings.Place
import work.ahmedhatem.mawaqit.settings.Settings
import work.ahmedhatem.mawaqit.settings.SettingsStore

/** The place and the duration of the forbidden period after sunrise. Every choice is saved at once. */
@Composable
fun SettingsScreen(settings: Settings, store: SettingsStore, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val finder = remember { LocationFinder(context) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<Int?>(null) }
    var messageArg by rememberSaveable { mutableStateOf("") }

    fun locate() {
        finder.find { result ->
            scope.launch {
                when (result) {
                    is LocationResult.Found -> {
                        val place = Place.ofPoint(result.latitude, result.longitude)
                        if (place != null && store.choosePoint(result.latitude, result.longitude)) {
                            message = R.string.location_done
                            messageArg = place.label
                        } else {
                            message = R.string.location_outside
                        }
                    }
                    LocationResult.Off -> message = R.string.location_off
                    LocationResult.Unavailable -> message = R.string.location_unavailable
                }
            }
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) locate() else message = R.string.location_denied
    }

    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)

        Text(stringResource(R.string.settings_place), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.settings_current_place, settings.place.label), modifier = Modifier.testTag("current_place"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { picking = true }, modifier = Modifier.testTag("pick_city")) {
                Text(stringResource(R.string.settings_pick_city))
            }
            Button(
                onClick = { if (finder.hasPermission()) locate() else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                modifier = Modifier.testTag("use_location"),
            ) { Text(stringResource(R.string.settings_use_location)) }
        }
        Text(stringResource(R.string.location_privacy), style = MaterialTheme.typography.bodySmall)
        message?.let {
            val text = if (it == R.string.location_done) stringResource(it, messageArg) else stringResource(it)
            Text(text, modifier = Modifier.testTag("location_message"), color = MaterialTheme.colorScheme.secondary)
        }

        HorizontalDivider()

        Text(stringResource(R.string.settings_makruh), style = MaterialTheme.typography.titleMedium)
        for (minutes in Settings.MAKRUH_CHOICES) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = settings.makruhMinutes == minutes,
                        role = Role.RadioButton,
                        onClick = { scope.launch { store.chooseMakruhMinutes(minutes) } },
                    )
                    .testTag("makruh_$minutes"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = settings.makruhMinutes == minutes, onClick = null)
                Text(stringResource(R.string.settings_makruh_choice, minutes), modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }

    if (picking) {
        CityPicker(
            onPick = { name ->
                scope.launch { store.chooseCity(name) }
                message = null
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}
