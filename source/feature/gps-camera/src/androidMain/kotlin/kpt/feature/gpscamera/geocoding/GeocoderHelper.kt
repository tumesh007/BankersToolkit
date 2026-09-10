package kpt.feature.gpscamera.geocoding

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

data class FormattedAddress(
    val fullAddress: String,
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val country: String = ""
)

class GeocoderHelper(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun reverseGeocode(latitude: Double, longitude: Double): FormattedAddress =
        withContext(Dispatchers.IO) {
            val nativeResult = getNativeAddress(latitude, longitude)
            if (nativeResult != null && nativeResult.fullAddress.isNotBlank()) {
                return@withContext nativeResult
            }

            // Fallback: OpenStreetMap Nominatim / HTTP Reverse Geocoding
            return@withContext getHttpFallbackAddress(latitude, longitude)
                ?: FormattedAddress(
                    fullAddress = "Coordinates: ${String.format(Locale.US, "%.6f", latitude)}, ${String.format(Locale.US, "%.6f", longitude)}"
                )
        }

    private suspend fun getNativeAddress(latitude: Double, longitude: Double): FormattedAddress? {
        if (!Geocoder.isPresent()) return null

        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            val addr = addresses.firstOrNull()
                            continuation.resume(addr?.toFormattedAddress())
                        }
                        override fun onError(errorMessage: String?) {
                            continuation.resume(null)
                        }
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.toFormattedAddress()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun Address.toFormattedAddress(): FormattedAddress {
        val lines = (0..maxAddressLineIndex).mapNotNull { getAddressLine(it) }
        val full = lines.joinToString(", ")
        val calculatedFull = if (full.isNotBlank()) {
            full
        } else {
            listOfNotNull(
                thoroughfare ?: subLocality,
                locality ?: subAdminArea,
                adminArea,
                postalCode,
                countryName
            ).joinToString(", ")
        }

        return FormattedAddress(
            fullAddress = calculatedFull,
            street = thoroughfare ?: subLocality ?: "",
            city = locality ?: subAdminArea ?: "",
            state = adminArea ?: "",
            postalCode = postalCode ?: "",
            country = countryName ?: ""
        )
    }

    private fun getHttpFallbackAddress(latitude: Double, longitude: Double): FormattedAddress? {
        return try {
            val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude&zoom=18&addressdetails=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "BankersToolkit-Android/1.0 (Banking Site Verification)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val displayName = json.optString("display_name")
            val addressObj = json.optJSONObject("address")

            val street = addressObj?.optString("road", addressObj.optString("suburb", "")) ?: ""
            val city = addressObj?.optString("city", addressObj.optString("town", addressObj.optString("county", ""))) ?: ""
            val state = addressObj?.optString("state", "") ?: ""
            val postalCode = addressObj?.optString("postcode", "") ?: ""
            val country = addressObj?.optString("country", "") ?: ""

            FormattedAddress(
                fullAddress = displayName,
                street = street,
                city = city,
                state = state,
                postalCode = postalCode,
                country = country
            )
        } catch (e: Exception) {
            null
        }
    }
}
