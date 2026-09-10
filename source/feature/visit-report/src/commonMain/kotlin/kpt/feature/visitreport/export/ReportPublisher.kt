package kpt.feature.visitreport.export

expect object ReportPublisher {
    fun publish(fileName: String, mimeType: String, bytes: ByteArray): String?
}
