package work.ahmedhatem.mawaqit.hijri

import java.io.IOException

/** How one sync attempt ended. */
sealed interface SyncOutcome {
    data object Updated : SyncOutcome
    data object NotModified : SyncOutcome

    /** The server sent a file that was refused (the old data stays). Trying again at once would not help. */
    data class Rejected(val reason: String) : SyncOutcome

    /** The server could not be reached or answered badly. Worth trying again later. */
    data class NetworkError(val reason: String) : SyncOutcome
}

/** One sync attempt: ask the server (with the stored ETag), check the file, keep it only if it is valid. */
class HijriSync(private val store: HijriStore, private val fetcher: HijriFetcher) {
    fun run(nowMillis: Long): SyncOutcome {
        val result = try {
            fetcher.fetch(store.state().etag)
        } catch (e: HijriDataException) {
            store.recordRejected(nowMillis)
            return SyncOutcome.Rejected(e.message ?: "bad file")
        } catch (e: IOException) {
            return SyncOutcome.NetworkError(e.message ?: e.javaClass.simpleName)
        }
        return when (result) {
            FetchResult.NotModified -> {
                store.recordNotModified(nowMillis)
                SyncOutcome.NotModified
            }
            is FetchResult.Downloaded -> try {
                val months = HijriParser.parse(result.text)
                HijriParser.checkReplaces(store.loadMonths(), months)
                store.saveFile(result.text, result.etag, nowMillis)
                SyncOutcome.Updated
            } catch (e: HijriDataException) {
                store.recordRejected(nowMillis)
                SyncOutcome.Rejected(e.message ?: "bad file")
            }
        }
    }

    companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000

        /** The app-open rule (decision 3a): sync when the last answered attempt is older than a day, or there is none. */
        fun isStale(state: HijriSyncState, nowMillis: Long): Boolean {
            val last = state.lastAttemptMillis ?: return true
            return nowMillis - last >= DAY_MILLIS || nowMillis < last
        }
    }
}
