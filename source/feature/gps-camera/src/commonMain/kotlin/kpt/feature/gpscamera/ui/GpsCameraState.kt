package kpt.feature.gpscamera.ui

import kpt.core.model.visit.PhotoCategory
import kpt.core.model.visit.VisitPhoto
import kpt.feature.gpscamera.LocationData

data class GpsCameraState(
    val visitId: String? = null,
    val caseNumber: String = "",
    val officerName: String = "",
    val officerId: String = "",
    val location: LocationData = LocationData(),
    val isFetchingLocation: Boolean = false,
    val cameraGranted: Boolean = false,
    val locationGranted: Boolean = false,
    val isCheckingPermissions: Boolean = true,
    val isCapturing: Boolean = false,
    val isFrontCamera: Boolean = false,
    val isTorchOn: Boolean = false,
    val showGrid: Boolean = true,
    val selectedCategory: PhotoCategory = PhotoCategory.PROPERTY,
    val caption: String = "",
    val capturedPhotos: List<VisitPhoto> = emptyList(),
    val lastCapturedPhoto: VisitPhoto? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)
