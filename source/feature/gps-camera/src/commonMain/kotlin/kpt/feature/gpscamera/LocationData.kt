package kpt.feature.gpscamera

import kotlin.time.Clock

data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val address: String? = null,
    val street: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val isMock: Boolean = false,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

val LocationData.isValid: Boolean
    // 0/0 is the feature's explicit "no fix" sentinel; never present it as GPS.
    get() = (latitude != 0.0 || longitude != 0.0) &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0

val LocationData.isAccuracyAcceptable: Boolean
    get() = isValid && accuracy > 0f && accuracy <= 50.0f && !isMock

