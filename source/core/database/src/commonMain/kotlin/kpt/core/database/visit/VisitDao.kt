package kpt.core.database.visit

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {
    @Transaction
    @Query("SELECT * FROM banker_visits ORDER BY updatedAt DESC")
    fun getAllVisitsWithPhotos(): Flow<List<VisitWithPhotos>>

    @Transaction
    @Query("SELECT * FROM banker_visits WHERE id = :id")
    suspend fun getVisitWithPhotosById(id: String): VisitWithPhotos?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: VisitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<VisitPhotoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: VisitPhotoEntity)

    @Query("DELETE FROM banker_visit_photos WHERE visitId = :visitId")
    suspend fun deletePhotosForVisit(visitId: String)

    @Query("DELETE FROM banker_visit_photos WHERE id = :photoId")
    suspend fun deletePhotoById(photoId: String)

    @Query("DELETE FROM banker_visits WHERE id = :id")
    suspend fun deleteVisitById(id: String)

    @Transaction
    suspend fun upsertVisitWithPhotos(visit: VisitEntity, photos: List<VisitPhotoEntity>) {
        insertVisit(visit)
        deletePhotosForVisit(visit.id)
        if (photos.isNotEmpty()) {
            insertPhotos(photos)
        }
    }
}
