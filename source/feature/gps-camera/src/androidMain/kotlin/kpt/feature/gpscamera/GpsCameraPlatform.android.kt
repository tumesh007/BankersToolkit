package kpt.feature.gpscamera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
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
import kpt.feature.gpscamera.geocoding.GeocoderHelper
import kpt.feature.gpscamera.graphics.WatermarkMetadata
import kpt.feature.gpscamera.graphics.WatermarkUtils
import kpt.feature.gpscamera.security.ExifHelper
import kpt.feature.gpscamera.security.LocationHelper
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

actual object GpsCameraPlatform {

    @Volatile
    private var imageCapture: ImageCapture? = null

    @Volatile
    private var activeCamera: Camera? = null

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
    
    private var galleryLauncher: ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest>? = null
    private var galleryResult: CompletableDeferred<android.net.Uri?>? = null

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
        galleryLauncher = activity.registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            galleryResult?.complete(uri)
        }
    }

    @Composable
    actual fun CameraPreview(
        modifier: Modifier,
        isFrontCamera: Boolean,
        isTorchOn: Boolean,
        onError: (String) -> Unit,
    ) {
        val activity = activityProvider.invoke() as? ComponentActivity
        if (activity == null) {
            onError("Camera requires an Android activity")
            return
        }
        val previewView = remember {
            PreviewView(activity).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
        }

        DisposableEffect(activity, isFrontCamera) {
            val executor = ContextCompat.getMainExecutor(activity)
            val future = ProcessCameraProvider.getInstance(activity)
            future.addListener(
                {
                    try {
                        val provider = future.get()
                        val preferredSelector = if (isFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                        val cameraSelector = if (provider.hasCamera(preferredSelector)) {
                            preferredSelector
                        } else if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            preferredSelector
                        }

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .build()
                        provider.unbindAll()
                        val cam = provider.bindToLifecycle(
                            activity, cameraSelector, preview, capture,
                        )
                        imageCapture = capture
                        activeCamera = cam

                        if (cam.cameraInfo.hasFlashUnit()) {
                            cam.cameraControl.enableTorch(isTorchOn)
                        }
                    } catch (e: Exception) {
                        imageCapture = null
                        activeCamera = null
                        onError("Unable to start camera: ${e.message ?: "unknown error"}")
                    }
                },
                executor,
            )
            onDispose {
                imageCapture = null
                activeCamera = null
                if (future.isDone) runCatching { future.get().unbindAll() }
            }
        }

        // Handle torch toggle without rebuilding preview
        DisposableEffect(isTorchOn) {
            activeCamera?.let { cam ->
                if (cam.cameraInfo.hasFlashUnit()) {
                    cam.cameraControl.enableTorch(isTorchOn)
                }
            }
            onDispose { }
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

    actual suspend fun capturePhoto(
        caseNumber: String,
        officerName: String,
        officerId: String,
        category: String,
        locationData: LocationData
    ): String? {
        val activity = activityProvider.invoke()
        val capture = imageCapture ?: return null
        val directory = File(activity.filesDir, "gps_camera").apply { mkdirs() }
        val rawFile = File(directory, "photo_${System.currentTimeMillis()}.jpg")

        val rawSaved = suspendCancellableCoroutine<Boolean> { continuation ->
            capture.takePicture(
                ImageCapture.OutputFileOptions.Builder(rawFile).build(),
                ContextCompat.getMainExecutor(activity),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        if (continuation.isActive) continuation.resume(true)
                    }
                    override fun onError(exception: ImageCaptureException) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                },
            )
        }

        if (!rawSaved || !rawFile.exists()) return null

        return withContext(Dispatchers.IO) {
            try {
                // Ensure freshest location / address
                val finalLoc = if (locationData.isValid) locationData else fetchLocation()
                
                // Fetch optional mini-map thumbnail (network call, run in IO)
                val mapThumb = if (finalLoc.isValid && !finalLoc.isMock) {
                    kpt.feature.gpscamera.graphics.MiniMapHelper.fetchMapThumbnail(
                        latitude = finalLoc.latitude,
                        longitude = finalLoc.longitude
                    )
                } else null

                // 1. Watermark Bitmap Stamping directly onto pixels
                val metadata = WatermarkMetadata(
                    organizationName = "Banker's Toolkit - Verification",
                    officerName = officerName,
                    officerId = officerId,
                    caseNumber = caseNumber,
                    category = category,
                    latitude = finalLoc.latitude,
                    longitude = finalLoc.longitude,
                    accuracyMeters = finalLoc.accuracy,
                    address = finalLoc.address ?: "Address unavailable",
                    timestamp = System.currentTimeMillis(),
                    isMockLocation = finalLoc.isMock,
                    mapThumbnail = mapThumb
                )
                val stampedFile = WatermarkUtils.stampAndSave(rawFile, metadata)

                // 2. EXIF Metadata Embedding
                ExifHelper.writeGpsAndAuditMetadata(
                    imageFile = stampedFile,
                    latitude = finalLoc.latitude,
                    longitude = finalLoc.longitude,
                    caseNumber = caseNumber,
                    officerId = officerId,
                    officerName = officerName,
                    address = finalLoc.address ?: "",
                    category = category,
                    timestamp = metadata.timestamp
                )

                stampedFile.absolutePath
            } catch (e: Exception) {
                // Fallback to raw file path if stamping fails
                rawFile.absolutePath
            }
        }
    }

    actual suspend fun stampPhoto(
        originalPath: String,
        caseNumber: String,
        officerName: String,
        officerId: String,
        category: String,
        locationData: LocationData
    ): String {
        return withContext(Dispatchers.IO) {
            val rawFile = File(originalPath)
            if (!rawFile.exists()) return@withContext originalPath
            
            try {
                // Fetch optional mini-map thumbnail (network call, run in IO)
                val mapThumb = if (locationData.isValid && !locationData.isMock) {
                    kpt.feature.gpscamera.graphics.MiniMapHelper.fetchMapThumbnail(
                        latitude = locationData.latitude,
                        longitude = locationData.longitude
                    )
                } else null

                // 1. Watermark Bitmap Stamping directly onto pixels
                val metadata = WatermarkMetadata(
                    organizationName = "Banker's Toolkit - Verification",
                    officerName = officerName,
                    officerId = officerId,
                    caseNumber = caseNumber,
                    category = category,
                    latitude = locationData.latitude,
                    longitude = locationData.longitude,
                    accuracyMeters = locationData.accuracy,
                    address = locationData.address ?: "Address unavailable",
                    timestamp = locationData.timestamp,
                    isMockLocation = locationData.isMock,
                    mapThumbnail = mapThumb
                )
                val stampedFile = WatermarkUtils.stampAndSave(rawFile, metadata)

                // 2. EXIF Metadata Embedding
                ExifHelper.writeGpsAndAuditMetadata(
                    imageFile = stampedFile,
                    latitude = locationData.latitude,
                    longitude = locationData.longitude,
                    caseNumber = caseNumber,
                    officerId = officerId,
                    officerName = officerName,
                    address = locationData.address ?: "",
                    category = category,
                    timestamp = metadata.timestamp
                )

                stampedFile.absolutePath
            } catch (e: Exception) {
                originalPath
            }
        }
    }

    actual suspend fun fetchLocation(): LocationData {
        val activity = activityProvider.invoke()
        if (!hasLocationPermission()) return LocationData()

        val locationHelper = LocationHelper(activity)
        val geocoderHelper = GeocoderHelper(activity)

        val verified = locationHelper.getFreshLocationSnapshot()
        val loc = verified.location ?: return LocationData(isMock = verified.isMock)

        val address = geocoderHelper.reverseGeocode(loc.latitude, loc.longitude)

        return LocationData(
            latitude = loc.latitude,
            longitude = loc.longitude,
            accuracy = loc.accuracy,
            address = address.fullAddress,
            street = address.street,
            city = address.city,
            state = address.state,
            pincode = address.postalCode,
            isMock = verified.isMock,
            timestamp = loc.time,
        )
    }

    actual suspend fun reverseGeocode(latitude: Double, longitude: Double): String? {
        val activity = activityProvider.invoke()
        val geocoderHelper = GeocoderHelper(activity)
        val address = geocoderHelper.reverseGeocode(latitude, longitude)
        return address.fullAddress.takeIf { it.isNotBlank() }
    }

    actual suspend fun pickPhotoFromGallery(): String? {
        val launcher = galleryLauncher ?: return null
        galleryResult = CompletableDeferred()
        
        launcher.launch(
            androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
        
        val uri = galleryResult?.await() ?: return null
        galleryResult = null
        
        val activity = activityProvider.invoke()
        
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = activity.contentResolver.openInputStream(uri) ?: return@withContext null
                val cacheDir = File(activity.filesDir, "gallery_images").apply { mkdirs() }
                val tempFile = File(cacheDir, "picked_image_${System.currentTimeMillis()}.jpg")
                
                tempFile.outputStream().use { outputStream ->
                    inputStream.use { input ->
                        input.copyTo(outputStream)
                    }
                }
                
                tempFile.absolutePath
            } catch (e: Exception) {
                null
            }
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

    actual suspend fun extractPhotoMetadata(path: String): PhotoMetadata? {
        return withContext(Dispatchers.IO) {
            kpt.feature.gpscamera.security.ExifHelper.readOriginalMetadata(path)
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

    actual fun sharePhoto(path: String) {
        val activity = activityProvider.invoke()
        val file = File(path)
        if (!file.exists()) return
        
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(Intent.createChooser(intent, "Share Photo"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    actual suspend fun savePhotoToGallery(path: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val activity = activityProvider.invoke()
                val file = File(path)
                if (!file.exists()) return@withContext false
                
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, file.name)
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/BankersToolkit")
                        put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                
                val uri = activity.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false
                    
                activity.contentResolver.openOutputStream(uri)?.use { output ->
                    file.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                    activity.contentResolver.update(uri, values, null, null)
                }
                
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
