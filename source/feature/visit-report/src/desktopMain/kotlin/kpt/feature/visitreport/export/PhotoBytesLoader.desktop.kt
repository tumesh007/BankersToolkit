package kpt.feature.visitreport.export

import java.io.File

actual object PhotoBytesLoader {

    actual fun loadPhotoBytes(path: String): ByteArray? {
        if (path.isBlank()) return null
        return try {
            val file = File(path)
            if (file.exists() && file.isFile) {
                file.readBytes()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
