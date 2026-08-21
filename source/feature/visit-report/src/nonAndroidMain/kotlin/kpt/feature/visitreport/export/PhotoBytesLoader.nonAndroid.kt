package kpt.feature.visitreport.export

actual object PhotoBytesLoader {
    // Non-Android targets (desktop/js/native) have no camera/storage bridge in
    // this app; the exporters fall back to a placeholder without real bytes.
    actual fun loadPhotoBytes(path: String): ByteArray? = null
}