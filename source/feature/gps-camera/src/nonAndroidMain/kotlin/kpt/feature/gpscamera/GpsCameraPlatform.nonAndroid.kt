package kpt.feature.gpscamera

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Non-Android degradation: no camera, no GPS. The screen renders with
 * permissions unavailable and capture disabled — the feature targets the
 * Android field-tool experience.
 */
actual object GpsCameraPlatform {
    @Composable
    actual fun CameraPreview(
        modifier: Modifier,
        isFrontCamera: Boolean,
        isTorchOn: Boolean,
        onError: (String) -> Unit
    ) = Unit
    actual fun hasCameraPermission(): Boolean = false

    actual fun hasLocationPermission(): Boolean = false

    actual suspend fun requestCameraPermission(): Boolean = false

    actual suspend fun requestLocationPermission(): Boolean = false

    actual fun openAppSettings() = Unit

    actual suspend fun capturePhoto(
        caseNumber: String,
        officerName: String,
        officerId: String,
        category: String,
        locationData: LocationData
    ): String? = null

    actual suspend fun stampPhoto(
        originalPath: String,
        caseNumber: String,
        officerName: String,
        officerId: String,
        category: String,
        locationData: LocationData
    ): String = originalPath

    actual suspend fun fetchLocation(): LocationData = LocationData()

    actual suspend fun reverseGeocode(latitude: Double, longitude: Double): String? = null

    actual suspend fun pickPhotoFromGallery(): String? = null

    actual fun loadImageBitmap(path: String): ImageBitmap? = null

    actual suspend fun extractPhotoMetadata(path: String): PhotoMetadata? = null

    actual fun sharePhoto(path: String) = Unit
    
    actual suspend fun savePhotoToGallery(path: String): Boolean = false
}
