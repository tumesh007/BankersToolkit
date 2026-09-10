package kpt.core.database.visit

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kpt.core.model.visit.PhotoCategory
import kpt.core.model.visit.VisitPhoto

@Entity(
    tableName = "banker_visit_photos",
    foreignKeys = [
        ForeignKey(
            entity = VisitEntity::class,
            parentColumns = ["id"],
            childColumns = ["visitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["visitId"])]
)
data class VisitPhotoEntity(
    @PrimaryKey
    val id: String,
    val visitId: String,
    val originalPath: String,
    val renderedPath: String?,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val address: String?,
    val category: String,
    val caption: String,
    val displayOrder: Int
) {
    fun toDomain(): VisitPhoto {
        return VisitPhoto(
            id = id,
            visitId = visitId,
            originalPath = originalPath,
            renderedPath = renderedPath,
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            timestamp = timestamp,
            address = address,
            category = try { PhotoCategory.valueOf(category) } catch (e: Exception) { PhotoCategory.OTHER },
            caption = caption,
            displayOrder = displayOrder
        )
    }

    companion object {
        fun fromDomain(photo: VisitPhoto): VisitPhotoEntity {
            return VisitPhotoEntity(
                id = photo.id,
                visitId = photo.visitId,
                originalPath = photo.originalPath,
                renderedPath = photo.renderedPath,
                latitude = photo.latitude,
                longitude = photo.longitude,
                accuracy = photo.accuracy,
                timestamp = photo.timestamp,
                address = photo.address,
                category = photo.category.name,
                caption = photo.caption,
                displayOrder = photo.displayOrder
            )
        }
    }
}
