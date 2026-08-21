package kpt.feature.pdfredactor.engine

actual object PdfRedactionProcessor {
    actual suspend fun processFile(
        inputUri: String,
        outputDir: String,
        signature: WatermarkSignature,
    ): RedactionResult = RedactionResult(
        outputPath = null,
        success = false,
        error = "PDF redaction is only supported on Android.",
    )
}
