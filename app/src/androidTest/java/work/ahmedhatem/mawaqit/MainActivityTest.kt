package work.ahmedhatem.mawaqit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertTextContains
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import work.ahmedhatem.mawaqit.engine.Prayer
import work.ahmedhatem.mawaqit.settings.settingsStore

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val store = settingsStore(InstrumentationRegistry.getInstrumentation().targetContext)

    @Before
    fun startFromDefaults() = reset()

    @After
    fun backToDefaults() = reset()

    private fun reset() = runBlocking {
        store.chooseCity("القاهرة")
        store.chooseMakruhMinutes(25)
        Unit
    }

    private fun waitForToday() {
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithTagExists("row_FAJR")
        }
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTagExists(tag: String): Boolean =
        onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun launchShowsTitleDatesAndAllSixTimes() {
        waitForToday()
        rule.onNodeWithText("مواقيت اليوم").assertIsDisplayed()
        for (prayer in Prayer.entries) rule.onNodeWithTag("row_${prayer.name}").performScrollTo().assertIsDisplayed()
        // the last row was scrolled to above, so each node is brought into view before it is checked
        rule.onNodeWithTag("gregorian").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("hijri").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("next").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("countdown").performScrollTo().assertTextContains("بعد", substring = true)
        rule.onNodeWithTag("makruh").performScrollTo().assertTextContains("وقت الكراهة حتى", substring = true)
        rule.onNodeWithTag("place").performScrollTo().assertTextContains("القاهرة", substring = true)
    }

    @Test
    fun tabsSwitchBetweenTodayAndSettings() {
        waitForToday()
        rule.onNodeWithTag("tab_SETTINGS").performClick()
        rule.onNodeWithTag("pick_city").assertIsDisplayed()
        rule.onNodeWithTag("use_location").assertIsDisplayed()
        rule.onNodeWithTag("tab_TODAY").performClick()
        rule.onNodeWithTag("next").assertIsDisplayed()
    }

    @Test
    fun choosingACityChangesThePlaceAndIsKept() {
        waitForToday()
        rule.onNodeWithTag("tab_SETTINGS").performClick()
        rule.onNodeWithTag("pick_city").performClick()
        rule.onNodeWithTag("city_search").performTextInput("اسوان")
        rule.onNodeWithTag("city_أسوان").performClick()
        rule.waitUntil(timeoutMillis = 5_000) { runBlocking { store.settings.first() }.place.label == "أسوان" }
        rule.onNodeWithTag("current_place").assertTextContains("أسوان", substring = true)
        rule.onNodeWithTag("tab_TODAY").performClick()
        rule.onNodeWithTag("place").performScrollTo().assertTextContains("أسوان", substring = true)
        // the choice is in the saved file, not only on the screen
        val saved = runBlocking { store.settings.first() }
        org.junit.Assert.assertEquals("أسوان", saved.place.label)
    }

    @Test
    fun makruhDurationIsChosenAndSaved() {
        waitForToday()
        rule.onNodeWithTag("tab_SETTINGS").performClick()
        rule.onNodeWithTag("makruh_25").performScrollTo().assertIsSelected()
        rule.onNodeWithTag("makruh_10").performScrollTo().performClick()
        rule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { store.settings.first() }.makruhMinutes == 10
        }
        rule.onNodeWithTag("makruh_10").assertIsSelected()
    }
}
