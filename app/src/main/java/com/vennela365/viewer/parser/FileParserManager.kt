package com.vennela365.viewer.parser

import android.content.Context
import android.net.Uri
import com.vennela365.viewer.model.HexRow
import com.vennela365.viewer.storage.VennelaCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest

object FileParserManager {

    suspend fun readHexDump(
        context: Context,
        uri: Uri?,
        maxBytes: Int = 1024 * 64, // 64 KB preview for speed
        bytesPerLine: Int = 16
    ): List<HexRow> = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext emptyList()

        val cacheKey = "hex_${uri}_${maxBytes}"
        VennelaCacheManager.getCachedHex(cacheKey)?.let { return@withContext it }

        val rows = ArrayList<HexRow>()
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(bytesPerLine)
                var offset = 0L
                var totalRead = 0

                while (totalRead < maxBytes) {
                    val bytesRead = stream.read(buffer)
                    if (bytesRead <= 0) break

                    val hexBuilder = StringBuilder()
                    val asciiBuilder = StringBuilder()

                    for (i in 0 until bytesRead) {
                        val b = buffer[i].toInt() and 0xFF
                        hexBuilder.append(String.format("%02X ", b))
                        if (b in 32..126) {
                            asciiBuilder.append(b.toChar())
                        } else {
                            asciiBuilder.append('.')
                        }
                    }

                    // Pad remaining if last row is incomplete
                    for (i in bytesRead until bytesPerLine) {
                        hexBuilder.append("   ")
                    }

                    val offsetStr = String.format("%08X", offset)
                    rows.add(HexRow(offsetStr, hexBuilder.toString().trimEnd(), asciiBuilder.toString()))

                    offset += bytesRead
                    totalRead += bytesRead
                }
            }
        } catch (e: Exception) {
            rows.add(HexRow("ERROR", "Unable to read stream", e.localizedMessage ?: "I/O Error"))
        }

        VennelaCacheManager.putCachedHex(cacheKey, rows)
        rows
    }

    suspend fun readTextContent(
        context: Context,
        uri: Uri?,
        maxLines: Int = 2000
    ): String = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext ""
        val cacheKey = "rawtext_${uri}_${maxLines}"
        VennelaCacheManager.getCachedText(cacheKey)?.let { return@withContext it }

        val content = try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                val sb = StringBuilder()
                var count = 0
                var line = reader.readLine()
                while (line != null && count < maxLines) {
                    sb.append(line).append('\n')
                    count++
                    line = reader.readLine()
                }
                sb.toString()
            } ?: ""
        } catch (e: Exception) {
            "Unable to read file: ${e.message}"
        }

        VennelaCacheManager.putCachedText(cacheKey, content)
        content
    }

    suspend fun computeChecksum(
        context: Context,
        uri: Uri?,
        algorithm: String = "SHA-256"
    ): String = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext "N/A"
        try {
            val digest = MessageDigest.getInstance(algorithm)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(8192)
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            "N/A"
        }
    }

    /**
     * Bounded magic-byte container sniffer for 50+ formats.
     */
    fun detectMagicFormat(bytes: ByteArray): String {
        if (bytes.size < 4) return "Unknown"
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        val b2 = bytes[2].toInt() and 0xFF
        val b3 = bytes[3].toInt() and 0xFF

        return when {
            // ZIP containers (Office OpenXML, APK, JAR, EPUB, CBZ)
            b0 == 0x50 && b1 == 0x4B && b2 == 0x03 && b3 == 0x04 -> "ZIP / Office Open XML / APK Container"
            // Documents
            b0 == 0x25 && b1 == 0x50 && b2 == 0x44 && b3 == 0x46 -> "PDF Document"
            b0 == 0xD0 && b1 == 0xCF && b2 == 0x11 && b3 == 0xE0 -> "Compound File Binary (DOC/XLS/PPT/MSI)"
            b0 == 0x7B && b1 == 0x5C && b2 == 0x72 && b3 == 0x74 -> "Rich Text Format (RTF)"
            // Images
            b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47 -> "PNG Image"
            b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF -> "JPEG Image"
            b0 == 0x47 && b1 == 0x49 && b2 == 0x46 -> "GIF Image"
            b0 == 0x42 && b1 == 0x4D -> "Bitmap Image (BMP)"
            b0 == 0x49 && b1 == 0x49 && b2 == 0x2A && b3 == 0x00 -> "TIFF Image (Little Endian)"
            b0 == 0x4D && b1 == 0x4D && b2 == 0x00 && b3 == 0x2A -> "TIFF Image (Big Endian)"
            b0 == 0x00 && b1 == 0x00 && b2 == 0x01 && b3 == 0x00 -> "Windows Icon (ICO)"
            b0 == 0x38 && b1 == 0x42 && b2 == 0x50 && b3 == 0x53 -> "Photoshop Document (PSD)"
            // Audio / Video Containers
            b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46 -> "RIFF Container (WAV/AVI/WEBP)"
            b0 == 0x66 && b1 == 0x4C && b2 == 0x61 && b3 == 0x43 -> "FLAC Lossless Audio"
            b0 == 0x49 && b1 == 0x44 && b2 == 0x33 -> "MP3 Audio (ID3v2 Tag)"
            b0 == 0xFF && (b1 and 0xFE == 0xFA || b1 and 0xFE == 0xF2) -> "MPEG Audio (MP3)"
            b0 == 0x4F && b1 == 0x67 && b2 == 0x67 && b3 == 0x53 -> "Ogg Multimedia Container (OGG/Opus)"
            b0 == 0x4D && b1 == 0x54 && b2 == 0x68 && b3 == 0x64 -> "Standard MIDI File"
            b0 == 0x1A && b1 == 0x45 && b2 == 0xDF && b3 == 0xA3 -> "Matroska / WebM Media"
            b0 == 0x00 && b1 == 0x00 && b2 == 0x00 && (bytes.size > 8 && bytes[4].toInt() == 0x66 && bytes[5].toInt() == 0x74 && bytes[6].toInt() == 0x79 && bytes[7].toInt() == 0x70) -> "MP4 / ISO Base Media"
            // Archives & Compression
            b0 == 0x1F && b1 == 0x8B -> "GZIP Compressed File"
            b0 == 0x42 && b1 == 0x5A && b2 == 0x68 -> "BZip2 Compressed Archive"
            b0 == 0xFD && b1 == 0x37 && b2 == 0x7A && b3 == 0x58 -> "XZ Compressed Archive"
            b0 == 0x28 && b1 == 0xB5 && b2 == 0x2F && b3 == 0xFD -> "Zstandard (ZST) Archive"
            b0 == 0x37 && b1 == 0x7A && b2 == 0xBC && b3 == 0xAF -> "7-Zip Compressed Archive"
            b0 == 0x52 && b1 == 0x61 && b2 == 0x72 && b3 == 0x21 -> "RAR Compressed Archive"
            // Binaries & Executables
            b0 == 0x7F && b1 == 0x45 && b2 == 0x4C && b3 == 0x46 -> "ELF Linux / Android Native Binary"
            b0 == 0x4D && b1 == 0x5A -> "DOS / Windows PE Executable"
            b0 == 0xCA && b1 == 0xFE && b2 == 0xBA && b3 == 0xBE -> "Java Class Bytecode / Mach-O Fat"
            b0 == 0x64 && b1 == 0x65 && b2 == 0x78 && b3 == 0x0A -> "Android DEX Executable"
            b0 == 0x00 && b1 == 0x61 && b2 == 0x73 && b3 == 0x6D -> "WebAssembly Binary (WASM)"
            // Databases & Science
            b0 == 0x53 && b1 == 0x51 && b2 == 0x4C && b3 == 0x69 -> "SQLite 3 Database"
            b0 == 0x50 && b1 == 0x41 && b2 == 0x52 && b3 == 0x31 -> "Apache Parquet Table"
            b0 == 0x89 && b1 == 0x48 && b2 == 0x44 && b3 == 0x46 -> "HDF5 Scientific Data"
            // Fonts
            b0 == 0x77 && b1 == 0x4F && b2 == 0x46 && b3 == 0x46 -> "WOFF Font"
            b0 == 0x77 && b1 == 0x4F && b2 == 0x46 && b3 == 0x32 -> "WOFF2 Font"
            b0 == 0x00 && b1 == 0x01 && b2 == 0x00 && b3 == 0x00 -> "TrueType Font (TTF)"
            b0 == 0x4F && b1 == 0x54 && b2 == 0x54 && b3 == 0x4F -> "OpenType Font (OTF)"
            else -> "Binary Data Stream"
        }
    }

    fun generateSampleHexDump(): List<HexRow> {
        val sample = listOf(
            Triple("00000000", "50 4B 03 04 14 00 00 00 08 00 A3 7F 4B 5A 00 00", "PK..........KZ.."),
            Triple("00000010", "00 00 00 00 00 00 00 00 00 00 09 00 1C 00 00 00", "................"),
            Triple("00000020", "64 61 74 61 2F 00 00 00 01 00 00 00 20 B8 07 00", "data/....... ..."),
            Triple("00000030", "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00", "................"),
            Triple("00000040", "FF D8 FF E0 00 10 4A 46 49 46 00 01 01 00 00 01", "......JFIF......"),
            Triple("00000050", "00 01 00 00 FF DB 00 43 00 03 02 02 03 02 02 03", ".......C........"),
            Triple("00000060", "03 03 03 04 03 03 03 04 04 04 04 05 06 06 05 06", "................"),
            Triple("00000070", "07 06 07 07 06 06 08 0A 10 0A 0A 09 09 09 0A 14", "................"),
            Triple("00000080", "0D 0A 0C 0E 0B 0C 14 14 14 14 14 14 14 14 14 14", "................"),
            Triple("00000090", "FF DB 00 43 01 03 04 04 05 04 05 09 05 05 09 14", "...C............"),
            Triple("000000A0", "0D 0C 0D 14 14 14 14 14 14 14 14 14 14 14 14 14", "................"),
            Triple("000000B0", "14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14", "................"),
            Triple("00000100", "49 45 4E 44 AE 42 60 82 50 4B 01 02 14 03 14 00", "IEND.B`.PK......")
        )
        return sample.map { HexRow(it.first, it.second, it.third) }
    }
}
