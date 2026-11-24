package work.ahmedhatem.mawaqit.hijri

import java.io.File
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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
        s.recordAttempt(200)
        assertEquals(HijriSyncState("\"e1\"", 100, 200), s.state())
        s.recordNotModified(300)
        assertEquals(HijriSyncState("\"e1\"", 300, 300), s.state())
    }

    @Test
    fun failedWriteThrowsAndLeavesNoTemporaryFile() {
        val s = store()
        // a non-empty directory sits where the file goes, so moving the finished temporary file onto it fails
        val blocked = File(folder.root, "hijri/hijri.json").apply { mkdirs(); File(this, "x").writeText("x") }
        assertThrows(IOException::class.java) { s.saveFile(hijriJson(bundledPlusOne()), "\"e2\"", 200) }
        assertEquals(HijriCalendar.bundledMonths(), s.loadMonths())
        assertEquals(listOf("hijri.json"), File(folder.root, "hijri").list()!!.toList())
        assertTrue(blocked.isDirectory)
    }

    @Test
    fun twoWritesUseTwoTemporaryFiles() {
        val dir = File(folder.root, "hijri").apply { mkdirs() }
        val a = File.createTempFile("hijri.json", ".tmp", dir)
        val b = File.createTempFile("hijri.json", ".tmp", dir)
        assertTrue(a != b)
        store().saveFile(hijriJson(), null, 1)   // a leftover of another writer is not touched
        assertTrue(a.exists() && b.exists())
    }

    @Test
    fun etagIsOnlyOfferedWhileItsFileIsValid() {
        val s = store()
        s.saveFile(hijriJson(bundledPlusOne()), "\"e1\"", 100)
        assertEquals("\"e1\"", s.state().etag)
        File(folder.root, "hijri/hijri.json").delete()
        assertNull(s.state().etag)                       // file lost: download again
        assertEquals(100L, s.state().lastSuccessMillis)  // the rest of the state stays
        File(folder.root, "hijri/hijri.json").writeText("garbage")
        assertNull(s.state().etag)                       // file damaged: download again
    }

    @Test
    fun noLeftoverTemporaryFilesAfterSaving() {
        store().saveFile(hijriJson(), null, 5)
        assertEquals(listOf("hijri-meta.json", "hijri.json"), File(folder.root, "hijri").list()!!.sorted())
        assertNull(store().state().etag)
    }
}
