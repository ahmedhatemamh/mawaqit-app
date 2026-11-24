package work.ahmedhatem.mawaqit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import work.ahmedhatem.mawaqit.R
import work.ahmedhatem.mawaqit.engine.Cities

/** A searchable list of the cities of the official tables. */
@Composable
fun CityPicker(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(query) { Cities.all.filter { ArabicSearch.matches(it.name, query) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        title = { Text(stringResource(R.string.city_picker_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.city_picker_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("city_search"),
                )
                if (shown.isEmpty()) {
                    Text(stringResource(R.string.city_picker_empty), modifier = Modifier.padding(top = 12.dp))
                }
                LazyColumn(Modifier.heightIn(max = 360.dp).testTag("city_list")) {
                    items(shown, key = { it.name }) { city ->
                        Text(
                            text = city.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(city.name) }
                                .padding(vertical = 12.dp)
                                .testTag("city_${city.name}"),
                        )
                    }
                }
            }
        },
    )
}
