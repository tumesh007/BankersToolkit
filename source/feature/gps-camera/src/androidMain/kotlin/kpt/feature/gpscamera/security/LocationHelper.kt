package kpt.feature.gpscamera.security

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

data class VerifiedLocationResult(
    val location: Location?,
    val isMock: Boolean,
    val isAccuracyAcceptable: Boolean,
    val accuracyMeters: Float,
    val isGpsEnabled: Boolean,
    val errorMessage: String? = null
)

class LocationHelper(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun isGpsProviderEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /**
     * Inspects Location object across all Android API levels for Mock / Fake GPS indicators.
     */
    fun isLocationMocked(location: Location?): Boolean {
        if (location == null) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }
    }

    /**
     * Verifies that the location fix was obtained recently (within 30 seconds).
     */
    fun isLocationFresh(location: Location): Boolean {
        val locationAgeMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L
        } else {
            System.currentTimeMillis() - location.time
        }
        return locationAgeMs <= TimeUnit.SECONDS.toMillis(30)
    }

    /**
     * Real-time Location Updates as a Kotlin Coroutines Flow.
     */
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMs: Long = 3000L): Flow<VerifiedLocationResult> = callbackFlow {
        if (!isGpsProviderEnabled()) {
            trySend(
                VerifiedLocationResult(
                    location = null,
                    isMock = false,
                    isAccuracyAcceptable = false,
                    accuracyMeters = Float.MAX_VALUE,
                    isGpsEnabled = false,
                    errorMessage = "GPS is disabled on device"
                )
            )
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setMinUpdateDistanceMeters(1.0f)
            .setWaitForAccurateLocation(true)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val mock = isLocationMocked(loc)
                val acceptable = loc.hasAccuracy() && loc.accuracy <= 50.0f

                trySend(
                    VerifiedLocationResult(
                        location = loc,
                        isMock = mock,
                        isAccuracyAcceptable = acceptable,
                        accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else Float.MAX_VALUE,
                        isGpsEnabled = isGpsProviderEnabled(),
                        errorMessage = if (mock) "Spoofed / Mock GPS detected!" else null
                    )
                )
            }
        }

        fusedClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }

    /**
     * Fetches immediate single high-accuracy snapshot using FusedLocationProvider.
     */
    @SuppressLint("MissingPermission")
    suspend fun getFreshLocationSnapshot(): VerifiedLocationResult {
        return try {
            val loc = fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                ?: fusedClient.lastLocation.await()

            if (loc != null) {
                val mock = isLocationMocked(loc)
                VerifiedLocationResult(
                    location = loc,
                    isMock = mock,
                    isAccuracyAcceptable = loc.hasAccuracy() && loc.accuracy <= 50f,
                    accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else Float.MAX_VALUE,
                    isGpsEnabled = isGpsProviderEnabled(),
                    errorMessage = if (mock) "Mock location rejected" else null
                )
            } else {
                VerifiedLocationResult(
                    location = null,
                    isMock = false,
                    isAccuracyAcceptable = false,
                    accuracyMeters = Float.MAX_VALUE,
                    isGpsEnabled = isGpsProviderEnabled(),
                    errorMessage = "Unable to acquire GPS fix"
                )
            }
        } catch (e: Exception) {
            VerifiedLocationResult(
                location = null,
                isMock = false,
                isAccuracyAcceptable = false,
                accuracyMeters = Float.MAX_VALUE,
                isGpsEnabled = isGpsProviderEnabled(),
                errorMessage = e.message
            )
        }
    }
}
