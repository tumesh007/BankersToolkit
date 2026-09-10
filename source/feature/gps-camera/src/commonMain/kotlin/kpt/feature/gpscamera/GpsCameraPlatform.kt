package kpt.feature.gpscamera

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform bridge for the GPS Camera feature.
 *
 * On Android the actual implementation drives the system camera app
 * ([android.app.Activity] launched via an [androidx.activity.result.ActivityResultContracts.TakePicture]
 * contract), requests runtime permissions and reads the fused GPS fix from
 * [android.location.LocationManager]. Non-Android targets degrade gracefully
 * (no camera, no GPS — see the `nonAndroidMain` actual).
 *
 * The host Android activity MUST register itself via
 * `GpsCameraPlatform.setActivityProvider { this }` (see MainActivity) before
 * any of the suspend APIs are called.
 */
expect object GpsCameraPlatform {

    /** Android hosts render a CameraX preview; other platforms show no preview. */
    @Composable
    fun CameraPreview(
        modifier: Modifier = Modifier,
        isFrontCamera: Boolean = false,
        isTorchOn: Boolean = false,
        onError: (String) -> Unit = {}
    )

    fun hasCameraPermission(): Boolean

    fun hasLocationPermission(): Boolean

    /** Requests the camera runtime permission; returns true when granted. */
    suspend fun requestCameraPermission(): Boolean

    /** Requests fine/coarse location runtime permissions; returns true when granted. */
    suspend fun requestLocationPermission(): Boolean

    fun openAppSettings()

    /**
     * Captures a high-resolution photo with verified GPS coordinates, tamper-proof
     * canvas watermark stamping, and standard EXIF metadata embedding.
     *
     * @return absolute file path of the stamped image, or `null` when capture failed.
     */
    suspend fun capturePhoto(
        caseNumber: String = "",
        officerName: String = "",
        officerId: String = "",
        category: String = "",
        locationData: LocationData = LocationData()
    ): String?

    /**
     * Fetches the current high-accuracy GPS fix with mock location checks and geocoding.
     */
    suspend fun fetchLocation(): LocationData

    /**
     * Stamps an existing image with a watermark and EXIF metadata.
     * @return absolute path to the newly stamped image file.
     */
    suspend fun stampPhoto(
        originalPath: String,
        caseNumber: String = "",
        officerName: String = "",
        officerId: String = "",
        category: String = "",
        locationData: LocationData
    ): String
    
    /**
     * Reverse geocodes the given coordinates to a physical address string.
     */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): String?

    /**
     * Launches the system gallery/picker to select an existing photo.
     * @return absolute file path of a local copy of the selected image, or null if cancelled.
     */
    suspend fun pickPhotoFromGallery(): String?

    /**
     * Decodes an image file into a displayable [ImageBitmap], downsampled
     * for thumbnail rendering. Returns `null` when the file is missing or
     * the platform cannot decode it.
     */
    fun loadImageBitmap(path: String): ImageBitmap?

    /**
     * Extracts original timestamp and GPS coordinates from the photo's EXIF metadata.
     */
    suspend fun extractPhotoMetadata(path: String): PhotoMetadata?

    /**
     * Triggers a system share sheet for the given photo path.
     */
    fun sharePhoto(path: String)

    /**
     * Copies the photo to the system's public Pictures/Gallery directory.
     * @return true if successful
     */
    suspend fun savePhotoToGallery(path: String): Boolean
}

data class PhotoMetadata(
    val timestamp: Long?,
    val latitude: Double?,
    val longitude: Double?
)
