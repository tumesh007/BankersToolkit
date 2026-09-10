package kpt.feature.gpscamera.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kpt.core.data.visit.VisitRepository
import kpt.core.model.visit.PhotoCategory
import kpt.core.model.visit.VisitPhoto
import kpt.feature.gpscamera.GpsCameraPlatform
import kpt.feature.gpscamera.LocationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock

class GpsCameraViewModel(
    private val visitRepository: VisitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GpsCameraState())
    val uiState: StateFlow<GpsCameraState> = _uiState.asStateFlow()

    init {
        refreshPermissions(requestMissing = true)
    }

    fun setVisitId(visitId: String?) {
        _uiState.update { it.copy(visitId = visitId) }
        if (!visitId.isNullOrBlank()) {
            viewModelScope.launch {
                try {
                    val visit = visitRepository.getVisitById(visitId)
                    if (visit != null) {
                        _uiState.update {
                            it.copy(
                                caseNumber = visit.loanAccountNumber.ifBlank { visit.accountNumber }.ifBlank { visit.id },
                                officerName = visit.officerName,
                                officerId = visit.branch
                            )
                        }
                    }
                } catch (e: Exception) {
                    // non-critical
                }
            }
        }
    }

    fun setCaseAndOfficer(caseNumber: String, officerName: String, officerId: String) {
        _uiState.update {
            it.copy(
                caseNumber = caseNumber,
                officerName = officerName,
                officerId = officerId
            )
        }
    }

    private fun refreshPermissions(requestMissing: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingPermissions = true) }
            val cameraGranted = GpsCameraPlatform.hasCameraPermission()
            val locationGranted = GpsCameraPlatform.hasLocationPermission()
            _uiState.update {
                it.copy(
                    isCheckingPermissions = false,
                    cameraGranted = cameraGranted,
                    locationGranted = locationGranted,
                )
            }
            if (!cameraGranted && requestMissing) {
                requestCameraPermission()
            } else if (cameraGranted) {
                if (!locationGranted && requestMissing) requestLocationPermission()
                if (locationGranted) refreshLocation()
            }
        }
    }

    fun requestCameraPermission() {
        viewModelScope.launch {
            val granted = GpsCameraPlatform.requestCameraPermission()
            _uiState.update { it.copy(cameraGranted = granted) }
            if (granted) {
                _uiState.update { it.copy(statusMessage = "Camera access granted") }
                if (!_uiState.value.locationGranted) requestLocationPermission()
            }
        }
    }

    fun requestLocationPermission() {
        viewModelScope.launch {
            val granted = GpsCameraPlatform.requestLocationPermission()
            _uiState.update { it.copy(locationGranted = granted) }
            if (granted) {
                _uiState.update { it.copy(statusMessage = "Location access granted") }
                refreshLocation()
            }
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            if (!_uiState.value.locationGranted) return@launch
            _uiState.update { it.copy(isFetchingLocation = true) }
            val location = GpsCameraPlatform.fetchLocation()
            _uiState.update { it.copy(location = location, isFetchingLocation = false) }
        }
    }

    fun toggleCamera() {
        _uiState.update { it.copy(isFrontCamera = !it.isFrontCamera) }
    }

    fun switchCamera(useFront: Boolean) {
        _uiState.update { it.copy(isFrontCamera = useFront) }
    }

    fun toggleTorch() {
        _uiState.update { it.copy(isTorchOn = !it.isTorchOn) }
    }

    fun toggleGrid() {
        _uiState.update { it.copy(showGrid = !it.showGrid) }
    }

    fun updateCategory(category: PhotoCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun updateCaption(caption: String) {
        _uiState.update { it.copy(caption = caption) }
    }

    fun capturePhoto() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.location.isMock) {
                _uiState.update { it.copy(errorMessage = "Capture blocked: Mock/Spoofed GPS location detected.") }
                return@launch
            }

            _uiState.update { it.copy(isCapturing = true, statusMessage = null, errorMessage = null) }

            val currentLocation = if (state.locationGranted) GpsCameraPlatform.fetchLocation() else state.location

            val path = try {
                GpsCameraPlatform.capturePhoto(
                    caseNumber = state.caseNumber.ifBlank { state.visitId ?: "AUDIT-${Random.nextInt(1000, 9999)}" },
                    officerName = state.officerName,
                    officerId = state.officerId,
                    category = state.selectedCategory.displayName,
                    locationData = currentLocation
                )
            } catch (e: Exception) {
                null
            }

            if (path == null) {
                _uiState.update { it.copy(isCapturing = false, statusMessage = "Photo capture cancelled") }
                return@launch
            }

            val now = Clock.System.now().toEpochMilliseconds()
            val photoId = "photo_${now}_${Random.nextInt(1000, 9999)}"
            val photo = VisitPhoto(
                id = photoId,
                visitId = state.visitId ?: "standalone",
                originalPath = path,
                renderedPath = path,
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude,
                accuracy = currentLocation.accuracy,
                timestamp = now,
                address = currentLocation.address,
                category = state.selectedCategory,
                caption = state.caption,
                displayOrder = state.capturedPhotos.size
            )

            state.visitId?.let { visitId ->
                try {
                    visitRepository.addPhotoToVisit(photo)
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(errorMessage = "Photo saved locally but could not be attached to the visit: ${e.message}")
                    }
                }
            }

            _uiState.update {
                it.copy(
                    capturedPhotos = it.capturedPhotos + photo,
                    lastCapturedPhoto = photo,
                    isCapturing = false,
                    statusMessage = "Verified GPS photo captured & stamped",
                    caption = ""
                )
            }
        }
    }

    fun pickPhotoFromGallery() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCapturing = true, statusMessage = null, errorMessage = null) }
            val state = _uiState.value
            
            val path = try {
                GpsCameraPlatform.pickPhotoFromGallery()
            } catch (e: Exception) {
                null
            }

            if (path == null) {
                _uiState.update { it.copy(isCapturing = false, statusMessage = "Gallery selection cancelled") }
                return@launch
            }
            
            // Extract original metadata from the image (v2.1 Feature)
            val metadata = GpsCameraPlatform.extractPhotoMetadata(path)
            
            // Fallback to current location/time if metadata is missing (e.g. stripped by WhatsApp)
            val currentLocation = if (state.locationGranted) GpsCameraPlatform.fetchLocation() else state.location
            val now = Clock.System.now().toEpochMilliseconds()
            
            val lat = metadata?.latitude ?: currentLocation.latitude
            val lon = metadata?.longitude ?: currentLocation.longitude
            
            // Use metadata address if available, else fallback
            val address = if (metadata?.latitude != null && metadata.longitude != null) {
                GpsCameraPlatform.reverseGeocode(metadata.latitude, metadata.longitude) ?: currentLocation.address
            } else {
                currentLocation.address
            }
            
            val photoId = "photo_gallery_${now}_${Random.nextInt(1000, 9999)}"
            
            // Stamp the image with the extracted/fallback data
            val stampedPath = GpsCameraPlatform.stampPhoto(
                originalPath = path,
                caseNumber = state.visitId ?: "Gallery Import",
                officerName = "Imported", // or fetched from session
                officerId = "",
                category = state.selectedCategory.displayName,
                locationData = LocationData(
                    latitude = lat,
                    longitude = lon,
                    accuracy = currentLocation.accuracy,
                    address = address,
                    timestamp = metadata?.timestamp ?: now,
                    isMock = false
                )
            )
            
            val photo = VisitPhoto(
                id = photoId,
                visitId = state.visitId ?: "standalone",
                originalPath = path,
                renderedPath = stampedPath,
                latitude = lat,
                longitude = lon,
                accuracy = currentLocation.accuracy, // Accuracy not reliably stored in standard EXIF
                timestamp = metadata?.timestamp ?: now,
                address = address,
                category = state.selectedCategory,
                caption = state.caption,
                displayOrder = state.capturedPhotos.size
            )

            state.visitId?.let { visitId ->
                try {
                    visitRepository.addPhotoToVisit(photo)
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(errorMessage = "Photo saved locally but could not be attached to the visit: ${e.message}")
                    }
                }
            }

            _uiState.update {
                it.copy(
                    capturedPhotos = it.capturedPhotos + photo,
                    lastCapturedPhoto = photo,
                    isCapturing = false,
                    statusMessage = "Photo selected from gallery",
                    caption = ""
                )
            }
        }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun openAppSettings() = GpsCameraPlatform.openAppSettings()

    fun sharePhoto(photo: VisitPhoto) {
        GpsCameraPlatform.sharePhoto(photo.renderedPath ?: photo.originalPath)
    }

    fun savePhotoToGallery(photo: VisitPhoto) {
        viewModelScope.launch {
            val success = GpsCameraPlatform.savePhotoToGallery(photo.renderedPath ?: photo.originalPath)
            if (success) {
                _uiState.update { it.copy(statusMessage = "Saved to Gallery / Pictures") }
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to save photo to Gallery") }
            }
        }
    }
}
