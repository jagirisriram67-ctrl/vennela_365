package com.vennela365.viewer.security

/** File-safety budgets — spec §7. Every parser must respect these. */
object SafeLimits {
    /** Never read more than this for type sniffing. */
    const val SNIFF_BYTES = 32 * 1024
    /** Hex/text preview window. */
    const val PREVIEW_BYTES = 64 * 1024
    const val PREVIEW_LINES = 2_000
    /** Archive bomb / traversal guards. */
    const val MAX_ARCHIVE_ENTRIES = 500
    const val MAX_ARCHIVE_DEPTH = 4
    const val MAX_UNCOMPRESSED_RATIO = 100L
    const val MAX_TEXT_FILE_BYTES = 10 * 1024 * 1024L
    const val LARGE_FILE_WARN_BYTES = 50 * 1024 * 1024L

    /** Reject zip-slip entries before touching disk. */
    fun isSafeEntryPath(path: String): Boolean {
        val n = path.replace('\\', '/')
        return !(n.contains("../") || n.startsWith("/") || n.contains("..\\") || n.startsWith("~"))
    }
}
