package kpt.feature.visitreport.export

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual object PhotoBytesLoader {

    @OptIn(ExperimentalEncodingApi::class)
    actual fun loadPhotoBytes(path: String): ByteArray? {
        if (path.isBlank()) return null
        return try {
            val base64Data = when {
                path.startsWith("data:") && path.contains(",") -> path.substringAfter(",")
                else -> path
            }
            Base64.decode(base64Data)
        } catch (_: Throwable) {
            null
        }
    }
}
