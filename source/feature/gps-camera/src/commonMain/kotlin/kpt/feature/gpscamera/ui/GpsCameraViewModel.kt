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

    fun updateCategory(category: PhotoCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun updateCaption(caption: String) {
        _uiState.update { it.copy(caption = caption) }
    }

    fun capturePhoto() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCapturing = true, statusMessage = null, errorMessage = null) }

            val path = try {
                GpsCameraPlatform.capturePhoto()
            } catch (e: Exception) {
                null
            }

            if (path == null) {
                _uiState.update { it.copy(isCapturing = false, statusMessage = "Photo capture cancelled") }
                return@launch
            }

            val state = _uiState.value
            val location = if (state.locationGranted) GpsCameraPlatform.fetchLocation() else state.location
            val now = Clock.System.now().toEpochMilliseconds()
            val photoId = "photo_${now}_${Random.nextInt(1000, 9999)}"
            val photo = VisitPhoto(
                id = photoId,
                visitId = state.visitId ?: "standalone",
                originalPath = path,
                renderedPath = path,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                timestamp = now,
                address = location.address,
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
                    statusMessage = "Photo captured with GPS stamp",
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
}
