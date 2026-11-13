package com.vennela365.viewer.storage

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.util.LruCache
import com.vennela365.viewer.model.ArchiveEntry
import com.vennela365.viewer.model.HexRow
import com.vennela365.viewer.model.SpreadsheetSheet

/**
 * Vennela 365 0-Latency In-Memory Cache Manager.
 *
 * Provides instant (<1ms) repeated file access while strictly protecting
 * low-end devices (Android Go / 1GB RAM) from Out-Of-Memory (OOM) crashes via
 * ComponentCallbacks2 trim memory eviction.
 */
object VennelaCacheManager : ComponentCallbacks2 {

    private var isInitialized = false

    // Cache budgets tuned for low-end device safety:
    // Text cache: Max 4MB of strings
    private val textCache = object : LruCache<String, String>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: String): Int = value.length * 2
    }

    // Spreadsheet cache: Max 32 sheets
    private val spreadsheetCache = LruCache<String, List<SpreadsheetSheet>>(32)

    // Archive entries cache: Max 32 archives
    private val archiveCache = LruCache<String, List<ArchiveEntry>>(32)

    // Hex rows cache: Max 32 files
    private val hexCache = LruCache<String, List<HexRow>>(32)

    // Checksum & metadata cache
    private val metadataCache = LruCache<String, Map<String, String>>(128)

    fun initialize(context: Context) {
        if (!isInitialized) {
            context.applicationContext.registerComponentCallbacks(this)
            isInitialized = true
        }
    }

    // ================= TEXT CACHE =================
    fun getCachedText(uriKey: String): String? = textCache.get(uriKey)

    fun putCachedText(uriKey: String, text: String) {
        if (text.length * 2 < 2 * 1024 * 1024) { // Don't cache single items larger than 2MB
            textCache.put(uriKey, text)
        }
    }

    // ================= SPREADSHEET CACHE =================
    fun getCachedSheets(uriKey: String): List<SpreadsheetSheet>? = spreadsheetCache.get(uriKey)

    fun putCachedSheets(uriKey: String, sheets: List<SpreadsheetSheet>) {
        spreadsheetCache.put(uriKey, sheets)
    }

    // ================= ARCHIVE CACHE =================
    fun getCachedArchive(uriKey: String): List<ArchiveEntry>? = archiveCache.get(uriKey)

    fun putCachedArchive(uriKey: String, entries: List<ArchiveEntry>) {
        archiveCache.put(uriKey, entries)
    }

    // ================= HEX CACHE =================
    fun getCachedHex(uriKey: String): List<HexRow>? = hexCache.get(uriKey)

    fun putCachedHex(uriKey: String, rows: List<HexRow>) {
        hexCache.put(uriKey, rows)
    }

    // ================= METADATA CACHE =================
    fun getCachedMetadata(uriKey: String): Map<String, String>? = metadataCache.get(uriKey)

    fun putCachedMetadata(uriKey: String, metadata: Map<String, String>) {
        metadataCache.put(uriKey, metadata)
    }

    fun clear() {
        textCache.evictAll()
        spreadsheetCache.evictAll()
        archiveCache.evictAll()
        hexCache.evictAll()
        metadataCache.evictAll()
        com.vennela365.viewer.parser.CodeSyntaxHighlighter.clearCache()
    }

    // Low-end device memory pressure handler
    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                // Aggressive eviction when device is starving for memory
                clear()
            }
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                // Partial eviction: trim down to half
                textCache.trimToSize(textCache.maxSize() / 2)
                spreadsheetCache.trimToSize(spreadsheetCache.maxSize() / 2)
                archiveCache.trimToSize(archiveCache.maxSize() / 2)
                hexCache.trimToSize(hexCache.maxSize() / 2)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}

    @Suppress("DEPRECATION")
    override fun onLowMemory() {
        clear()
    }
}
