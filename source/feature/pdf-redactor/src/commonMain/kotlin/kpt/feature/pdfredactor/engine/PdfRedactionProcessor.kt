package kpt.feature.pdfredactor.engine

data class RedactionResult(
    val outputPath: String?,
    val pagesProcessed: Int = 0,
    val watermarksRemoved: Int = 0,
    val success: Boolean = false,
    val error: String? = null,
)

expect object PdfRedactionProcessor {
    suspend fun processFile(
        inputUri: String,
        outputDir: String,
        signature: WatermarkSignature,
    ): RedactionResult
}
