package com.vennela365.viewer.parser

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExifData(
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val lensModel: String? = null,
    val exposureTime: String? = null,
    val fNumber: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val dateTime: String? = null,
    val dimensions: String? = null,
    val flash: String? = null,
    val whiteBalance: String? = null,
    val software: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val rawTags: Map<String, String> = emptyMap(),
    val hasExif: Boolean = false,
    val error: String? = null
)

object ExifParser {

    suspend fun extractExif(context: Context, uri: Uri?): ExifData = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext ExifData(error = "No image selected")
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()?.takeIf { it.isNotBlank() }
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()?.takeIf { it.isNotBlank() }
                val lens = runCatching { exif.getAttribute("LensModel") }.getOrNull()?.trim()?.takeIf { it.isNotBlank() }

                val exp = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let {
                    val expDouble = it.toDoubleOrNull()
                    if (expDouble != null && expDouble < 1.0 && expDouble > 0.0) {
                        "1/${Math.round(1.0 / expDouble)}s"
                    } else "$it s"
                }

                val fNum = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" }
                val isoVal = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
                    ?: exif.getAttribute("PhotographicSensitivity")
                val iso = isoVal?.let { "ISO $it" }

                val focal = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let {
                    val d = it.toDoubleOrNull()
                    if (d != null) "${d.toInt()} mm" else "$it mm"
                }

                val date = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

                val width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
                val height = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)
                val dim = if (!width.isNullOrBlank() && !height.isNullOrBlank()) "$width × $height" else null

                val flash = exif.getAttribute(ExifInterface.TAG_FLASH)?.let {
                    if (it == "0") "Did not fire" else "Fired"
                }

                val wb = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE)?.let {
                    if (it == "0") "Auto" else "Manual"
                }

                val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)

                val latLong = FloatArray(2)
                val hasGps = exif.getLatLong(latLong)
                val lat = if (hasGps) latLong[0].toDouble() else null
                val lon = if (hasGps) latLong[1].toDouble() else null

                val hasAny = make != null || model != null || exp != null || fNum != null || iso != null || date != null

                val map = mutableMapOf<String, String>()
                if (make != null) map["Make"] = make
                if (model != null) map["Model"] = model
                if (lens != null) map["Lens"] = lens
                if (exp != null) map["Exposure"] = exp
                if (fNum != null) map["Aperture"] = fNum
                if (iso != null) map["ISO"] = iso
                if (focal != null) map["Focal Length"] = focal
                if (dim != null) map["Dimensions"] = dim
                if (date != null) map["Date Taken"] = date
                if (flash != null) map["Flash"] = flash
                if (wb != null) map["White Balance"] = wb
                if (software != null) map["Software"] = software
                if (hasGps) map["Coordinates"] = String.format(java.util.Locale.US, "%.4f, %.4f", lat ?: 0.0, lon ?: 0.0)

                ExifData(
                    cameraMake = make,
                    cameraModel = model,
                    lensModel = lens,
                    exposureTime = exp,
                    fNumber = fNum,
                    iso = iso,
                    focalLength = focal,
                    dateTime = date,
                    dimensions = dim,
                    flash = flash,
                    whiteBalance = wb,
                    software = software,
                    latitude = lat,
                    longitude = lon,
                    rawTags = map,
                    hasExif = hasAny
                )
            } ?: ExifData(error = "Cannot open image stream")
        } catch (e: Exception) {
            ExifData(error = e.message ?: "Failed to read EXIF metadata")
        }
    }
}
