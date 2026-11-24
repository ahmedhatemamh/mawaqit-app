package work.ahmedhatem.mawaqit.hijri

import java.io.File
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URL
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import work.ahmedhatem.mawaqit.engine.HijriCalendar

/** A one-request-at-a-time HTTP server on localhost: reads the request headers, lets [handler] write the raw answer. */
private class TinyServer(var handler: (Map<String, String>, OutputStream) -> Unit) {
    private val socket = ServerSocket(0, 5, InetAddress.getByName("127.0.0.1"))
    val port get() = socket.localPort
    val requests = ArrayList<String?>()   // the If-None-Match header of each request
    private val thread = Thread {
        try {
            while (true) {
                val client = socket.accept()
                try {
                    val reader = client.getInputStream().bufferedReader(Charsets.ISO_8859_1)
                    val headers = HashMap<String, String>()
                    reader.readLine()
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                        val i = line.indexOf(':')
                        headers[line.substring(0, i).trim().lowercase()] = line.substring(i + 1).trim()
                    }
                    synchronized(requests) { requests.add(headers["if-none-match"]) }
                    handler(headers, client.getOutputStream())
                    client.getOutputStream().flush()
                } catch (e: java.io.IOException) {
                    // the client gave up (timeout): fine
                } finally {
                    client.close()
                }
            }
        } catch (e: java.io.IOException) {
            // server socket closed
        }
    }.apply { isDaemon = true; start() }

    fun stop() = socket.close()
}

private fun OutputStream.text(s: String) = write(s.toByteArray(Charsets.ISO_8859_1))

private fun status(out: OutputStream, code: Int, extra: String = "", length: Int? = null) {
    out.text("HTTP/1.1 $code X\r\n$extra" + (if (length != null) "Content-Length: $length\r\n" else "") + "Connection: close\r\n\r\n")
}

class HijriSyncTest {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var server: TinyServer
    private var handler: (Map<String, String>, OutputStream) -> Unit = { _, out -> status(out, 404, length = 0) }
    private val requests get() = server.requests

    @Before
    fun start() {
        server = TinyServer { headers, out -> handler(headers, out) }
    }

    @After
    fun stop() = server.stop()

    private fun reply(code: Int, body: String? = null, etag: String? = null): (Map<String, String>, OutputStream) -> Unit = { _, out ->
        val bytes = (body ?: "").toByteArray()
        status(out, code, if (etag != null) "ETag: $etag\r\n" else "", bytes.size)
        out.write(bytes)
    }

    private val store by lazy { HijriStore(File(folder.root, "hijri")) }
    private fun sync(timeout: Int = 5000) =
        HijriSync(store, HijriFetcher(URL("http://127.0.0.1:${server.port}/hijri.json"), timeout))

    @Test
    fun productionAddressIsTheFixedHttpsOne() {
        assertEquals("https://data.ahmedhatem.work/hijri.json", HijriFetcher.HIJRI_URL)
    }

    @Test
    fun newFileIsStoredWithItsEtagThenNotModified() {
        handler = reply(200, hijriJson(bundledPlusOne()), "\"v1\"")
        assertEquals(SyncOutcome.Updated, sync().run(1000))
        assertEquals(bundledPlusOne(), store.loadMonths())
        assertEquals(HijriSyncState("\"v1\"", 1000, 1000), store.state())

        handler = { h, out -> if (h["if-none-match"] == "\"v1\"") reply(304)(h, out) else reply(500)(h, out) }
        assertEquals(SyncOutcome.NotModified, sync().run(2000))
        assertEquals(listOf(null, "\"v1\""), requests)
        assertEquals(HijriSyncState("\"v1\"", 2000, 2000), store.state())
        assertEquals(bundledPlusOne(), store.loadMonths())
    }

    @Test
    fun fileWithoutEtagIsAccepted() {
        handler = reply(200, hijriJson(bundledPlusOne()))
        assertEquals(SyncOutcome.Updated, sync().run(1))
        assertEquals(null, store.state().etag)
    }

    @Test
    fun correctionOfAKnownMonthIsAccepted() {
        val current = HijriCalendar.bundledMonths()
        // the last month was 30 days after the one before: an official correction to 29 days
        val corrected = current.map { if (it == current.last()) it.copy(start = it.start.minusDays(1)) else it }
        handler = reply(200, hijriJson(corrected), "\"c\"")
        assertEquals(SyncOutcome.Updated, sync().run(1))
        assertEquals(corrected, store.loadMonths())
    }

    @Test
    fun badFilesAreRejectedAndTheOldDataStays() {
        handler = reply(200, hijriJson(bundledPlusOne()), "\"good\"")
        sync().run(1000)
        val bad = listOf(
            "", "garbage", "<html>x</html>", hijriJson(schema = 2),
            hijriJson(HijriCalendar.bundledMonths()),          // older: lacks the newest month
            hijriJson(bundledPlusOne().filterIndexed { i, _ -> i != 7 }),   // lost a month
            hijriJson(emptyList()),
        )
        for (text in bad) {
            handler = reply(200, text, "\"bad\"")
            val outcome = sync().run(2000)
            assertTrue(text.take(30), outcome is SyncOutcome.Rejected)
            assertEquals(bundledPlusOne(), store.loadMonths())
            assertEquals(HijriSyncState("\"good\"", 1000, 2000), store.state())   // the bad file's ETag is not kept
        }
    }

    @Test
    fun invalidUtf8IsRejected() {
        handler = { _, out ->
            status(out, 200, length = 3)
            out.write(byteArrayOf(0x7b, 0xC3.toByte(), 0x28))
        }
        assertTrue(sync().run(1) is SyncOutcome.Rejected)
        assertEquals(HijriCalendar.bundledMonths(), store.loadMonths())
    }

    @Test
    fun oversizedFileIsRejectedWhetherAnnouncedOrNot() {
        val big = "x".repeat(HijriParser.MAX_BYTES + 10)
        handler = reply(200, big)
        assertTrue(sync().run(1) is SyncOutcome.Rejected)
        handler = { _, out ->   // no Content-Length: the body ends when the connection closes
            status(out, 200)
            out.write(big.toByteArray())
        }
        assertTrue(sync().run(1) is SyncOutcome.Rejected)
        assertEquals(HijriCalendar.bundledMonths(), store.loadMonths())
    }

    @Test
    fun serverErrorsAndRedirectsAreNetworkErrorsThatOnlyRecordTheAttempt() {
        for (code in listOf(404, 429, 500, 503, 301, 302, 204, 401)) {
            handler = { _, out ->
                status(out, code, if (code in 300..399) "Location: http://127.0.0.1:1/x\r\n" else "", 0)
            }
            assertTrue("$code", sync().run(7) is SyncOutcome.NetworkError)
        }
        assertEquals(HijriSyncState(null, null, 7), store.state())   // no success, no ETag, no new data
        assertEquals(HijriCalendar.bundledMonths(), store.loadMonths())
    }

    @Test
    fun noServerAndCutConnectionAndTimeout() {
        handler = { _, out -> status(out, 200, length = 1000); out.write(byteArrayOf(1)) }
        assertTrue(sync().run(1) is SyncOutcome.NetworkError)           // body cut short
        handler = { _, _ -> Thread.sleep(1500) }
        assertTrue(sync(timeout = 300).run(1) is SyncOutcome.NetworkError)   // too slow
        val port = server.port
        server.stop()
        val gone = HijriSync(store, HijriFetcher(URL("http://127.0.0.1:$port/hijri.json"), 1000))
        assertTrue(gone.run(1) is SyncOutcome.NetworkError)
        assertEquals(HijriSyncState(null, null, 1), store.state())
    }

    @Test
    fun aFailingStoreGivesAStorageErrorNotAnException() {
        // a non-empty directory sits where the state file goes, so every write of it fails
        File(folder.root, "hijri/hijri-meta.json").apply { mkdirs(); File(this, "x").writeText("x") }
        handler = reply(200, hijriJson(bundledPlusOne()), "\"v1\"")
        assertTrue(sync().run(1) is SyncOutcome.StorageError)         // new file: state write fails
        handler = reply(200, "garbage")
        assertTrue(sync().run(1) is SyncOutcome.StorageError)         // refused file: attempt write fails
        handler = reply(500)
        assertTrue(sync().run(1) is SyncOutcome.StorageError)         // network error: attempt write fails
    }

    @Test
    fun twoSyncsAtOnceNeverLeaveTheOlderFile() {
        val newer = TinyServer { _, out -> reply(200, hijriJson(bundledPlus(2)), "\"two\"")(emptyMap(), out) }
        try {
            repeat(30) { round ->
                val dir = File(folder.root, "race$round")
                handler = reply(200, hijriJson(bundledPlus(1)), "\"one\"")
                val older = HijriSync(HijriStore(dir), HijriFetcher(URL("http://127.0.0.1:${server.port}/hijri.json"), 5000))
                val latest = HijriSync(HijriStore(dir), HijriFetcher(URL("http://127.0.0.1:${newer.port}/hijri.json"), 5000))
                val start = java.util.concurrent.CountDownLatch(1)
                val threads = listOf(older, latest).map { sync ->
                    Thread { start.await(); sync.run(round.toLong()) }.apply { start() }
                }
                start.countDown()
                threads.forEach { it.join() }
                assertEquals("round $round", bundledPlus(2), HijriStore(dir).loadMonths())
            }
        } finally {
            newer.stop()
        }
    }

    @Test
    fun lockedBlocksAreNeverRunAtTheSameTime() {
        val inside = java.util.concurrent.atomic.AtomicInteger()
        val overlaps = java.util.concurrent.atomic.AtomicInteger()
        val threads = (1..4).map {
            Thread {
                repeat(50) {
                    HijriStore(File(folder.root, "lock")).locked {
                        if (inside.incrementAndGet() > 1) overlaps.incrementAndGet()
                        Thread.yield()
                        inside.decrementAndGet()
                    }
                }
            }.apply { start() }
        }
        threads.forEach { it.join() }
        assertEquals(0, overlaps.get())
    }

    @Test
    fun appOpenRuleIsOlderThanADay() {
        val day = HijriSync.DAY_MILLIS
        assertTrue(HijriSync.isStale(HijriSyncState(null, null, null), 5))
        assertFalse(HijriSync.isStale(HijriSyncState(null, 0, 1000), 1000 + day - 1))
        assertTrue(HijriSync.isStale(HijriSyncState(null, 0, 1000), 1000 + day))
        assertTrue(HijriSync.isStale(HijriSyncState(null, 0, 5000), 1000))   // the clock went back
    }

    @Test
    fun appOpenWorkSkipsWhenTheLastAttemptIsRecentAndTheDailyWorkNever() {
        val fresh = HijriSyncState(null, 0, 1000)
        val now = 1000 + HijriSync.DAY_MILLIS - 1
        assertTrue(HijriSyncWorker.shouldSkip(true, fresh, now))
        assertFalse(HijriSyncWorker.shouldSkip(false, fresh, now))                       // the daily work never skips
        assertFalse(HijriSyncWorker.shouldSkip(true, fresh, 1000 + HijriSync.DAY_MILLIS))  // due
        assertFalse(HijriSyncWorker.shouldSkip(true, HijriSyncState(null, null, null), now))  // never tried
    }

    @Test
    fun onlyTheDailyWorkRetriesAFailedAttempt() {
        val success = androidx.work.ListenableWorker.Result.success()
        for (failure in listOf(SyncOutcome.NetworkError("x"), SyncOutcome.StorageError("x"))) {
            assertEquals(success, HijriSyncWorker.resultOf(failure, 0, fromAppOpen = true))
            assertEquals(androidx.work.ListenableWorker.Result.retry(), HijriSyncWorker.resultOf(failure, 0, fromAppOpen = false))
        }
    }

    @Test
    fun workerResultMapping() {
        val retry = androidx.work.ListenableWorker.Result.retry()
        assertEquals(androidx.work.ListenableWorker.Result.success(), HijriSyncWorker.resultOf(SyncOutcome.Updated, 0))
        assertEquals(androidx.work.ListenableWorker.Result.success(), HijriSyncWorker.resultOf(SyncOutcome.NotModified, 0))
        assertEquals(androidx.work.ListenableWorker.Result.success(), HijriSyncWorker.resultOf(SyncOutcome.Rejected("x"), 0))
        assertEquals(retry, HijriSyncWorker.resultOf(SyncOutcome.NetworkError("x"), 0))
        assertEquals(retry, HijriSyncWorker.resultOf(SyncOutcome.NetworkError("x"), 3))
        assertEquals(retry, HijriSyncWorker.resultOf(SyncOutcome.StorageError("x"), 0))
        assertEquals(androidx.work.ListenableWorker.Result.failure(), HijriSyncWorker.resultOf(SyncOutcome.StorageError("x"), 4))
        assertEquals(androidx.work.ListenableWorker.Result.failure(), HijriSyncWorker.resultOf(SyncOutcome.NetworkError("x"), 4))
    }
}
