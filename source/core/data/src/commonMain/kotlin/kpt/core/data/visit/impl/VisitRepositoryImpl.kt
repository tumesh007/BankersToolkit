package kpt.core.data.visit.impl

import kpt.core.data.visit.VisitRepository
import kpt.core.data.visit.persistPhotoToStorage
import kpt.core.database.visit.VisitDao
import kpt.core.database.visit.VisitEntity
import kpt.core.database.visit.VisitPhotoEntity
import kpt.core.model.visit.Visit
import kpt.core.model.visit.VisitPhoto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VisitRepositoryImpl(
    private val visitDao: VisitDao
) : VisitRepository {

    override fun getAllVisits(): Flow<List<Visit>> {
        return visitDao.getAllVisitsWithPhotos().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getVisitById(id: String): Visit? {
        return visitDao.getVisitWithPhotosById(id)?.toDomain()
    }

    override suspend fun saveVisit(visit: Visit) {
        val entity = VisitEntity.fromDomain(visit)
        val photoEntities = visit.photos.mapIndexed { index, photo ->
            VisitPhotoEntity.fromDomain(photo.copy(displayOrder = index))
        }
        visitDao.upsertVisitWithPhotos(entity, photoEntities)
    }

    override suspend fun deleteVisit(id: String) {
        visitDao.deleteVisitById(id)
    }

    override suspend fun addPhotoToVisit(photo: VisitPhoto) {
        val persistedPhoto = persistPhotoToStorage(photo)
        visitDao.insertPhoto(VisitPhotoEntity.fromDomain(persistedPhoto))
    }

    override suspend fun deletePhoto(photoId: String) {
        visitDao.deletePhotoById(photoId)
    }
}
