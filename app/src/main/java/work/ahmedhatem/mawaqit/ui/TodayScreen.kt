package work.ahmedhatem.mawaqit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import work.ahmedhatem.mawaqit.R
import work.ahmedhatem.mawaqit.engine.Prayer
import work.ahmedhatem.mawaqit.today.TodayModel

/** The home screen: date, next time with its countdown, and the six times of the day. */
@Composable
fun TodayScreen(model: TodayModel, dataAgeDays: Int?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.today_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        DateBlock(model)
        NextBlock(model)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
            Column(Modifier.padding(vertical = 4.dp)) {
                for (row in model.rows) {
                    TimeRowView(row.prayer, ArabicFormat.time(row.minute), row.isNext)
                    if (row.prayer == Prayer.SUNRISE) {
                        Text(
                            text = stringResource(R.string.makruh_until, ArabicFormat.time(model.makruhEndsAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp).testTag("makruh"),
                        )
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.place_line, model.placeLabel),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("place"),
        )
        if (!model.dstConfirmed) {
            Text(
                text = stringResource(R.string.dst_unconfirmed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("dst_unconfirmed"),
            )
        }
        if (dataAgeDays != null) {
            Text(
                text = stringResource(R.string.data_age, dataAgeDays),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("data_age"),
            )
        }
    }
}

@Composable
private fun DateBlock(model: TodayModel) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "${ArabicFormat.weekday(model.date)}، ${ArabicFormat.gregorian(model.date)}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("gregorian"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = ArabicFormat.hijri(model.hijri),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("hijri"),
            )
            if (!model.hijri.certain) {
                Text(
                    text = stringResource(R.string.uncertain_hijri),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.testTag("hijri_uncertain"),
                )
            }
        }
    }
}

@Composable
private fun NextBlock(model: TodayModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth().testTag("next"),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "${stringResource(R.string.next_label)}: ${model.next.prayer.arabicName}  ${ArabicFormat.time(model.next.minute)}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = stringResource(R.string.in_time, ArabicFormat.countdown(model.next.minutesUntil)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("countdown"),
            )
        }
    }
}

@Composable
private fun TimeRowView(prayer: Prayer, time: String, highlighted: Boolean) {
    val shape = RoundedCornerShape(10.dp)
    val background = if (highlighted) MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f) else androidx.compose.ui.graphics.Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(shape)
            .background(background)
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .testTag("row_${prayer.name}"),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = prayer.arabicName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            text = time,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
