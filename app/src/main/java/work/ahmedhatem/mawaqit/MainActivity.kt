package work.ahmedhatem.mawaqit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import work.ahmedhatem.mawaqit.engine.Prayer
import work.ahmedhatem.mawaqit.ui.MawaqitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MawaqitTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TodaySkeleton()
                }
            }
        }
    }
}

/** Phase 6 placeholder: the screen shell and the app-to-engine wiring. Real times arrive in phases 7 and 9. */
@Composable
fun TodaySkeleton() {
    Column(
        // Insets first (edge-to-edge on Android 15+), then scrolling so large fonts or short windows stay reachable,
        // then the visual margin.
        modifier = Modifier
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.today_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Prayer.entries.forEach { prayer ->
            Text(text = prayer.arabicName, style = MaterialTheme.typography.titleLarge)
        }
    }
}
