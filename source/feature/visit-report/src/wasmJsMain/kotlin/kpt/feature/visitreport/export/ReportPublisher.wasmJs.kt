package kpt.feature.visitreport.export

import org.khronos.webgl.Int8Array
import org.khronos.webgl.set
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import kotlinx.browser.document

actual object ReportPublisher {

    actual fun publish(fileName: String, mimeType: String, bytes: ByteArray): String? {
        return try {
            val int8Array = Int8Array(bytes.size)
            for (i in bytes.indices) {
                int8Array[i] = bytes[i]
            }

            val blobProps = BlobPropertyBag(type = mimeType)
            val parts = js("[]")
            parts.push(int8Array)
            val blob = Blob(parts, blobProps)

            val url = URL.createObjectURL(blob)
            val anchor = document.createElement("a") as HTMLAnchorElement
            anchor.href = url
            anchor.download = fileName
            document.body?.appendChild(anchor)
            anchor.click()
            document.body?.removeChild(anchor)
            URL.revokeObjectURL(url)

            fileName
        } catch (e: Throwable) {
            println("ReportPublisher export error: ${e.message}")
            null
        }
    }
}
