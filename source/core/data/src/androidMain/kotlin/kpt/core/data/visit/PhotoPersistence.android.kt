package kpt.core.data.visit

import android.app.Application
import android.content.Context
import kpt.core.model.visit.VisitPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PhotoPersistenceHolder {
    @Volatile
    var application: Application? = null

    fun init(app: Application) {
        application = app
    }
}

actual suspend fun persistPhotoToStorage(
    photo: VisitPhoto,
): VisitPhoto {
    val app = PhotoPersistenceHolder.application ?: return photo

    return withContext(Dispatchers.IO) {
        val originalPath = photo.originalPath
        if (originalPath.isBlank()) return@withContext photo

        val sourceFile = File(originalPath)
        if (!sourceFile.exists() || !sourceFile.isFile) return@withContext photo

        val persistentDir = File(app.filesDir, "visit_photos").apply { mkdirs() }
        val cacheDir = app.cacheDir

        if (sourceFile.absolutePath.startsWith(cacheDir.absolutePath)) {
            // File is in cache dir - copy to persistent storage
            val destFile = File(persistentDir, sourceFile.name)
            var counter = 1
            var finalDest = destFile
            while (finalDest.exists()) {
                val nameWithoutExt = sourceFile.nameWithoutExtension
                val ext = sourceFile.extension
                finalDest = File(persistentDir, "${nameWithoutExt}_$counter.$ext")
                counter++
            }
            sourceFile.copyTo(finalDest, overwrite = false)
            return@withContext photo.copy(renderedPath = finalDest.absolutePath)
        }

        photo
    }
}