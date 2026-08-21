package kpt.feature.visitreport.export

/**
 * Resolves a stored visit-photo path/URI into raw image bytes so the PDF/DOCX
 * exporters can embed the actual photograph.
 *
 * A captured photo is persisted as an absolute filesystem path
 * (e.g. `<cacheDir>/gps_camera/photo_<ts>.jpg`) by the GPS Camera feature, but
 * content:// (or file://) URIs are handled too. Returns `null` when the path is
 * blank, missing, unreadable, or the platform cannot open it — in that case the
 * exporters render a graceful placeholder instead of crashing.
 */
expect object PhotoBytesLoader {
    fun loadPhotoBytes(path: String): ByteArray?
}