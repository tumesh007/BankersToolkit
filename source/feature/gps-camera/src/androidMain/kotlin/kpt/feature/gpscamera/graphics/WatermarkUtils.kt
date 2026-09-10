package kpt.feature.gpscamera.graphics

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class WatermarkMetadata(
    val organizationName: String = "Banker's Toolkit - Asset & Field Verification",
    val officerName: String = "",
    val officerId: String = "",
    val caseNumber: String = "",
    val category: String = "",
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val address: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isMockLocation: Boolean = false,
    val mapThumbnail: Bitmap? = null
)

object WatermarkUtils {

    suspend fun stampAndSave(
        sourceFile: File,
        metadata: WatermarkMetadata
    ): File = withContext(Dispatchers.Default) {
        // 1. Read EXIF orientation before decoding
        val orientation = try {
            val exif = ExifInterface(sourceFile.absolutePath)
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val original = BitmapFactory.decodeFile(sourceFile.absolutePath)
            ?: throw IllegalStateException("Could not decode image at ${sourceFile.absolutePath}")

        // 2. Rotate bitmap if needed according to EXIF
        val orientedBitmap = rotateBitmapIfRequired(original, orientation)

        // 3. Render watermark canvas overlay
        val stamped = addVerificationWatermark(orientedBitmap, metadata)
        if (orientedBitmap != original) {
            orientedBitmap.recycle()
        }
        original.recycle()

        // 4. Overwrite source file with stamped JPEG
        FileOutputStream(sourceFile).use { out ->
            stamped.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        stamped.recycle()
        sourceFile
    }

    private fun rotateBitmapIfRequired(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun addVerificationWatermark(source: Bitmap, meta: WatermarkMetadata): Bitmap {
        val config = source.config ?: Bitmap.Config.ARGB_8888
        val result = source.copy(config, true)
        val canvas = Canvas(result)

        val width = result.width.toFloat()
        val height = result.height.toFloat()

        // Responsive typography scale based on photo width (normalized to 1080px base)
        val scale = (width / 1080f).coerceIn(1.0f, 4.0f)

        val bannerPadding = 28f * scale
        val textSizeTitle = 22f * scale
        val textSizeBody = 17f * scale
        val textSizeSmall = 13f * scale

        val textPaintTitle = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD700") // Gold
            textSize = textSizeTitle
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val textPaintBody = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = textSizeBody
            typeface = Typeface.DEFAULT
        }

        val textPaintWarn = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF4444")
            textSize = textSizeTitle
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val textPaintSmall = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CCCCCC")
            textSize = textSizeSmall
            typeface = Typeface.DEFAULT
        }

        // Format Date & Time
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        val formattedTime = sdf.format(Date(meta.timestamp))

        // Text lines preparation
        val titleText = meta.organizationName.uppercase()
        val detailsLine = buildString {
            if (meta.category.isNotBlank()) append("Category: ${meta.category}")
            if (meta.caseNumber.isNotBlank()) {
                if (isNotEmpty()) append(" | ")
                append("Case/Visit: ${meta.caseNumber}")
            }
            if (meta.officerName.isNotBlank() || meta.officerId.isNotBlank()) {
                if (isNotEmpty()) append(" | ")
                append("Officer: ")
                if (meta.officerName.isNotBlank()) append(meta.officerName)
                if (meta.officerId.isNotBlank()) append(" (${meta.officerId})")
            }
        }
        val gpsText = "GPS: ${String.format(Locale.US, "%.6f", meta.latitude)}, ${String.format(Locale.US, "%.6f", meta.longitude)} (±${meta.accuracyMeters.toInt()}m accuracy)"
        val addressText = "Location: ${meta.address.ifBlank { "Physical address lookup unavailable" }}"
        val timeText = "Timestamp: $formattedTime"

        // Calculate available text width (reserve space for map thumbnail if present)
        val mapThumbSize = if (meta.mapThumbnail != null) 180f * scale else 0f
        val textWidth = (width - (bannerPadding * 2) - if (mapThumbSize > 0) mapThumbSize + bannerPadding else 0f).toInt().coerceAtLeast(100)

        // StaticLayouts for multiline wrapping
        val addressLayout = StaticLayout.Builder.obtain(addressText, 0, addressText.length, textPaintBody, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(4f * scale, 1.0f)
            .build()

        val contentHeight = (textSizeTitle * 1.4f) +
                (if (detailsLine.isNotBlank()) textSizeBody * 1.35f else 0f) +
                (textSizeBody * 1.35f) +
                addressLayout.height +
                (textSizeSmall * 1.35f) +
                if (meta.isMockLocation) (textSizeTitle * 1.4f) else 0f

        val bannerHeight = (contentHeight + (bannerPadding * 2)).coerceAtLeast(if (mapThumbSize > 0) mapThumbSize + (bannerPadding * 2) else 0f)
        val bannerTop = height - bannerHeight

        // 1. Draw Semi-Transparent Dark Banner
        val bannerPaint = Paint().apply {
            color = Color.argb(195, 15, 23, 42) // Slate 900 with 76% Alpha
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, bannerTop, width, height, bannerPaint)

        // Accent top bar on the banner
        val accentBarPaint = Paint().apply {
            color = if (meta.isMockLocation) Color.RED else Color.parseColor("#0284C7")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, bannerTop, width, bannerTop + (4f * scale), accentBarPaint)

        // 2. Draw Text Lines
        var currentY = bannerTop + bannerPadding + (textSizeTitle * 0.8f)
        val startX = bannerPadding

        if (meta.isMockLocation) {
            canvas.drawText("⚠️ WARNING: MOCK / SPOOFED LOCATION DETECTED", startX, currentY, textPaintWarn)
            currentY += textSizeTitle * 1.3f
        }

        canvas.drawText(titleText, startX, currentY, textPaintTitle)
        currentY += textSizeBody * 1.35f

        if (detailsLine.isNotBlank()) {
            canvas.drawText(detailsLine, startX, currentY, textPaintBody)
            currentY += textSizeBody * 1.35f
        }

        canvas.drawText(gpsText, startX, currentY, textPaintBody)
        currentY += 8f * scale

        // Draw multiline address
        canvas.save()
        canvas.translate(startX, currentY)
        addressLayout.draw(canvas)
        canvas.restore()
        currentY += addressLayout.height + (4f * scale)

        canvas.drawText(timeText, startX, currentY, textPaintSmall)

        // 3. Draw Optional Mini-Map Thumbnail on Right Side
        meta.mapThumbnail?.let { mapBmp ->
            val mapLeft = width - bannerPadding - mapThumbSize
            val mapTop = bannerTop + bannerPadding
            val destRect = RectF(mapLeft, mapTop, mapLeft + mapThumbSize, mapTop + mapThumbSize)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 2f * scale
            }
            canvas.drawBitmap(mapBmp, null, destRect, null)
            canvas.drawRoundRect(destRect, 8f * scale, 8f * scale, borderPaint)
        }

        return result
    }
}
