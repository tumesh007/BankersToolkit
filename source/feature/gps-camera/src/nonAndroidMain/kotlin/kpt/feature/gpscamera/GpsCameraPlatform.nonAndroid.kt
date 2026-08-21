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
    actual fun CameraPreview(modifier: Modifier, onError: (String) -> Unit) = Unit
    actual fun hasCameraPermission(): Boolean = false

    actual fun hasLocationPermission(): Boolean = false

    actual suspend fun requestCameraPermission(): Boolean = false

    actual suspend fun requestLocationPermission(): Boolean = false

    actual fun openAppSettings() = Unit

    actual suspend fun capturePhoto(): String? = null

    actual suspend fun fetchLocation(): LocationData = LocationData()

    actual fun loadImageBitmap(path: String): ImageBitmap? = null
}
