package work.ahmedhatem.mawaqit.hijri

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import work.ahmedhatem.mawaqit.engine.HijriCalendar

class HijriStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun store() = HijriStore(File(folder.root, "hijri"))

    @Test
    fun emptyStoreUsesTheBundledMonths() {
        assertEquals(HijriCalendar.bundledMonths(), store().loadMonths())
        assertEquals(HijriSyncState(null, null, null), store().state())
    }

    @Test
    fun savedFileIsUsedAndRemembered() {
        val s = store()
        s.saveFile(hijriJson(bundledPlusOne()), "\"abc\"", 1000)
        assertEquals(bundledPlusOne(), s.loadMonths())
        assertEquals(HijriSyncState("\"abc\"", 1000, 1000), HijriStore(File(folder.root, "hijri")).state())
    }

    @Test
    fun damagedOrUnsuitableStoredFileFallsBackToBundled() {
        val dir = File(folder.root, "hijri").apply { mkdirs() }
        val data = File(dir, "hijri.json")
        for (bad in listOf("", "garbage", hijriJson(schema = 9), hijriJson(HijriCalendar.bundledMonths().take(5)))) {
            data.writeText(bad)
            assertEquals(bad, HijriCalendar.bundledMonths(), store().loadMonths())
        }
        data.writeBytes(ByteArray(HijriParser.MAX_BYTES + 1) { 'a'.code.toByte() })
        assertEquals(HijriCalendar.bundledMonths(), store().loadMonths())
    }

    @Test
    fun damagedMetaFileMeansNoState() {
        val dir = File(folder.root, "hijri").apply { mkdirs() }
        File(dir, "hijri-meta.json").writeText("{not json")
        assertEquals(HijriSyncState(null, null, null), store().state())
    }

    @Test
    fun notModifiedAndRejectedUpdateOnlyTheirOwnFields() {
        val s = store()
        s.saveFile(hijriJson(), "\"e1\"", 100)
        s.recordRejected(200)
        assertEquals(HijriSyncState("\"e1\"", 100, 200), s.state())
        s.recordNotModified(300)
        assertEquals(HijriSyncState("\"e1\"", 300, 300), s.state())
    }

    @Test
    fun failedWriteLeavesTheOldFileWhole() {
        val s = store()
        s.saveFile(hijriJson(), "\"e1\"", 100)
        // make the temporary file impossible to create: a directory sits where it would go
        File(folder.root, "hijri/hijri.json.tmp").mkdirs()
        val failed = runCatching { s.saveFile(hijriJson(bundledPlusOne()), "\"e2\"", 200) }
        assertTrue(failed.isFailure)
        assertEquals(HijriCalendar.bundledMonths(), s.loadMonths())
        assertEquals("\"e1\"", s.state().etag)
    }

    @Test
    fun noLeftoverTemporaryFilesAfterSaving() {
        store().saveFile(hijriJson(), null, 5)
        assertEquals(listOf("hijri-meta.json", "hijri.json"), File(folder.root, "hijri").list()!!.sorted())
        assertNull(store().state().etag)
    }
}
