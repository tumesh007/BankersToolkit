package kpt.core.data.visit

import kpt.core.model.visit.Visit
import kpt.core.model.visit.VisitPhoto
import kotlinx.coroutines.flow.Flow

interface VisitRepository {
    fun getAllVisits(): Flow<List<Visit>>
    suspend fun getVisitById(id: String): Visit?
    suspend fun saveVisit(visit: Visit)
    suspend fun deleteVisit(id: String)
    suspend fun addPhotoToVisit(photo: VisitPhoto)
    suspend fun deletePhoto(photoId: String)
}
