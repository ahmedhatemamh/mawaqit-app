package work.ahmedhatem.mawaqit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import work.ahmedhatem.mawaqit.settings.settingsStore

@RunWith(AndroidJUnit4::class)
class SearchTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val store = settingsStore(InstrumentationRegistry.getInstrumentation().targetContext)

    @Before
    fun start() = reset()

    @After
    fun end() = reset()

    private fun reset() = runBlocking { store.chooseCity("القاهرة"); Unit }

    private fun open() {
        rule.waitUntil(10_000) { rule.onAllNodes(hasTestTag("search_open")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("search_open").performClick()
    }

    private fun fill(day: String, month: String, year: String) {
        if (day.isNotEmpty()) rule.onNodeWithTag("search_day").performTextInput(day)
        if (month.isNotEmpty()) rule.onNodeWithTag("search_month").performTextInput(month)
        rule.onNodeWithTag("search_year").performTextInput(year)
    }

    @Test
    fun gregorianDayShowsThatDayAndTodayButtonReturns() {
        open()
        fill("1", "3", "2025")
        rule.onNodeWithTag("search_go").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("back_to_today")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("gregorian").assertIsDisplayed()
        rule.onNodeWithTag("back_to_today").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("back_to_today")).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun hijriDayShowsTheMatchingGregorianDay() {
        open()
        rule.onNodeWithTag("search_hijri").performClick()
        fill("1", "9", "1446")
        rule.onNodeWithTag("search_go").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("back_to_today")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("gregorian").assertIsDisplayed()
    }

    @Test
    fun badDateKeepsTheDialogAndShowsTheReason() {
        open()
        fill("31", "4", "2026")
        rule.onNodeWithTag("search_go").performClick()
        rule.onNodeWithTag("search_error").assertIsDisplayed()
        rule.onNodeWithTag("search_go").assertIsDisplayed()
    }

    @Test
    fun yearOnlyOpensTheYearTable() {
        open()
        fill("", "", "2030")
        rule.onNodeWithTag("search_go").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("year_title")).fetchSemanticsNodes().isNotEmpty() }
    }
}
