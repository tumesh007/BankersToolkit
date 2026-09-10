package kpt.feature.gpscamera.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kpt.core.model.visit.VisitPhoto
import kpt.feature.gpscamera.GpsCameraPlatform
import kpt.feature.gpscamera.isAccuracyAcceptable
import kpt.feature.gpscamera.isValid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsCameraScreen(
    viewModel: GpsCameraViewModel,
    onPhotoCaptured: (VisitPhoto) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.lastCapturedPhoto) {
        state.lastCapturedPhoto?.let(onPhotoCaptured)
    }

    // Keep GPS fix fresh while destination is visible
    LaunchedEffect(state.locationGranted) {
        while (state.locationGranted) {
            viewModel.refreshLocation()
            kotlinx.coroutines.delay(10_000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.visitId != null) "Verified Visit Camera" else "GPS Verification Camera",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.cameraGranted) {
                        // Flashlight Toggle
                        IconButton(
                            onClick = { viewModel.toggleTorch() },
                            enabled = !state.isCapturing && !state.isCheckingPermissions
                        ) {
                            Icon(
                                imageVector = if (state.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Flashlight",
                                tint = if (state.isTorchOn) Color(0xFFFFD700) else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Grid Toggle
                        IconButton(
                            onClick = { viewModel.toggleGrid() },
                            enabled = !state.isCapturing && !state.isCheckingPermissions
                        ) {
                            Icon(
                                imageVector = if (state.showGrid) Icons.Default.GridOn else Icons.Default.GridOff,
                                contentDescription = "Grid",
                                tint = if (state.showGrid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Switch Camera
                        IconButton(
                            onClick = { viewModel.toggleCamera() },
                            enabled = !state.isCapturing && !state.isCheckingPermissions
                        ) {
                            Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch Camera")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Camera Viewfinder Card with Live Overlay
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when {
                            state.isCheckingPermissions -> {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Checking permissions...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            !state.cameraGranted -> {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Camera",
                                    tint = Color.White,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Camera permission required",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { viewModel.requestCameraPermission() }) {
                                    Text("GRANT CAMERA ACCESS", fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = viewModel::openAppSettings) {
                                    Text("OPEN APP SETTINGS")
                                }
                            }

                            else -> {
                                GpsCameraPlatform.CameraPreview(
                                    modifier = Modifier.fillMaxSize(),
                                    isFrontCamera = state.isFrontCamera,
                                    isTorchOn = state.isTorchOn,
                                    onError = { /* capture control surfaces platform errors below */ },
                                )
                            }
                        }
                    }

                    // Rule-of-Thirds Grid Overlay
                    if (state.cameraGranted && state.showGrid && !state.isCheckingPermissions) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeColor = Color.White.copy(alpha = 0.25f)
                            val w = size.width
                            val h = size.height

                            drawLine(strokeColor, Offset(w / 3, 0f), Offset(w / 3, h), strokeWidth = 1.5f)
                            drawLine(strokeColor, Offset(w * 2 / 3, 0f), Offset(w * 2 / 3, h), strokeWidth = 1.5f)
                            drawLine(strokeColor, Offset(0f, h / 3), Offset(w, h / 3), strokeWidth = 1.5f)
                            drawLine(strokeColor, Offset(0f, h * 2 / 3), Offset(w, h * 2 / 3), strokeWidth = 1.5f)
                        }
                    }

                    // Floating Switch Camera Button overlay on top-right of viewfinder
                    if (state.cameraGranted && !state.isCheckingPermissions) {
                        IconButton(
                            onClick = { viewModel.toggleCamera() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.55f), shape = CircleShape),
                            enabled = !state.isCapturing
                        ) {
                            Icon(
                                Icons.Default.FlipCameraAndroid,
                                contentDescription = "Switch Camera",
                                tint = Color.White
                            )
                        }
                    }

                    // Live Verified Watermark / GPS Overlay Box
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            // GPS Lock / Mock Status Indicator Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when {
                                            state.location.isMock -> Icons.Default.Warning
                                            state.location.isAccuracyAcceptable -> Icons.Default.GpsFixed
                                            else -> Icons.Default.GpsNotFixed
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            state.location.isMock -> Color(0xFFFF4444)
                                            state.location.isAccuracyAcceptable -> Color(0xFF00E676)
                                            else -> Color(0xFFFFEB3B)
                                        },
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when {
                                            state.location.isMock -> "SPOOFED GPS DETECTED"
                                            state.location.isAccuracyAcceptable -> "GPS LOCKED (±${state.location.accuracy.toInt()}m)"
                                            state.isFetchingLocation -> "LOCATING GPS FIX..."
                                            else -> "ACQUIRING GPS FIX..."
                                        },
                                        color = when {
                                            state.location.isMock -> Color(0xFFFF4444)
                                            state.location.isAccuracyAcceptable -> Color(0xFF00E676)
                                            else -> Color(0xFFFFEB3B)
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (state.caseNumber.isNotBlank()) {
                                    Text(
                                        text = "CASE: ${state.caseNumber}",
                                        color = Color(0xFFFFD700),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (state.location.isValid) {
                                Text(
                                    "GPS: ${formatCoordinate(state.location.latitude)}, ${formatCoordinate(state.location.longitude)}",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                            if (!state.location.address.isNullOrEmpty()) {
                                Text(
                                    "Location: ${state.location.address}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                            Text(
                                "Category: ${state.selectedCategory.displayName}",
                                color = Color(0xFFFFD700),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Location Permission / GPS Controls
            if (!state.isCheckingPermissions && !state.locationGranted) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "High-accuracy location is required to stamp verified GPS on photos",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 13.sp
                        )
                        TextButton(onClick = { viewModel.requestLocationPermission() }) {
                            Text("ENABLE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (state.locationGranted) {
                OutlinedButton(
                    onClick = { viewModel.refreshLocation() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isFetchingLocation
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (state.isFetchingLocation) "Acquiring High-Accuracy GPS..." else "REFRESH GPS SNAPSHOT")
                }
            }

            // Photo Category Selection
            Text(
                "Inspection Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            @OptIn(ExperimentalLayoutApi::class) androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                kpt.core.model.visit.PhotoCategory.entries.forEach { category ->
                    FilterChip(
                        selected = state.selectedCategory == category,
                        onClick = { viewModel.updateCategory(category) },
                        label = { Text(category.displayName) },
                        leadingIcon = if (state.selectedCategory == category) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // Photo Caption Input
            OutlinedTextField(
                value = state.caption,
                onValueChange = { viewModel.updateCaption(it) },
                label = { Text("Photo Notes / Inspection Observation") },
                placeholder = { Text("e.g. Verified residential entrance and electricity meter") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Status notification bar
            state.statusMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(msg, color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 13.sp)
                    }
                }
            }

            // Error notification bar
            state.errorMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                    }
                }
            }

            // Capture Action Row with Switch Camera button
            val isCaptureBlocked = state.location.isMock || !state.cameraGranted || state.isCapturing || state.isCheckingPermissions

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.capturePhoto() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isCaptureBlocked
                ) {
                    if (state.isCapturing) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VERIFYING & STAMPING...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CAPTURE VERIFIED PHOTO", fontWeight = FontWeight.Bold)
                    }
                }

                FilledTonalIconButton(
                    onClick = { viewModel.toggleCamera() },
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = state.cameraGranted && !state.isCapturing && !state.isCheckingPermissions
                ) {
                    Icon(
                        Icons.Default.FlipCameraAndroid,
                        contentDescription = "Switch Camera"
                    )
                }
            }

            OutlinedButton(
                onClick = { viewModel.pickPhotoFromGallery() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = !state.isCapturing
            ) {
                Icon(androidx.compose.material.icons.Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("UPLOAD FROM GALLERY", fontWeight = FontWeight.Bold)
            }

            // Captured Photos List
            if (state.capturedPhotos.isNotEmpty()) {
                Text(
                    "Verified Audit Photos (${state.capturedPhotos.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                state.capturedPhotos.forEach { photo ->
                    CapturedPhotoCard(
                        photo = photo,
                        onShare = { viewModel.sharePhoto(photo) },
                        onSave = { viewModel.savePhotoToGallery(photo) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CapturedPhotoCard(photo: VisitPhoto, onShare: () -> Unit = {}, onSave: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val bitmap = remember(photo.originalPath) { GpsCameraPlatform.loadImageBitmap(photo.originalPath) }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = photo.caption,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text("Category: ${photo.category.displayName}", fontWeight = FontWeight.Bold)
            if (photo.caption.isNotBlank()) {
                Text("Observation: ${photo.caption}")
            }
            Text("GPS: ${photo.latitude}, ${photo.longitude} (±${photo.accuracy}m)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!photo.address.isNullOrEmpty()) {
                Text("Address: ${photo.address}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSave) {
                    Text("SAVE TO GALLERY")
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(onClick = onShare) {
                    Icon(androidx.compose.material.icons.Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SHARE")
                }
            }
        }
    }
}

private fun formatCoordinate(value: Double): String {
    val factor = 1000000.0
    val rounded = (kotlin.math.round(value * factor) / factor).toString()
    val parts = rounded.split(".")
    val integerPart = parts[0]
    val fractionalPart = if (parts.size > 1) parts[1].padEnd(6, '0').take(6) else "000000"
    return "$integerPart.$fractionalPart"
}
