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
    fun CameraPreview(modifier: Modifier = Modifier, onError: (String) -> Unit = {})

    fun hasCameraPermission(): Boolean

    fun hasLocationPermission(): Boolean

    /** Requests the camera runtime permission; returns true when granted. */
    suspend fun requestCameraPermission(): Boolean

    /** Requests fine/coarse location runtime permissions; returns true when granted. */
    suspend fun requestLocationPermission(): Boolean

    fun openAppSettings()

    /**
     * Launches the system camera and captures a photo.
     *
     * @return absolute file path of the captured image, or `null` when the
     * user cancelled, no camera app is available or capture failed.
     */
    suspend fun capturePhoto(): String?

    /**
     * Fetches the current GPS fix.
     *
     * @return the freshest available location, or [LocationData] defaults
     * when GPS is unavailable / permission is missing.
     */
    suspend fun fetchLocation(): LocationData

    /**
     * Decodes an image file into a displayable [ImageBitmap], downsampled
     * for thumbnail rendering. Returns `null` when the file is missing or
     * the platform cannot decode it.
     */
    fun loadImageBitmap(path: String): ImageBitmap?
}
