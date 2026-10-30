package kpt.feature.gpscamera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.FileReader
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual object GpsCameraPlatform {

    @Composable
    actual fun CameraPreview(
        modifier: Modifier,
        isFrontCamera: Boolean,
        isTorchOn: Boolean,
        onError: (String) -> Unit
    ) {
        Box(
            modifier = modifier.background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Web App Mode",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Click 'UPLOAD FROM GALLERY' to pick photos from your device",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            }
        }
    }

    actual fun hasCameraPermission(): Boolean = true

    actual fun hasLocationPermission(): Boolean = true

    actual suspend fun requestCameraPermission(): Boolean = true

    actual suspend fun requestLocationPermission(): Boolean = true

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

    actual suspend fun fetchLocation(): LocationData {
        return LocationData(
            latitude = 0.0,
            longitude = 0.0,
            accuracy = 0f,
            address = "Web Browser",
            timestamp = window.performance.now().toLong(),
            isMock = false
        )
    }

    actual suspend fun reverseGeocode(latitude: Double, longitude: Double): String? = null

    actual suspend fun pickPhotoFromGallery(): String? {
        val deferred = CompletableDeferred<String?>()
        try {
            val input = document.createElement("input") as HTMLInputElement
            input.type = "file"
            input.accept = "image/*"

            input.onchange = {
                val files = input.files
                if (files != null && files.length > 0) {
                    val file = files.item(0)
                    if (file != null) {
                        val reader = FileReader()
                        reader.onload = {
                            val result = reader.result
                            deferred.complete(result?.toString())
                        }
                        reader.onerror = {
                            deferred.complete(null)
                        }
                        reader.readAsDataURL(file)
                    } else {
                        deferred.complete(null)
                    }
                } else {
                    deferred.complete(null)
                }
            }

            input.click()
        } catch (_: Throwable) {
            deferred.complete(null)
        }
        return deferred.await()
    }

    @OptIn(ExperimentalEncodingApi::class)
    actual fun loadImageBitmap(path: String): ImageBitmap? {
        return try {
            val base64Data = when {
                path.startsWith("data:") && path.contains(",") -> path.substringAfter(",")
                else -> path
            }
            val bytes = Base64.decode(base64Data)
            org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (_: Throwable) {
            null
        }
    }

    actual suspend fun extractPhotoMetadata(path: String): PhotoMetadata? {
        return PhotoMetadata(
            timestamp = window.performance.now().toLong(),
            latitude = null,
            longitude = null
        )
    }

    actual fun sharePhoto(path: String) {
        try {
            if (path.startsWith("data:")) {
                val win = window.open("", "_blank")
                win?.document?.write("<img src=\"$path\" style=\"max-width:100%;height:auto;\" />")
            }
        } catch (_: Throwable) {}
    }

    @OptIn(ExperimentalEncodingApi::class)
    actual suspend fun savePhotoToGallery(path: String): Boolean {
        return try {
            val base64Data = when {
                path.startsWith("data:") && path.contains(",") -> path.substringAfter(",")
                else -> path
            }
            val bytes = Base64.decode(base64Data)
            val int8Array = Int8Array(bytes.size)
            for (i in bytes.indices) {
                int8Array[i] = bytes[i]
            }

            val blobProps = BlobPropertyBag(type = "image/jpeg")
            val parts = js("[]")
            parts.push(int8Array)
            val blob = Blob(parts, blobProps)

            val url = URL.createObjectURL(blob)
            val anchor = document.createElement("a") as HTMLAnchorElement
            anchor.href = url
            anchor.download = "bankers_toolkit_${window.performance.now().toLong()}.jpg"
            document.body?.appendChild(anchor)
            anchor.click()
            document.body?.removeChild(anchor)
            URL.revokeObjectURL(url)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
