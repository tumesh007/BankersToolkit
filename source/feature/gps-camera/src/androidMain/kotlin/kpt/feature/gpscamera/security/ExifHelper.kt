package kpt.feature.gpscamera.security

import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object ExifHelper {

    fun writeGpsAndAuditMetadata(
        imageFile: File,
        latitude: Double,
        longitude: Double,
        caseNumber: String,
        officerId: String,
        officerName: String = "",
        address: String = "",
        category: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        val exif = ExifInterface(imageFile.absolutePath)

        // 1. Coordinates in standard DMS format
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, decimalToDms(latitude))
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (latitude >= 0) "N" else "S")
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, decimalToDms(longitude))
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (longitude >= 0) "E" else "W")

        // 2. Date & Time Formats (UTC)
        val exifDateFormatter = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateString = exifDateFormatter.format(Date(timestamp))
        exif.setAttribute(ExifInterface.TAG_DATETIME, dateString)
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, dateString)
        exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, dateString)

        // 3. User comments & Software audit signature
        val userComment = buildString {
            append("CaseID: $caseNumber")
            if (officerId.isNotBlank()) append(" | OfficerID: $officerId")
            if (officerName.isNotBlank()) append(" | Officer: $officerName")
            if (category.isNotBlank()) append(" | Category: $category")
            if (address.isNotBlank()) append(" | Address: $address")
            append(" | TimestampUTC: $timestamp")
        }
        exif.setAttribute(ExifInterface.TAG_USER_COMMENT, userComment)
        exif.setAttribute(ExifInterface.TAG_SOFTWARE, "BankersToolkit Secure GPS Verification Camera")

        exif.saveAttributes()
    }

    private fun decimalToDms(decimal: Double): String {
        val positiveVal = abs(decimal)
        val degrees = positiveVal.toInt()
        val minutes = ((positiveVal - degrees) * 60).toInt()
        val seconds = ((((positiveVal - degrees) * 60) - minutes) * 60 * 1000).toInt()
        return "$degrees/1,$minutes/1,$seconds/1000"
    }

    fun readOriginalMetadata(imagePath: String): kpt.feature.gpscamera.PhotoMetadata {
        return try {
            val exif = ExifInterface(imagePath)
            
            // Try to parse timestamp
            var timestamp: Long? = null
            val dateTimeString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) 
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            
            if (dateTimeString != null) {
                try {
                    val exifDateFormatter = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC") // Assuming EXIF time is UTC for our app, but standard camera EXIF might be local time. We'll use local time for generic parsing.
                    }
                    val genericFormatter = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                    timestamp = genericFormatter.parse(dateTimeString)?.time
                } catch (e: Exception) {
                    // Ignore parsing error
                }
            }

            // Extract lat/long
            val latLong = exif.latLong
            val latitude = latLong?.get(0)
            val longitude = latLong?.get(1)

            kpt.feature.gpscamera.PhotoMetadata(timestamp, latitude, longitude)
        } catch (e: Exception) {
            kpt.feature.gpscamera.PhotoMetadata(null, null, null)
        }
    }
}
