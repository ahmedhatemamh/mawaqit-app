package work.ahmedhatem.mawaqit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.YearMonth
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import work.ahmedhatem.mawaqit.engine.EgyptClock
import work.ahmedhatem.mawaqit.settings.settingsStore
import work.ahmedhatem.mawaqit.ui.ArabicFormat

@RunWith(AndroidJUnit4::class)
class YearScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val store = settingsStore(InstrumentationRegistry.getInstrumentation().targetContext)

    @Before
    fun startFromDefaults() = reset()

    @After
    fun backToDefaults() = reset()

    private fun reset() = runBlocking {
        store.chooseCity("القاهرة")
        Unit
    }

    private fun openYearTab() {
        rule.waitUntil(timeoutMillis = 10_000) { rule.onAllNodes(hasTestTag("tab_YEAR")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("tab_YEAR").performClick()
        rule.waitUntil(timeoutMillis = 10_000) { rule.onAllNodes(hasTestTag("year_title")).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun title(month: YearMonth) = "${ArabicFormat.gregorianMonthName(month.monthValue)} ${month.year}"

    @Test
    fun theYearTabOpensOnTheCurrentMonthAndShowsDays() {
        openYearTab()
        val now = YearMonth.from(EgyptClock.localDateTime(java.time.Instant.now()).toLocalDate())
        rule.onNodeWithTag("year_title").assertTextContains(title(now))
        rule.onNodeWithTag("year_list").assertIsDisplayed()
        rule.onNodeWithTag("year_hijri_range").assertIsDisplayed()
    }

    @Test
    fun monthAndYearButtonsMoveTheTable() {
        openYearTab()
        val now = YearMonth.from(EgyptClock.localDateTime(java.time.Instant.now()).toLocalDate())
        rule.onNodeWithTag("year_next").performClick()
        rule.onNodeWithTag("year_title").assertTextContains(title(now.plusMonths(1)))
        rule.onNodeWithTag("year_prev").performClick()
        rule.onNodeWithTag("year_prev").performClick()
        rule.onNodeWithTag("year_title").assertTextContains(title(now.minusMonths(1)))
        rule.onNodeWithTag("year_next_year").performClick()
        rule.onNodeWithTag("year_title").assertTextContains(title(now.minusMonths(1).plusYears(1)))
        rule.onNodeWithTag("year_prev_year").performClick()
        rule.onNodeWithTag("year_prev_year").performClick()
        rule.onNodeWithTag("year_title").assertTextContains(title(now.minusMonths(1).minusYears(1)))
        rule.onNodeWithTag("year_current").performClick()
        rule.onNodeWithTag("year_title").assertTextContains(title(now))
    }

    @Test
    fun aFarFutureYearIsShownWithItsSummerTimeFlag() {
        openYearTab()
        // 2031 is outside the years whose summer-time rule is confirmed: about 5 years from now, press the year button
        repeat(60) { rule.onNodeWithTag("year_next").performClick() }
        rule.onNodeWithTag("year_dst_unconfirmed").assertIsDisplayed()
    }
}
