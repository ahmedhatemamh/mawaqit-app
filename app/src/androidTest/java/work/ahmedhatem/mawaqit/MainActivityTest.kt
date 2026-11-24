package work.ahmedhatem.mawaqit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import work.ahmedhatem.mawaqit.engine.Prayer

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchShowsTitleAndAllSixTimes() {
        rule.onNodeWithText("مواقيت اليوم").assertIsDisplayed()
        Prayer.entries.forEach { rule.onNodeWithText(it.arabicName).assertIsDisplayed() }
    }
}
