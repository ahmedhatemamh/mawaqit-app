package work.ahmedhatem.mawaqit

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
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
import java.time.Instant
import java.time.LocalDate
import work.ahmedhatem.mawaqit.engine.EgyptClock
import work.ahmedhatem.mawaqit.settings.settingsStore
import work.ahmedhatem.mawaqit.ui.ArabicFormat

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
        rule.waitUntil(UI_WAIT_MS) { rule.onAllNodes(hasTestTag("search_open")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("search_open").performClick()
    }

    /** Like assertTextContains, but the failure message holds the text really shown (one line, so the CI log keeps it). */
    private fun expectShown(tag: String, expected: String) {
        val shown = rule.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString(" ")
        if (!shown.contains(expected)) throw AssertionError("$tag shows [$shown] but expected it to contain [$expected]")
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
        rule.waitUntil(UI_WAIT_MS) { rule.onAllNodes(hasTestTag("back_to_today")).fetchSemanticsNodes().isNotEmpty() }
        expectShown("gregorian", ArabicFormat.gregorian(LocalDate.of(2025, 3, 1)))
        rule.onNodeWithTag("back_to_today").performClick()
        rule.waitUntil(UI_WAIT_MS) { rule.onAllNodes(hasTestTag("back_to_today")).fetchSemanticsNodes().isEmpty() }
        val today = EgyptClock.localDateTime(Instant.now()).toLocalDate()
        expectShown("gregorian", ArabicFormat.gregorian(today))
    }

    @Test
    fun hijriDayShowsTheMatchingGregorianDay() {
        open()
        rule.onNodeWithTag("search_hijri").performClick()
        fill("1", "9", "1446")
        rule.onNodeWithTag("search_go").performClick()
        rule.waitUntil(UI_WAIT_MS) { rule.onAllNodes(hasTestTag("back_to_today")).fetchSemanticsNodes().isNotEmpty() }
        // The device may hold a newer synced calendar than the bundled one, so the shown day is checked by its own Hijri date.
        rule.onNodeWithTag("hijri").assertTextContains("1 ${ArabicFormat.hijriMonthName(9)} 1446")
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
        rule.waitUntil(UI_WAIT_MS) { rule.onAllNodes(hasTestTag("year_title")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("year_title").assertTextContains("${ArabicFormat.gregorianMonthName(1)} 2030")
    }
}
