package kpt.core.database.visit

import androidx.room3.Embedded
import androidx.room3.Relation

data class VisitWithPhotos(
    @Embedded val visit: VisitEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["visitId"]
    )
    val photos: List<VisitPhotoEntity>
) {
    fun toDomain(): kpt.core.model.visit.Visit {
        val sortedPhotos = photos.sortedBy { it.displayOrder }.map { it.toDomain() }
        return visit.toDomain(sortedPhotos)
    }
}
