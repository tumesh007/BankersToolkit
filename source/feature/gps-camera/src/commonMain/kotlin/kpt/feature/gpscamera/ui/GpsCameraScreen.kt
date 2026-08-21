package kpt.feature.gpscamera.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kpt.core.model.visit.VisitPhoto
import kpt.feature.gpscamera.GpsCameraPlatform
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

    // Keep a fresh fix while this destination is visible. Camera preview is never
    // blocked by this best-effort work.
    LaunchedEffect(state.locationGranted) {
        while (state.locationGranted) {
            viewModel.refreshLocation()
            kotlinx.coroutines.delay(15_000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.visitId != null) "Visit GPS Camera" else "Standalone GPS Camera",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Camera Viewfinder Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
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
                                    onError = { /* capture control surfaces platform errors below */ },
                                )
                            }
                        }
                    }

                    // Watermark / GPS Overlay Box
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(8.dp)
                    ) {
                        Column {
                            when {
                                state.isFetchingLocation -> {
                                    Text(
                                        "Locating GPS...",
                                        color = Color.Green,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                !state.location.isValid -> {
                                    Text(
                                        "GPS unavailable — enable location & refresh",
                                        color = Color.Yellow,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                else -> {
                                    Text(
                                        "GPS: ${state.location.latitude.toString().take(7)}, ${state.location.longitude.toString().take(7)} (±${state.location.accuracy}m)",
                                        color = Color.Green,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (!state.location.address.isNullOrEmpty()) {
                                Text(
                                    "Location: ${state.location.address}",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                "Category: ${state.selectedCategory.displayName}",
                                color = Color.Yellow,
                                fontSize = 11.sp
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
                            "Location permission is needed to stamp GPS coordinates on photos",
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
                    Text(if (state.isFetchingLocation) "Fetching GPS fix..." else "REFRESH GPS LOCATION")
                }
            }

            // Photo Category Selection
            Text(
                "Photo Category",
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
                label = { Text("Photo Caption / Notes") },
                placeholder = { Text("e.g. Front view of stock godown") },
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

            // Capture Action Button
            Button(
                onClick = { viewModel.capturePhoto() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = state.cameraGranted && !state.isCapturing && !state.isCheckingPermissions
            ) {
                if (state.isCapturing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CAPTURING...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CAPTURE GPS PHOTO", fontWeight = FontWeight.Bold)
                }
            }

            // Captured Photos List
            if (state.capturedPhotos.isNotEmpty()) {
                Text(
                    "Captured Photos (${state.capturedPhotos.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                state.capturedPhotos.forEach { photo ->
                    CapturedPhotoCard(photo)
                }
            }
        }
    }
}

@Composable
private fun CapturedPhotoCard(photo: VisitPhoto) {
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
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text("Category: ${photo.category.displayName}", fontWeight = FontWeight.Bold)
            if (photo.caption.isNotBlank()) {
                Text("Caption: ${photo.caption}")
            }
            Text("GPS: ${photo.latitude}, ${photo.longitude}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Path: ${photo.originalPath}", fontSize = 11.sp, color = Color.Gray)
        }
    }
}
