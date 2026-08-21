package kpt.feature.gpscamera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.function.Consumer
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

actual object GpsCameraPlatform {

    @Volatile
    private var imageCapture: ImageCapture? = null

    private var activityProvider: () -> Activity = {
        throw IllegalStateException(
            "GpsCameraPlatform activity provider not set. " +
                "Call GpsCameraPlatform.setActivityProvider() from the host Activity.",
        )
    }

    private var cameraPermissionLauncher: ActivityResultLauncher<String>? = null
    private var locationPermissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var cameraPermissionResult: CompletableDeferred<Boolean>? = null
    private var locationPermissionResult: CompletableDeferred<Boolean>? = null

    fun setActivityProvider(provider: () -> Activity) {
        activityProvider = provider
        val activity = provider() as? ComponentActivity ?: return
        cameraPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted -> cameraPermissionResult?.complete(granted) }
        locationPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { results ->
            locationPermissionResult?.complete(
                results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    results[Manifest.permission.ACCESS_COARSE_LOCATION] == true,
            )
        }
    }

    @Composable
    actual fun CameraPreview(modifier: Modifier, onError: (String) -> Unit) {
        val activity = activityProvider.invoke() as? ComponentActivity
        if (activity == null) {
            onError("Camera requires an Android activity")
            return
        }
        val previewView = remember {
            PreviewView(activity).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
        }
        DisposableEffect(activity) {
            val executor = ContextCompat.getMainExecutor(activity)
            val future = ProcessCameraProvider.getInstance(activity)
            future.addListener(
                {
                    try {
                        val provider = future.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            activity, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture,
                        )
                        imageCapture = capture
                    } catch (e: Exception) {
                        imageCapture = null
                        onError("Unable to start camera: ${e.message ?: "unknown error"}")
                    }
                },
                executor,
            )
            onDispose {
                imageCapture = null
                if (future.isDone) runCatching { future.get().unbindAll() }
            }
        }
        AndroidView(factory = { previewView }, modifier = modifier)
    }

    actual fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            activityProvider.invoke(), Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

    actual fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            activityProvider.invoke(), Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    actual suspend fun requestCameraPermission(): Boolean {
        if (hasCameraPermission()) return true
        val launcher = cameraPermissionLauncher ?: return false
        val deferred = CompletableDeferred<Boolean>()
        cameraPermissionResult = deferred
        launcher.launch(Manifest.permission.CAMERA)
        return deferred.await()
    }

    actual suspend fun requestLocationPermission(): Boolean {
        if (hasLocationPermission()) return true
        val launcher = locationPermissionLauncher ?: return false
        val deferred = CompletableDeferred<Boolean>()
        locationPermissionResult = deferred
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
        return deferred.await()
    }

    actual fun openAppSettings() {
        val activity = activityProvider.invoke()
        activity.startActivity(
            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", activity.packageName, null)
            },
        )
    }

    actual suspend fun capturePhoto(): String? {
        val activity = activityProvider.invoke()
        val capture = imageCapture ?: return null
        val directory = File(activity.cacheDir, "gps_camera").apply { mkdirs() }
        val file = File(directory, "photo_${System.currentTimeMillis()}.jpg")
        return suspendCancellableCoroutine { continuation ->
            capture.takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(),
                ContextCompat.getMainExecutor(activity),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(
                        outputFileResults: ImageCapture.OutputFileResults,
                    ) {
                        if (continuation.isActive) continuation.resume(file.absolutePath)
                    }
                    override fun onError(exception: ImageCaptureException) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                },
            )
        }
    }

    actual suspend fun fetchLocation(): LocationData {
        val activity = activityProvider.invoke()
        if (!hasLocationPermission()) return LocationData()
        val locationManager =
            activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val fresh = withTimeoutOrNull(8_000) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                suspendCancellableCoroutine { continuation ->
                    val consumer = Consumer<Location> { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }
                    try {
                        locationManager.getCurrentLocation(
                            LocationManager.GPS_PROVIDER,
                            null,
                            ContextCompat.getMainExecutor(activity),
                            consumer,
                        )
                    } catch (e: SecurityException) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            } else {
                suspendCancellableCoroutine { continuation ->
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            if (continuation.isActive) continuation.resume(location)
                        }
                    }
                    try {
                        @Suppress("DEPRECATION")
                        locationManager.requestSingleUpdate(
                            LocationManager.GPS_PROVIDER,
                            listener,
                            Looper.getMainLooper(),
                        )
                        continuation.invokeOnCancellation {
                            locationManager.removeUpdates(listener)
                        }
                    } catch (e: SecurityException) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            }
        }

        val location = fresh ?: lastKnownBest(locationManager)
        return if (location != null) {
            LocationData(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                address = reverseGeocode(activity, location),
                timestamp = location.time,
            )
        } else {
            LocationData()
        }
    }

    private fun reverseGeocode(context: Context, location: Location): String? = runCatching {
        if (!Geocoder.isPresent()) return null
        @Suppress("DEPRECATION")
        Geocoder(context).getFromLocation(location.latitude, location.longitude, 1)
            ?.firstOrNull()?.getAddressLine(0)
    }.getOrNull()

    private fun lastKnownBest(locationManager: LocationManager): Location? {
        val gps = try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (e: SecurityException) { null }
        val network = try {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: SecurityException) { null }
        return when {
            gps != null && network != null -> if (gps.accuracy <= network.accuracy) gps else network
            gps != null -> gps
            network != null -> network
            else -> null
        }
    }

    actual fun loadImageBitmap(path: String): ImageBitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, 512, 512)
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeFile(path, options)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(
        width: Int, height: Int, reqWidth: Int, reqHeight: Int,
    ): Int {
        var sampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / sampleSize) >= reqHeight &&
                (halfWidth / sampleSize) >= reqWidth
            ) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }
}
