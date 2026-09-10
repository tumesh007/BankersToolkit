package kpt.feature.pdfredactor.engine

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import android.app.Application
import java.io.File
import java.util.zip.InflaterInputStream

actual object PdfRedactionProcessor : KoinComponent {

    private val app: Application by inject()

    actual suspend fun processFile(
        inputUri: String,
        outputDir: String,
        signature: WatermarkSignature,
    ): RedactionResult = withContext(Dispatchers.IO) {
        try {
            val context = app.applicationContext
            val uri = Uri.parse(inputUri)
            val displayName = resolveDisplayName(uri) ?: "document.pdf"

            val inputBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext RedactionResult(
                    outputPath = null, success = false,
                    error = "Cannot read PDF from $inputUri",
                )

            val result = processRawPdf(inputBytes, signature)
            val outName = "redacted_${displayName.removeSuffix(".pdf")}.pdf"

            val publishedUri = publishToDownloads(outName, "application/pdf", result.outputBytes)

            RedactionResult(
                outputPath = publishedUri ?: outName,
                pagesProcessed = result.pagesTotal,
                watermarksRemoved = result.watermarksRemoved,
                success = publishedUri != null,
                error = if (publishedUri == null) "Failed to save output PDF" else null,
            )
        } catch (e: Exception) {
            RedactionResult(
                outputPath = null, success = false,
                error = "Redaction failed: ${e.message}",
            )
        }
    }

    private fun publishToDownloads(
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ): String? {
        val context = app.applicationContext
        val resolver = context.contentResolver

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/BankersToolkit/Redacted",
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

    private fun resolveDisplayName(uri: Uri): String? {
        if (uri.scheme == "content") {
            app.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }
}

private data class PdfProcessResult(
    val outputBytes: ByteArray,
    val pagesTotal: Int,
    val watermarksRemoved: Int,
)

private fun processRawPdf(
    pdfBytes: ByteArray,
    signature: WatermarkSignature,
): PdfProcessResult {
    val content = pdfBytes.decodeToString(throwOnInvalidSequence = false)
    var modified = content
    var watermarksRemoved = 0

    val streamRegex = Regex("""stream\r?\n([\s\S]*?)\r?\nendstream""")
    val matchTargets = signature.generateMatchTargets()
    val datePattern = signature.patternRegex
    val streams = streamRegex.findAll(content).toList()

    for (match in streams.reversed()) {
        val rawStreamBody = match.groupValues[1]

        val headerEnd = match.value.indexOf('\n') + 1
        val bodyStartInPdf = match.range.first + headerEnd
        val bodyEndInPdf = match.range.last - "endstream".length

        val decompressed = tryDecompress(pdfBytes, bodyStartInPdf, bodyEndInPdf)
            ?: rawStreamBody

        var hasWatermark = false
        if (datePattern.containsMatchIn(decompressed)) hasWatermark = true
        if (!hasWatermark) {
            for (target in matchTargets) {
                if (target.isNotBlank() && decompressed.contains(target, ignoreCase = true)) {
                    hasWatermark = true
                    break
                }
            }
        }

        if (hasWatermark) {
            val cleaned = WatermarkRemoverEngine.processStreamContent(decompressed, signature)
            if (cleaned != decompressed) {
                watermarksRemoved++
                val streamHeader = match.value.substringBefore('\n', "stream")
                val replacement = "$streamHeader\n$cleaned\nendstream"
                modified = modified.substring(0, match.range.first) +
                    replacement + modified.substring(match.range.last + 1)
            }
        }
    }

    val pageCount = Regex("""/Type\s*/Page[^s]""").findAll(content).count()

    return PdfProcessResult(
        outputBytes = modified.encodeToByteArray(),
        pagesTotal = pageCount,
        watermarksRemoved = watermarksRemoved,
    )
}

private fun tryDecompress(pdfBytes: ByteArray, start: Int, end: Int): String? {
    if (start >= end || start >= pdfBytes.size) return null
    val actualEnd = minOf(end, pdfBytes.size)
    val compressed = pdfBytes.copyOfRange(start, actualEnd)
    return try {
        InflaterInputStream(compressed.inputStream()).readBytes()
            .decodeToString(throwOnInvalidSequence = false)
    } catch (_: Exception) {
        null
    }
}
