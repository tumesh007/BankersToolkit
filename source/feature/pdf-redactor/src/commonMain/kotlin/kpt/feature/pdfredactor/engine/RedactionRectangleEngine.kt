package kpt.feature.pdfredactor.engine

data class NormalizedRect(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
)

object RedactionRectangleEngine {
    fun convertToPageCoordinates(rect: NormalizedRect, pageWidth: Float, pageHeight: Float): NormalizedRect {
        return NormalizedRect(
            x1 = rect.x1 * pageWidth,
            y1 = rect.y1 * pageHeight,
            x2 = rect.x2 * pageWidth,
            y2 = rect.y2 * pageHeight
        )
    }
}
