package work.ahmedhatem.mawaqit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import work.ahmedhatem.mawaqit.R
import work.ahmedhatem.mawaqit.engine.HijriCalendar
import work.ahmedhatem.mawaqit.search.CalendarKind
import work.ahmedhatem.mawaqit.search.DateQuery
import work.ahmedhatem.mawaqit.search.DateSearch
import work.ahmedhatem.mawaqit.search.SearchError
import work.ahmedhatem.mawaqit.search.SearchResult

private fun SearchError.message(): Int = when (this) {
    SearchError.MISSING_YEAR -> R.string.search_error_year
    SearchError.DAY_WITHOUT_MONTH -> R.string.search_error_day_without_month
    SearchError.NOT_A_NUMBER -> R.string.search_error_number
    SearchError.BAD_MONTH -> R.string.search_error_month
    SearchError.BAD_DAY -> R.string.search_error_day
    SearchError.YEAR_OUT_OF_RANGE -> R.string.search_error_range
}

/** Search by a Gregorian or Hijri date, a month, or a year (spec 3.1). A refused entry shows why and keeps the dialog open. */
@Composable
fun DateSearchDialog(calendar: HijriCalendar, onResult: (SearchResult) -> Unit, onDismiss: () -> Unit) {
    var hijri by rememberSaveable { mutableStateOf(false) }
    var day by rememberSaveable { mutableStateOf("") }
    var month by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<SearchError?>(null) }

    fun submit() {
        val query = DateQuery(if (hijri) CalendarKind.HIJRI else CalendarKind.GREGORIAN, day, month, year)
        when (val result = DateSearch.resolve(query, calendar)) {
            is SearchResult.Invalid -> error = result.error
            else -> onResult(result)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.search_title)) },
        confirmButton = {
            Button(onClick = { submit() }, modifier = Modifier.testTag("search_go")) { Text(stringResource(R.string.search_go)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    for ((isHijri, label, tag) in listOf(
                        Triple(false, R.string.search_gregorian, "search_gregorian"),
                        Triple(true, R.string.search_hijri, "search_hijri"),
                    )) {
                        Row(
                            modifier = Modifier
                                .selectable(selected = hijri == isHijri, role = Role.RadioButton, onClick = { hijri = isHijri; error = null })
                                .testTag(tag),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = hijri == isHijri, onClick = null)
                            Text(stringResource(label), modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Field(day, { day = it; error = null }, R.string.search_day, "search_day", Modifier.weight(1f))
                    Field(month, { month = it; error = null }, R.string.search_month, "search_month", Modifier.weight(1f))
                    Field(year, { year = it; error = null }, R.string.search_year, "search_year", Modifier.weight(1.4f))
                }
                Text(stringResource(R.string.search_hint), style = MaterialTheme.typography.bodySmall)
                error?.let {
                    Text(stringResource(it.message()), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("search_error"))
                }
            }
        },
    )
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: Int, tag: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 6) onChange(it) },
        label = { Text(stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.testTag(tag),
    )
}
