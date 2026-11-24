package work.ahmedhatem.mawaqit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import work.ahmedhatem.mawaqit.R
import work.ahmedhatem.mawaqit.engine.HijriCalendar
import work.ahmedhatem.mawaqit.engine.Prayer
import work.ahmedhatem.mawaqit.settings.Place
import work.ahmedhatem.mawaqit.year.DayRow
import work.ahmedhatem.mawaqit.year.MonthTable
import work.ahmedhatem.mawaqit.year.YearModel

/** The year result, a month at a time: every day on two lines, the date on the first and the six times on the second. */
@Composable
fun YearScreen(place: Place, calendar: HijriCalendar, today: LocalDate, modifier: Modifier = Modifier) {
    // Kept as one number so the choice survives a rotation.
    var monthIndex by rememberSaveable { mutableIntStateOf(today.year * 12 + today.monthValue - 1) }
    val month = YearModel.clamp(YearMonth.of(Math.floorDiv(monthIndex, 12), Math.floorMod(monthIndex, 12) + 1))
    fun go(delta: Int) {
        val moved = YearModel.shift(month, delta)
        monthIndex = moved.year * 12 + moved.monthValue - 1
    }
    val table = remember(month, place, calendar) { YearModel.build(month, place, calendar) }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (YearMonth.from(today) == month) today.dayOfMonth - 1 else 0,
    )

    Column(modifier = modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { go(-1) }, modifier = Modifier.testTag("year_prev")) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.year_prev_month))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${ArabicFormat.gregorianMonthName(month.monthValue)} ${month.year}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("year_title"),
                )
                Text(
                    text = hijriRange(table),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("year_hijri_range"),
                )
            }
            IconButton(onClick = { go(1) }, modifier = Modifier.testTag("year_next")) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.year_next_month))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { go(-12) }, modifier = Modifier.testTag("year_prev_year")) { Text(stringResource(R.string.year_prev_year)) }
            TextButton(onClick = { monthIndex = today.year * 12 + today.monthValue - 1 }, modifier = Modifier.testTag("year_current")) {
                Text(stringResource(R.string.year_today))
            }
            TextButton(onClick = { go(12) }, modifier = Modifier.testTag("year_next_year")) { Text(stringResource(R.string.year_next_year)) }
        }
        if (!table.dstConfirmed) {
            Text(
                text = stringResource(R.string.year_dst_unconfirmed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("year_dst_unconfirmed"),
            )
        }
        androidx.compose.runtime.key(month) {
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("year_list")) {
                items(table.rows, key = { it.date.toEpochDay() }) { row -> DayCard(row, isToday = row.date == today) }
            }
        }
    }
}

private fun hijriRange(table: MonthTable): String {
    val parts = table.hijriMonths
    val names = parts.joinToString(" - ") { ArabicFormat.hijriMonthName(it.second) }
    val years = parts.map { it.first }.distinct().joinToString(" - ")
    return "$names $years"
}

@Composable
private fun DayCard(row: DayRow, isToday: Boolean) {
    val friday = row.date.dayOfWeek == DayOfWeek.FRIDAY
    val container = when {
        isToday -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
        friday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }
    Card(colors = CardDefaults.cardColors(containerColor = container), modifier = Modifier.fillMaxWidth().testTag("day_${row.date.toEpochDay()}")) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${ArabicFormat.weekday(row.date)} ${row.date.dayOfMonth} ${ArabicFormat.gregorianMonthName(row.date.monthValue)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                )
                Text(
                    text = ArabicFormat.hijri(row.hijri) + if (row.hijri.certain) "" else " ${stringResource(R.string.uncertain_hijri)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (row.hijri.certain) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.testTag("hijri_${row.date.toEpochDay()}"),
                )
            }
            Row(Modifier.fillMaxWidth()) {
                for (prayer in Prayer.entries) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(prayer.arabicName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(ArabicFormat.time(row.minute(prayer)), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
