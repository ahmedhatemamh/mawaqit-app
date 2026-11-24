package work.ahmedhatem.mawaqit.hijri

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.json.JSONException
import org.json.JSONObject
import work.ahmedhatem.mawaqit.engine.HijriCalendar
import work.ahmedhatem.mawaqit.engine.HijriMonthStart

/** What the last syncs left behind: the server's ETag of the stored file and when the server last answered. */
data class HijriSyncState(val etag: String?, val lastSuccessMillis: Long?, val lastAttemptMillis: Long?)

/**
 * The synced Hijri file and its bookkeeping, in [dir].
 *
 * The file is written to a temporary file first and then moved over the old one in one step, so a crash or a full disk
 * leaves the old file whole. The stored file is checked again on every read: a damaged or unsuitable file is ignored
 * and the months bundled in the app are used instead.
 */
class HijriStore(private val dir: File) {
    private val data = File(dir, "hijri.json")
    private val meta = File(dir, "hijri-meta.json")

    /** The month starts to use now: the stored file if it is valid, otherwise the bundled ones. */
    fun loadMonths(): List<HijriMonthStart> = stored() ?: HijriCalendar.bundledMonths()

    /** The stored file's months, or null when there is none or it is not valid. */
    private fun stored(): List<HijriMonthStart>? {
        if (!data.isFile) return null
        return try {
            if (data.length() > HijriParser.MAX_BYTES) return null
            val months = HijriParser.parse(String(Files.readAllBytes(data.toPath()), StandardCharsets.UTF_8))
            HijriParser.checkReplaces(HijriCalendar.bundledMonths(), months)
            months
        } catch (e: HijriDataException) {
            null
        } catch (e: IOException) {
            null
        }
    }

    fun state(): HijriSyncState {
        val empty = HijriSyncState(null, null, null)
        if (!meta.isFile) return empty
        return try {
            val json = JSONObject(String(Files.readAllBytes(meta.toPath()), StandardCharsets.UTF_8))
            HijriSyncState(
                etag = json.opt("etag") as? String,
                lastSuccessMillis = (json.opt("lastSuccess") as? Number)?.toLong(),
                lastAttemptMillis = (json.opt("lastAttempt") as? Number)?.toLong(),
            )
        } catch (e: JSONException) {
            empty
        } catch (e: IOException) {
            empty
        }
    }

    /**
     * Keeps a validated file. The meta file is written after the data file, so if the process dies in between, the
     * ETag is older than the data and the next check simply downloads once more.
     */
    fun saveFile(text: String, etag: String?, nowMillis: Long) {
        writeAtomically(data, text)
        writeState(HijriSyncState(etag, nowMillis, nowMillis))
    }

    /** The server answered "not modified". Its ETag stays; the check counts as a success. */
    fun recordNotModified(nowMillis: Long) {
        writeState(state().copy(lastSuccessMillis = nowMillis, lastAttemptMillis = nowMillis))
    }

    /** The server answered but the file was refused. Nothing else changes, and the ETag of the rejected file is not kept. */
    fun recordRejected(nowMillis: Long) {
        writeState(state().copy(lastAttemptMillis = nowMillis))
    }

    private fun writeState(state: HijriSyncState) {
        val json = JSONObject()
        json.put("etag", state.etag ?: JSONObject.NULL)
        json.put("lastSuccess", state.lastSuccessMillis ?: JSONObject.NULL)
        json.put("lastAttempt", state.lastAttemptMillis ?: JSONObject.NULL)
        writeAtomically(meta, json.toString())
    }

    private fun writeAtomically(target: File, text: String) {
        dir.mkdirs()
        val temporary = File(dir, target.name + ".tmp")
        FileOutputStream(temporary).use { out ->
            out.write(text.toByteArray(StandardCharsets.UTF_8))
            out.fd.sync()
        }
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }
}
