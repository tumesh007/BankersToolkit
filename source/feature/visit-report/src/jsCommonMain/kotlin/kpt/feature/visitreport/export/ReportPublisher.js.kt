package kpt.feature.visitreport.export

actual object ReportPublisher {
    actual fun publish(fileName: String, mimeType: String, bytes: ByteArray): String? {
        return null
    }
}
