package kpt.feature.visitreport.export

/**
 * Shared JPEG dimension parser (SOF marker scan) used by the PDF and DOCX
 * exporters to embed raw JPEG bytes without a platform bitmap decoder.
 */
internal fun jpegDimensions(bytes: ByteArray): Pair<Int, Int>? {
    if (bytes.size < 4) return null
    if (bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte()) return null
    var offset = 2
    while (offset + 9 < bytes.size) {
        if (bytes[offset] != 0xFF.toByte()) {
            offset++
            continue
        }
        val marker = bytes[offset + 1].toInt() and 0xFF
        // SOF0-SOF15 except DHT(0xC4), JPG(0xC8), DAC(0xCC)
        if (marker in 0xC0..0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC) {
            val height = ((bytes[offset + 5].toInt() and 0xFF) shl 8) or (bytes[offset + 6].toInt() and 0xFF)
            val width = ((bytes[offset + 7].toInt() and 0xFF) shl 8) or (bytes[offset + 8].toInt() and 0xFF)
            if (width in 2..10000 && height in 2..10000) return width to height
            return null
        }
        val segLen = ((bytes[offset + 2].toInt() and 0xFF) shl 8) or (bytes[offset + 3].toInt() and 0xFF)
        if (segLen < 2) return null
        offset += 2 + segLen
    }
    return null
}

/** Formats a coordinate to 5 decimal places without platform String.format. */
internal fun formatCoord(value: Double): String {
    if (value == 0.0) return "0.00000"
    val neg = value < 0
    val av = if (neg) -value else value
    val scaled = (av * 100_000 + 0.5).toLong()
    val intPart = scaled / 100_000
    val frac = scaled % 100_000
    val sign = if (neg) "-" else ""
    return "$sign$intPart.${frac.toString().padStart(5, '0')}"
}