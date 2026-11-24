package work.ahmedhatem.mawaqit.hijri

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import work.ahmedhatem.mawaqit.engine.HijriCalendar

/** The parts that differ between the JVM and a real phone: Android's own org.json and file system, and the schedule. */
@RunWith(AndroidJUnit4::class)
class HijriOnDeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dir = File(context.cacheDir, "hijri-test")

    @After
    fun clean() {
        dir.deleteRecursively()
    }

    private fun json(schema: Int = 1, extra: String = ""): String {
        val list = JSONArray()
        for (m in HijriCalendar.bundledMonths()) {
            list.put(JSONObject().put("year", m.year).put("month", m.month).put("start", m.start.toString()))
        }
        return JSONObject().put("schema", schema).put("months", list).toString().let {
            if (extra.isEmpty()) it else it.replaceFirst("{", "{$extra,")
        }
    }

    @Test
    fun androidJsonReadsTheFileAndRejectsBadOnes() {
        assertEquals(HijriCalendar.bundledMonths(), HijriParser.parse(json()))
        assertEquals(133, HijriParser.parse(json(extra = "\"future\":{\"a\":[1,2]}")).size)
        for (bad in listOf("", "garbage", "[]", json(schema = 2), "{\"schema\":1,\"months\":[{\"year\":\"1446\",\"month\":9,\"start\":\"2025-03-01\"}]}",
            "{\"schema\":1,\"months\":[{\"year\":1446.5,\"month\":9,\"start\":\"2025-03-01\"}]}",
            "{\"schema\":1,\"months\":[{\"year\":1446,\"month\":9,\"start\":null}]}")) {
            assertThrows(bad, HijriDataException::class.java) { HijriParser.parse(bad) }
        }
    }

    @Test
    fun storeKeepsAFileOnTheRealFileSystem() {
        val store = HijriStore(dir)
        assertEquals(HijriCalendar.bundledMonths(), store.loadMonths())
        store.saveFile(json(), "\"e\"", 42)
        assertEquals(HijriCalendar.bundledMonths(), HijriStore(dir).loadMonths())
        assertEquals(HijriSyncState("\"e\"", 42, 42), HijriStore(dir).state())
    }

    @Test
    fun dailyWorkIsScheduledOnceWithANetworkConstraint() {
        val manager = WorkManager.getInstance(context)
        manager.cancelUniqueWork("hijri-sync-daily").result.get()
        HijriSyncWorker.scheduleDaily(context)
        HijriSyncWorker.scheduleDaily(context)   // a second call keeps the first
        val infos = manager.getWorkInfosForUniqueWork("hijri-sync-daily").get()
        assertEquals(1, infos.size)
        assertTrue(infos[0].state == WorkInfo.State.ENQUEUED || infos[0].state == WorkInfo.State.RUNNING)
        assertTrue(infos[0].tags.any { it == HijriSyncWorker::class.java.name })
        assertEquals(NetworkType.CONNECTED, infos[0].constraints.requiredNetworkType)
        assertEquals(24L * 60 * 60 * 1000, infos[0].periodicityInfo!!.repeatIntervalMillis)
    }
}
