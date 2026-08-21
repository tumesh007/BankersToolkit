package kpt.feature.pdfredactor.ui

import kpt.feature.pdfredactor.engine.NormalizedRect

enum class RedactionMode {
    AUTOMATIC_WATERMARK_REMOVAL,
    EMPLOYEE_ID_ASSISTED,
    MANUAL_RECTANGLE_MASK
}

data class PdfRedactorState(
    val selectedFilePaths: List<String> = emptyList(),
    val employeeId: String = "",
    val mode: RedactionMode = RedactionMode.AUTOMATIC_WATERMARK_REMOVAL,
    val customRectangles: List<NormalizedRect> = emptyList(),
    val isProcessing: Boolean = false,
    val progressPercent: Float = 0f,
    val statusMessage: String? = null,
    val processedFilesCount: Int = 0
)
