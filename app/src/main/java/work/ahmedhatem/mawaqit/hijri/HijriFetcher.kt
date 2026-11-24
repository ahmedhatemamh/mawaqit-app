package work.ahmedhatem.mawaqit.hijri

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.ByteBuffer

/** The answer of the data server. */
sealed interface FetchResult {
    /** The ETag we sent still matches: nothing changed. */
    data object NotModified : FetchResult

    /** A new file, not yet checked. */
    data class Downloaded(val text: String, val etag: String?) : FetchResult
}

/**
 * Downloads the Hijri file from one fixed address. Anything but a plain 200 or 304 is an [IOException] (the caller
 * retries later): redirects are not followed, and a body over [HijriParser.MAX_BYTES] is refused while it is read.
 */
class HijriFetcher(
    private val url: URL = URL(HIJRI_URL),
    private val timeoutMillis: Int = 20_000,
) {
    fun fetch(etag: String?): FetchResult {
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            connection.setRequestProperty("Accept", "application/json")
            if (etag != null) connection.setRequestProperty("If-None-Match", etag)
            return when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_NOT_MODIFIED -> FetchResult.NotModified
                HttpURLConnection.HTTP_OK -> FetchResult.Downloaded(readBody(connection), connection.getHeaderField("ETag"))
                else -> throw IOException("unexpected HTTP status $code")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun readBody(connection: HttpURLConnection): String {
        val declared = connection.contentLengthLong
        if (declared > HijriParser.MAX_BYTES) throw HijriDataException("the file is larger than ${HijriParser.MAX_BYTES} bytes")
        val bytes = connection.inputStream.use { input ->
            val buffer = ByteArray(8192)
            val out = java.io.ByteArrayOutputStream()
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                if (out.size() + n > HijriParser.MAX_BYTES) throw HijriDataException("the file is larger than ${HijriParser.MAX_BYTES} bytes")
                out.write(buffer, 0, n)
            }
            out.toByteArray()
        }
        // A connection cut in the middle can look like a normal end of the body: compare with the announced length.
        if (declared >= 0 && bytes.size.toLong() != declared) throw IOException("the body ended early: ${bytes.size} of $declared bytes")
        return try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: CharacterCodingException) {
            throw HijriDataException("the file is not valid UTF-8")
        }
    }

    companion object {
        const val HIJRI_URL = "https://data.ahmedhatem.work/hijri.json"
    }
}
