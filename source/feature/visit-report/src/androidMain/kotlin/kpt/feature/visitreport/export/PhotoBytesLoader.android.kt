package kpt.feature.visitreport.export

import android.app.Application
import android.net.Uri
import java.io.File
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual object PhotoBytesLoader : KoinComponent {

    private val app: Application by inject()

    actual fun loadPhotoBytes(path: String): ByteArray? {
        if (path.isBlank()) return null
        return try {
            val uri = Uri.parse(path)
            when (uri.scheme?.lowercase()) {
                // content:// (e.g. MediaStore) or file:// references must go
                // through the ContentResolver, NOT the filesystem.
                "content", "file" ->
                    app.applicationContext.contentResolver.openInputStream(uri)
                        ?.use { it.readBytes() }
                // A plain absolute filesystem path (CameraX cache capture).
                else -> File(path).takeIf { it.exists() && it.isFile }?.readBytes()
            }
        } catch (e: Exception) {
            null
        }
    }
}