package com.vennela365.viewer.parser

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class FontMetadata(
    val fontName: String = "",
    val style: String = "Regular",
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val fileSizeFormatted: String = "",
    val error: String? = null
)

data class FontLoadResult(
    val typeface: Typeface? = null,
    val metadata: FontMetadata = FontMetadata()
)

object FontParser {

    private const val MAX_FONT_BYTES = 25 * 1024 * 1024

    suspend fun loadFont(context: Context, uri: Uri?): FontLoadResult = withContext(Dispatchers.IO) {
        if (uri == null) {
            return@withContext FontLoadResult(
                metadata = FontMetadata(error = "No font file selected")
            )
        }

        val tempFontFile = File(context.cacheDir, "vennela_preview_font.ttf")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFontFile).use { output ->
                    val buffer = ByteArray(16384)
                    var bytesRead = input.read(buffer)
                    var total = 0
                    while (bytesRead != -1 && total < MAX_FONT_BYTES) {
                        output.write(buffer, 0, bytesRead)
                        total += bytesRead
                        bytesRead = input.read(buffer)
                    }
                }
            } ?: return@withContext FontLoadResult(
                metadata = FontMetadata(error = "Could not open font stream")
            )

            val tf = Typeface.createFromFile(tempFontFile)
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "CustomFont.ttf"
            val styleStr = when {
                tf.isBold && tf.isItalic -> "Bold Italic"
                tf.isBold -> "Bold"
                tf.isItalic -> "Italic"
                else -> "Regular"
            }
            val sizeKb = (tempFontFile.length() / 1024).coerceAtLeast(1)

            FontLoadResult(
                typeface = tf,
                metadata = FontMetadata(
                    fontName = fileName.substringBeforeLast('.'),
                    style = styleStr,
                    isBold = tf.isBold,
                    isItalic = tf.isItalic,
                    fileSizeFormatted = "$sizeKb KB"
                )
            )
        } catch (e: Exception) {
            FontLoadResult(
                metadata = FontMetadata(error = e.message ?: "Failed to parse font file")
            )
        }
    }
}

