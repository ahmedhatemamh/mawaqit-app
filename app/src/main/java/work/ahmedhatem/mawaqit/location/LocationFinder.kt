package work.ahmedhatem.mawaqit.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

/** The answer of one position request. */
sealed interface LocationResult {
    data class Found(val latitude: Double, val longitude: Double) : LocationResult
    /** The phone's location service is switched off. */
    data object Off : LocationResult
    /** No position arrived in time. */
    data object Unavailable : LocationResult
}

/**
 * One approximate position, asked for only when the user presses the button. It needs the approximate-location
 * permission only (the network provider), never the precise one, and nothing is tracked afterwards.
 */
class LocationFinder(private val context: Context) {
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun find(onResult: (LocationResult) -> Unit) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!hasPermission()) {
            onResult(LocationResult.Unavailable)
            return
        }
        if (!LocationManagerCompat.isLocationEnabled(manager) || !manager.isProviderEnabled(PROVIDER)) {
            onResult(LocationResult.Off)
            return
        }
        val handler = Handler(Looper.getMainLooper())
        val cancel = CancellationSignal()
        var done = false
        fun finish(location: Location?) {
            if (done) return
            done = true
            handler.removeCallbacksAndMessages(null)
            val last = location ?: runCatching { manager.getLastKnownLocation(PROVIDER) }.getOrNull()
            onResult(if (last == null) LocationResult.Unavailable else LocationResult.Found(last.latitude, last.longitude))
        }
        handler.postDelayed({ cancel.cancel(); finish(null) }, TIMEOUT_MILLIS)
        LocationManagerCompat.getCurrentLocation(manager, PROVIDER, cancel, ContextCompat.getMainExecutor(context)) {
            finish(it)
        }
    }

    private companion object {
        const val PROVIDER = LocationManager.NETWORK_PROVIDER
        const val TIMEOUT_MILLIS = 15_000L
    }
}
