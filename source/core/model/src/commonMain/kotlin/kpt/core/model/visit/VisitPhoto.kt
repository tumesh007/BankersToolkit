package kpt.core.model.visit

import kotlinx.serialization.Serializable

@Serializable
data class VisitPhoto(
    val id: String,
    val visitId: String,
    val originalPath: String,
    val renderedPath: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val timestamp: Long = 0L,
    val address: String? = null,
    val category: PhotoCategory = PhotoCategory.OTHER,
    val caption: String = "",
    val displayOrder: Int = 0
)
