package com.tumesh007.pdfredactor

import kotlin.math.max
import kotlin.math.min

data class PdfPageBounds(
    val width: Float,
    val height: Float,
    val rotation: Int = 0 // 0, 90, 180, 270 degrees
)

data class ViewTransform(
    val viewWidth: Float,
    val viewHeight: Float,
    val zoom: Float = 1.0f,
    val panX: Float = 0.0f,
    val panY: Float = 0.0f
)

data class AbsoluteRect(
    val x0: Float,
    val y0: Float,
    val x1: Float,
    val y1: Float
) {
    val width: Float get() = x1 - x0
    val height: Float get() = y1 - y0
}

object CoordinateTransformer {

    fun normalizedToPdfRect(
        norm: NormalizedRect,
        pageBounds: PdfPageBounds,
        marginFraction: Float = 0.0f
    ): AbsoluteRect {
        val marginX = marginFraction * pageBounds.width
        val marginY = marginFraction * pageBounds.height

        val x0 = max(0.0f, (norm.x * pageBounds.width) - marginX)
        val y0 = max(0.0f, (norm.y * pageBounds.height) - marginY)
        val x1 = min(pageBounds.width, ((norm.x + norm.width) * pageBounds.width) + marginX)
        val y1 = min(pageBounds.height, ((norm.y + norm.height) * pageBounds.height) + marginY)

        val pdfY0 = pageBounds.height - y1
        val pdfY1 = pageBounds.height - y0

        return AbsoluteRect(x0 = x0, y0 = pdfY0, x1 = x1, y1 = pdfY1)
    }

    fun normalizedToViewRect(
        norm: NormalizedRect,
        pageBounds: PdfPageBounds,
        view: ViewTransform
    ): AbsoluteRect {
        val scaleX = (view.viewWidth / pageBounds.width) * view.zoom
        val scaleY = (view.viewHeight / pageBounds.height) * view.zoom

        val vx0 = (norm.x * pageBounds.width * scaleX) + view.panX
        val vy0 = (norm.y * pageBounds.height * scaleY) + view.panY
        val vx1 = ((norm.x + norm.width) * pageBounds.width * scaleX) + view.panX
        val vy1 = ((norm.y + norm.height) * pageBounds.height * scaleY) + view.panY

        return AbsoluteRect(
            x0 = max(0.0f, min(view.viewWidth, vx0)),
            y0 = max(0.0f, min(view.viewHeight, vy0)),
            x1 = max(0.0f, min(view.viewWidth, vx1)),
            y1 = max(0.0f, min(view.viewHeight, vy1))
        )
    }

    fun viewTouchToNormalizedRect(
        touchX0: Float,
        touchY0: Float,
        touchX1: Float,
        touchY1: Float,
        pageBounds: PdfPageBounds,
        view: ViewTransform
    ): NormalizedRect {
        val scaleX = (view.viewWidth / pageBounds.width) * view.zoom
        val scaleY = (view.viewHeight / pageBounds.height) * view.zoom

        val minVx = (min(touchX0, touchX1) - view.panX) / scaleX
        val maxVx = (max(touchX0, touchX1) - view.panX) / scaleX
        val minVy = (min(touchY0, touchY1) - view.panY) / scaleY
        val maxVy = (max(touchY0, touchY1) - view.panY) / scaleY

        val nx0 = max(0.0f, min(1.0f, minVx / pageBounds.width))
        val ny0 = max(0.0f, min(1.0f, minVy / pageBounds.height))
        val nx1 = max(0.0f, min(1.0f, maxVx / pageBounds.width))
        val ny1 = max(0.0f, min(1.0f, maxVy / pageBounds.height))

        val nw = max(0.001f, nx1 - nx0)
        val nh = max(0.001f, ny1 - ny0)

        return NormalizedRect(x = nx0, y = ny0, width = nw, height = nh)
    }
}
