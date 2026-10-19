package kpt.feature.gpscamera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.Font
import java.awt.Frame
import java.awt.RenderingHints
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

actual object GpsCameraPlatform {

    @Composable
    actual fun CameraPreview(
        modifier: Modifier,
        isFrontCamera: Boolean,
        isTorchOn: Boolean,
        onError: (String) -> Unit
    ) {
        Box(
            modifier = modifier.background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Desktop Mode",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Click 'UPLOAD FROM GALLERY' below to select photos",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            }
        }
    }

    actual fun hasCameraPermission(): Boolean = false

    actual fun hasLocationPermission(): Boolean = true

    actual suspend fun requestCameraPermission(): Boolean = false

    actual suspend fun requestLocationPermission(): Boolean = true

    actual fun openAppSettings() = Unit

    actual suspend fun capturePhoto(
        caseNumber: String,
        officerName: String,
        officerId: String,
        category: String,
        locationData: LocationData
    ): String? = null

    actual suspend fun fetchLocation(): LocationData = LocationData(
        latitude = 0.0,
        longitude = 0.0,
        accuracy = 0f,
        address = "Desktop",
        timestamp = System.currentTimeMillis(),
        isMock = false
    )

    actual suspend fun reverseGeocode(latitude: Double, longitude: Double): String? = null

    actual suspend fun pickPhotoFromGallery(): String? = withContext(Dispatchers.IO) {
        // 1. Try zenity first (native Linux GTK/GNOME dialog, instant and supports Wayland)
        try {
            val zenityPaths = listOf("/usr/bin/zenity", "/bin/zenity", "zenity")
            val zenityBin = zenityPaths.firstOrNull { File(it).exists() } ?: "zenity"
            val process = ProcessBuilder(
                zenityBin,
                "--file-selection",
                "--title=Select Photo from Gallery",
                "--file-filter=Image files (jpg, jpeg, png, webp, bmp) | *.jpg *.jpeg *.png *.webp *.bmp *.JPG *.JPEG *.PNG *.WEBP *.BMP",
                "--file-filter=All files | *"
            ).start()

            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            if (exitCode == 0 && output.isNotBlank()) {
                val selected = File(output)
                if (selected.exists()) {
                    return@withContext copyToAppStorage(selected)
                }
            }
            if (exitCode == 1) {
                // User explicitly cancelled the selection
                return@withContext null
            }
        } catch (_: Exception) {
            // Fall through to AWT / Swing
        }

        // 2. Fallback to native AWT FileDialog
        try {
            val result = CompletableDeferred<String?>()
            EventQueue.invokeLater {
                try {
                    val dialog = FileDialog(null as Frame?, "Select Photo from Gallery", FileDialog.LOAD)
                    dialog.setFilenameFilter { _, name ->
                        val lower = name.lowercase()
                        lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
                            lower.endsWith(".webp") || lower.endsWith(".bmp")
                    }
                    dialog.isVisible = true
                    val file = dialog.file
                    val dir = dialog.directory
                    if (file != null && dir != null) {
                        val selected = File(dir, file)
                        result.complete(copyToAppStorage(selected))
                    } else {
                        result.complete(null)
                    }
                } catch (_: Exception) {
                    result.complete(null)
                }
            }
            val path = result.await()
            if (path != null) return@withContext path
        } catch (_: Exception) {
            // Fall through to Swing
        }

        // 3. Fallback to Swing JFileChooser
        try {
            val result = CompletableDeferred<String?>()
            SwingUtilities.invokeLater {
                try {
                    val chooser = JFileChooser().apply {
                        dialogTitle = "Select Photo from Gallery"
                        fileFilter = FileNameExtensionFilter(
                            "Image files (*.jpg, *.jpeg, *.png, *.webp, *.bmp)",
                            "jpg", "jpeg", "png", "webp", "bmp"
                        )
                    }
                    val res = chooser.showOpenDialog(null)
                    if (res == JFileChooser.APPROVE_OPTION && chooser.selectedFile != null) {
                        result.complete(copyToAppStorage(chooser.selectedFile))
                    } else {
                        result.complete(null)
                    }
                } catch (_: Exception) {
                    result.complete(null)
                }
            }
            return@withContext result.await()
        } catch (_: Exception) {
            null
        }
    }

    private fun copyToAppStorage(source: File): String {
        val cacheDir = File(System.getProperty("user.home"), ".bankerstoolkit/gallery_images").apply { mkdirs() }
        val ext = source.extension.ifBlank { "jpg" }
        val dest = File(cacheDir, "gallery_${System.currentTimeMillis()}_${source.nameWithoutExtension}.$ext")
        source.copyTo(dest, overwrite = true)
        return dest.absolutePath
    }

    actual fun loadImageBitmap(path: String): ImageBitmap? {
        return try {
            val file = File(path)
            if (!file.exists()) return null
            val bytes = file.readBytes()
            org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (_: Exception) {
            try {
                val img = ImageIO.read(File(path)) ?: return null
                img.toComposeImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    actual suspend fun extractPhotoMetadata(path: String): PhotoMetadata? = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext null
            PhotoMetadata(
                timestamp = file.lastModified(),
                latitude = null,
                longitude = null
            )
        } catch (_: Exception) {
            null
        }
    }

    actual suspend fun stampPhoto(
        originalPath: String,
        caseNumber: String,
        officerName: String,
        officerId: String,
        category: String,
        locationData: LocationData
    ): String = withContext(Dispatchers.IO) {
        val srcFile = File(originalPath)
        if (!srcFile.exists()) return@withContext originalPath

        try {
            val img = ImageIO.read(srcFile) ?: return@withContext originalPath
            val width = img.width
            val height = img.height

            val g2d = img.createGraphics()
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            val scale = (width / 1080.0f).coerceIn(1.0f, 4.0f)
            val bannerPadding = (24 * scale).toInt()
            val titleFontSize = (22 * scale).toInt()
            val bodyFontSize = (16 * scale).toInt()
            val lineHeight = (22 * scale).toInt()

            val titleFont = Font(Font.SANS_SERIF, Font.BOLD, titleFontSize)
            val bodyFont = Font(Font.SANS_SERIF, Font.PLAIN, bodyFontSize)

            val lineCount = 5
            val bannerHeight = bannerPadding * 2 + titleFontSize + (lineCount * lineHeight)
            val bannerY = height - bannerHeight

            // Draw dark semi-transparent banner
            g2d.color = java.awt.Color(0, 0, 0, 190)
            g2d.fillRect(0, bannerY, width, bannerHeight)

            // Accent stripe
            g2d.color = java.awt.Color(0, 168, 150)
            g2d.fillRect(0, bannerY, width, (3 * scale).toInt().coerceAtLeast(2))

            var currentY = bannerY + bannerPadding + titleFontSize
            g2d.color = java.awt.Color.WHITE
            g2d.font = titleFont
            g2d.drawString("Banker's Toolkit - Field & Asset Verification", bannerPadding, currentY)

            g2d.font = bodyFont
            currentY += lineHeight
            g2d.color = java.awt.Color(220, 220, 220)

            val casePart = if (caseNumber.isNotBlank()) "Case: $caseNumber" else "General Verification"
            val officerPart = if (officerName.isNotBlank()) " | Officer: $officerName" else ""
            val catPart = if (category.isNotBlank()) " | Category: $category" else ""
            g2d.drawString("$casePart$officerPart$catPart", bannerPadding, currentY)

            currentY += lineHeight
            val latStr = if (locationData.latitude != 0.0) "${locationData.latitude}" else "N/A"
            val lonStr = if (locationData.longitude != 0.0) "${locationData.longitude}" else "N/A"
            val accStr = if (locationData.accuracy > 0) " (±${locationData.accuracy.toInt()}m)" else ""
            g2d.drawString("GPS: $latStr, $lonStr$accStr", bannerPadding, currentY)

            currentY += lineHeight
            val addrStr = locationData.address?.takeIf { it.isNotBlank() } ?: "Address: Desktop Import"
            val displayAddr = if (addrStr.length > 90) addrStr.take(87) + "..." else addrStr
            g2d.drawString(displayAddr, bannerPadding, currentY)

            currentY += lineHeight
            val timeStr = Instant.ofEpochMilli(locationData.timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm:ss a"))
            g2d.drawString("Timestamp: $timeStr", bannerPadding, currentY)

            g2d.dispose()

            val stampedDir = File(System.getProperty("user.home"), ".bankerstoolkit/gps_camera").apply { mkdirs() }
            val stampedFile = File(stampedDir, "stamped_${System.currentTimeMillis()}_${srcFile.nameWithoutExtension}.jpg")
            ImageIO.write(img, "jpg", stampedFile)
            stampedFile.absolutePath
        } catch (_: Exception) {
            originalPath
        }
    }

    actual fun sharePhoto(path: String) {
        try {
            val file = File(path)
            if (!file.exists()) return
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file)
            } else {
                ProcessBuilder("xdg-open", file.absolutePath).start()
            }
        } catch (_: Exception) {
            try {
                ProcessBuilder("xdg-open", path).start()
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    actual suspend fun savePhotoToGallery(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(path)
            if (!sourceFile.exists()) return@withContext false

            val picturesDir = File(System.getProperty("user.home"), "Pictures/BankersToolkit").apply { mkdirs() }
            val targetFile = File(picturesDir, sourceFile.name)
            sourceFile.copyTo(targetFile, overwrite = true)
            true
        } catch (_: Exception) {
            false
        }
    }
}
