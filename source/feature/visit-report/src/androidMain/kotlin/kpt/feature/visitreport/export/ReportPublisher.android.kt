package kpt.feature.visitreport.export

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.app.Application
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual object ReportPublisher : KoinComponent {

    private val app: Application by inject()

    actual fun publish(fileName: String, mimeType: String, bytes: ByteArray): String? {
        val context = app.applicationContext
        val resolver = context.contentResolver

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/BankersToolkit/VisitReports",
                )
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val uri = resolver.insert(collection, values) ?: return null
        resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return null
        return uri.toString()
    }
}
