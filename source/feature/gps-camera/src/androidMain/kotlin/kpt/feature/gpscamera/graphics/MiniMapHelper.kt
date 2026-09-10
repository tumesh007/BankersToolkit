package kpt.feature.gpscamera.graphics

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.*

/**
 * Fetches a mini-map thumbnail from OpenStreetMap tile servers (free, no API key).
 * Draws a red location pin marker at the exact coordinate position on the tile.
 */
object MiniMapHelper {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private const val TILE_SIZE = 256
    private const val ZOOM = 16 // Street-level detail

    /**
     * Downloads a 256×256 OSM tile centered on the given coordinates
     * and draws a red pin marker at the exact location.
     *
     * @return Bitmap of the map thumbnail, or null if download fails.
     */
    suspend fun fetchMapThumbnail(
        latitude: Double,
        longitude: Double,
        zoom: Int = ZOOM
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // Convert lat/lng to tile coordinates
            val tileX = lngToTileX(longitude, zoom)
            val tileY = latToTileY(latitude, zoom)

            val tileXInt = tileX.toInt()
            val tileYInt = tileY.toInt()

            // Download the center tile
            val centerTile = downloadTile(tileXInt, tileYInt, zoom) ?: return@withContext null

            // Calculate pixel position of the coordinate within the tile
            val pixelX = ((tileX - tileXInt) * TILE_SIZE).toFloat()
            val pixelY = ((tileY - tileYInt) * TILE_SIZE).toFloat()

            // Draw marker on the tile
            val result = centerTile.copy(Bitmap.Config.ARGB_8888, true)
            centerTile.recycle()

            val canvas = Canvas(result)
            drawLocationMarker(canvas, pixelX, pixelY)

            result
        } catch (e: Exception) {
            null
        }
    }

    private fun downloadTile(x: Int, y: Int, zoom: Int): Bitmap? {
        // Use OSM tile server with proper user-agent (required by OSM policy)
        val url = "https://tile.openstreetmap.org/$zoom/$x/$y.png"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "BankersToolkit-Android/1.0 (Banking Field Verification)")
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            val bytes = response.body?.bytes() ?: return null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun drawLocationMarker(canvas: Canvas, x: Float, y: Float) {
        // Outer circle (red with white border)
        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(x, y, 12f, outerPaint)

        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E53935") // Material Red 600
            style = Paint.Style.FILL
        }
        canvas.drawCircle(x, y, 9f, pinPaint)

        // Inner white dot
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(x, y, 4f, innerPaint)

        // Drop shadow ring
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 0, 0, 0)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawCircle(x, y, 13f, shadowPaint)
    }

    // Mercator projection: longitude to tile X
    private fun lngToTileX(lng: Double, zoom: Int): Double {
        return (lng + 180.0) / 360.0 * (1 shl zoom)
    }

    // Mercator projection: latitude to tile Y
    private fun latToTileY(lat: Double, zoom: Int): Double {
        val latRad = Math.toRadians(lat)
        return (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * (1 shl zoom)
    }
}
