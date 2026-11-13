package com.vennela365.viewer.parser

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

enum class DataNodeKind { OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL, ELEMENT }

data class DataNode(
    val id: String,
    val key: String,
    val kind: DataNodeKind,
    val previewValue: String,
    val fullValue: String,
    val path: String,
    val depth: Int,
    val childCount: Int = 0,
    val children: List<DataNode> = emptyList()
)

data class StructuredParseResult(
    val formatLabel: String,
    val roots: List<DataNode>,
    val prettyFormatted: String,
    val totalNodes: Int,
    val error: String? = null
)

data class SvgInspectionResult(
    val sanitizedHtmlDocument: String,
    val rawSvg: String,
    val viewBox: String,
    val widthLabel: String,
    val heightLabel: String,
    val pathCount: Int,
    val shapeCount: Int,
    val groupCount: Int,
    val paletteHexes: List<String>
)

data class TimedCue(
    val index: Int,
    val timeRange: String,
    val speakerOrMeta: String = "",
    val text: String
)

data class GoogleShortcutInfo(
    val docId: String?,
    val url: String?,
    val email: String?,
    val resourceKey: String?
)

/**
 * Pure Kotlin parser & sanitizer for:
 * - JSON / JSONC / JSON5 / NDJSON / GeoJSON / XML / YAML / TOML / INI / ENV structured trees
 * - Google Drive shortcut pointers (.gdoc / .gsheet / .gslides)
 * - Sandboxed SVG/SVGZ inspection & script/network stripping
 * - Subtitles, synced lyrics & playlists (.srt, .vtt, .ass, .lrc, .m3u, .m3u8, .pls)
 */
object StructuredDataParser {

    private const val MAX_TREE_NODES = 2500

    fun parseStructuredTree(source: String, extension: String): StructuredParseResult {
        val ext = extension.lowercase().removePrefix(".")
        if (source.isBlank()) {
            return StructuredParseResult(ext.uppercase(), emptyList(), "", 0, "Empty structured file.")
        }
        return when {
            ext in setOf("xml", "xsd", "xsl", "xslt", "svg", "kml", "gpx", "plist") -> parseXmlTree(source, ext)
            ext in setOf("yaml", "yml", "toml", "ini", "cfg", "conf", "config", "env", "dotenv", "properties", "editorconfig") ->
                parseKeyValOrYamlTree(source, ext)
            ext in setOf("ndjson", "jsonl") -> parseNdjsonTree(source, ext)
            else -> parseJsonTree(source, ext)
        }
    }

    fun parseGoogleShortcut(source: String): GoogleShortcutInfo? {
        if (!source.trimStart().startsWith("{")) return null
        return runCatching {
            val obj = JSONObject(source)
            val url = obj.optString("url").takeIf { it.isNotBlank() }
            val docId = obj.optString("doc_id").takeIf { it.isNotBlank() }
            val email = obj.optString("email").takeIf { it.isNotBlank() }
            val rKey = obj.optString("resource_key").takeIf { it.isNotBlank() }
            if (url != null || docId != null) GoogleShortcutInfo(docId, url, email, rKey) else null
        }.getOrNull()
    }

    private fun parseJsonTree(rawSource: String, ext: String): StructuredParseResult {
        val cleaned = if (ext in setOf("jsonc", "json5")) stripJsonComments(rawSource) else rawSource
        var nodeCounter = 0

        fun buildNode(key: String, value: Any?, path: String, depth: Int): DataNode {
            if (nodeCounter >= MAX_TREE_NODES) {
                return DataNode("n_${nodeCounter++}", key, DataNodeKind.STRING, "… (node limit)", "", path, depth)
            }
            val id = "n_${nodeCounter++}_$path"
            return when (value) {
                is JSONObject -> {
                    val keys = value.keys().asSequence().toList()
                    val children = if (depth < 12) {
                        keys.take(250).map { k ->
                            val childPath = if (path == "$") "$.$k" else "$path.$k"
                            buildNode(k, value.opt(k), childPath, depth + 1)
                        }
                    } else emptyList()
                    DataNode(id, key, DataNodeKind.OBJECT, "{ ${keys.size} keys }", "", path, depth, keys.size, children)
                }
                is JSONArray -> {
                    val len = value.length()
                    val children = if (depth < 12) {
                        (0 until minOf(len, 250)).map { idx ->
                            val childPath = "$path[$idx]"
                            buildNode("[$idx]", value.opt(idx), childPath, depth + 1)
                        }
                    } else emptyList()
                    DataNode(id, key, DataNodeKind.ARRAY, "[ $len items ]", "", path, depth, len, children)
                }
                JSONObject.NULL, null ->
                    DataNode(id, key, DataNodeKind.NULL, "null", "null", path, depth)
                is Boolean ->
                    DataNode(id, key, DataNodeKind.BOOLEAN, value.toString(), value.toString(), path, depth)
                is Number ->
                    DataNode(id, key, DataNodeKind.NUMBER, value.toString(), value.toString(), path, depth)
                else -> {
                    val s = value.toString()
                    val preview = if (s.length > 72) "\"${s.take(69)}…\"" else "\"$s\""
                    DataNode(id, key, DataNodeKind.STRING, preview, s, path, depth)
                }
            }
        }

        return runCatching {
            val parsed = JSONTokener(cleaned.trim()).nextValue()
            val rootNode = buildNode("root", parsed, "$", 0)
            val pretty = when (parsed) {
                is JSONObject -> parsed.toString(2)
                is JSONArray -> parsed.toString(2)
                else -> rawSource
            }
            StructuredParseResult(
                formatLabel = ext.uppercase(),
                roots = listOf(rootNode),
                prettyFormatted = pretty,
                totalNodes = nodeCounter
            )
        }.getOrElse { err ->
            StructuredParseResult(
                formatLabel = ext.uppercase(),
                roots = emptyList(),
                prettyFormatted = rawSource,
                totalNodes = 0,
                error = err.message ?: "Invalid JSON syntax."
            )
        }
    }

    private fun parseNdjsonTree(rawSource: String, ext: String): StructuredParseResult {
        val lines = rawSource.lines().filter { it.isNotBlank() }.take(200)
        val wrapper = JSONArray()
        lines.forEach { line ->
            runCatching { JSONTokener(line).nextValue() }.getOrNull()?.let { wrapper.put(it) }
        }
        return parseJsonTree(wrapper.toString(), ext)
    }

    private fun stripJsonComments(src: String): String {
        val out = StringBuilder(src.length)
        var i = 0
        var inStr = false
        var escaped = false
        while (i < src.length) {
            val c = src[i]
            if (inStr) {
                out.append(c)
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == '"') inStr = false
                i++
            } else {
                if (c == '"') {
                    inStr = true
                    out.append(c)
                    i++
                } else if (c == '/' && i + 1 < src.length && src[i + 1] == '/') {
                    while (i < src.length && src[i] != '\n') i++
                } else if (c == '/' && i + 1 < src.length && src[i + 1] == '*') {
                    i += 2
                    while (i + 1 < src.length && !(src[i] == '*' && src[i + 1] == '/')) i++
                    i += 2
                } else {
                    out.append(c)
                    i++
                }
            }
        }
        return out.toString()
    }

    private fun parseXmlTree(source: String, ext: String): StructuredParseResult {
        var counter = 0
        data class MutableXml(
            val id: String,
            val tag: String,
            val attrsSummary: String,
            val path: String,
            val depth: Int,
            val textBuf: StringBuilder = StringBuilder(),
            val children: MutableList<MutableXml> = mutableListOf()
        )

        val stack = ArrayDeque<MutableXml>()
        val top = mutableListOf<MutableXml>()

        val err = runCatching {
            val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
            val parser = factory.newPullParser()
            parser.setInput(StringReader(source))
            var ev = parser.eventType
            while (ev != XmlPullParser.END_DOCUMENT && counter < MAX_TREE_NODES) {
                when (ev) {
                    XmlPullParser.START_TAG -> {
                        val tag = parser.name
                        val attrCount = parser.attributeCount
                        val attrPreview = if (attrCount > 0) {
                            (0 until minOf(attrCount, 4)).joinToString(" ") { idx ->
                                "${parser.getAttributeName(idx)}=\"${parser.getAttributeValue(idx)}\""
                            } + if (attrCount > 4) " …" else ""
                        } else ""
                        val parent = stack.lastOrNull()
                        val path = if (parent == null) "/$tag" else "${parent.path}/$tag"
                        val node = MutableXml("xml_${counter++}", tag, attrPreview, path, stack.size)
                        if (parent == null) top += node else parent.children += node
                        stack.addLast(node)
                    }
                    XmlPullParser.TEXT -> {
                        val txt = parser.text.orEmpty().trim()
                        if (txt.isNotEmpty()) stack.lastOrNull()?.textBuf?.append(txt)?.append(" ")
                    }
                    XmlPullParser.END_TAG -> {
                        if (stack.isNotEmpty()) stack.removeLast()
                    }
                }
                ev = parser.next()
            }
        }.exceptionOrNull()?.message

        fun freeze(m: MutableXml): DataNode {
            val childNodes = m.children.map { freeze(it) }
            val text = m.textBuf.toString().trim()
            val preview = buildString {
                if (m.attrsSummary.isNotEmpty()) append(m.attrsSummary)
                if (text.isNotEmpty()) {
                    if (isNotEmpty()) append(" • ")
                    append(if (text.length > 56) text.take(53) + "…" else text)
                }
                if (isEmpty()) append(if (childNodes.isNotEmpty()) "< ${childNodes.size} nodes >" else "<empty>")
            }
            return DataNode(
                id = m.id,
                key = "<${m.tag}>",
                kind = if (childNodes.isNotEmpty()) DataNodeKind.ELEMENT else DataNodeKind.STRING,
                previewValue = preview,
                fullValue = text.ifEmpty { m.attrsSummary },
                path = m.path,
                depth = m.depth,
                childCount = childNodes.size,
                children = childNodes
            )
        }

        return StructuredParseResult(
            formatLabel = ext.uppercase(),
            roots = top.map { freeze(it) },
            prettyFormatted = source,
            totalNodes = counter,
            error = err
        )
    }

    private fun parseKeyValOrYamlTree(source: String, ext: String): StructuredParseResult {
        var counter = 0
        val roots = mutableListOf<DataNode>()
        val isSectionBased = ext in setOf("toml", "ini", "cfg", "conf", "env", "dotenv", "properties", "editorconfig")

        if (isSectionBased) {
            var currentSection = "root"
            var sectionChildren = mutableListOf<DataNode>()
            fun flushSection() {
                if (sectionChildren.isNotEmpty()) {
                    if (currentSection == "root") {
                        roots += sectionChildren
                    } else {
                        roots += DataNode(
                            id = "sec_${counter++}",
                            key = "[$currentSection]",
                            kind = DataNodeKind.OBJECT,
                            previewValue = "${sectionChildren.size} entries",
                            fullValue = "",
                            path = currentSection,
                            depth = 0,
                            childCount = sectionChildren.size,
                            children = sectionChildren
                        )
                    }
                    sectionChildren = mutableListOf()
                }
            }

            source.lines().take(1500).forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) return@forEach
                if (line.startsWith("[") && line.endsWith("]") && line.length > 2) {
                    flushSection()
                    currentSection = line.substring(1, line.length - 1).trim()
                } else if (line.contains("=")) {
                    val k = line.substringBefore("=").trim()
                    val v = line.substringAfter("=").trim().removeSurrounding("\"").removeSurrounding("'")
                    val kind = inferScalarKind(v)
                    val path = if (currentSection == "root") k else "$currentSection.$k"
                    sectionChildren += DataNode("kv_${counter++}", k, kind, v, v, path, if (currentSection == "root") 0 else 1)
                }
            }
            flushSection()
        } else {
            // Indentation-aware YAML parser
            data class YamlItem(val key: String, val valStr: String, val indent: Int, val children: MutableList<YamlItem> = mutableListOf())
            val stack = mutableListOf<YamlItem>()
            val topItems = mutableListOf<YamlItem>()

            source.lines().take(1500).forEach { raw ->
                if (raw.isBlank() || raw.trimStart().startsWith("#") || raw.trim() == "---") return@forEach
                val indent = raw.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
                val trimmed = raw.trim().removePrefix("- ").trim()
                val colon = trimmed.indexOf(':')
                val k = if (colon > 0) trimmed.substring(0, colon).trim() else trimmed
                val v = if (colon > 0) trimmed.substring(colon + 1).trim().removeSurrounding("\"").removeSurrounding("'") else ""
                val item = YamlItem(k, v, indent)
                while (stack.isNotEmpty() && stack.last().indent >= indent) {
                    stack.removeAt(stack.lastIndex)
                }
                if (stack.isEmpty()) topItems += item else stack.last().children += item
                stack += item
            }

            fun toDataNode(item: YamlItem, parentPath: String, depth: Int): DataNode {
                val path = if (parentPath.isEmpty()) item.key else "$parentPath.${item.key}"
                val childNodes = item.children.map { toDataNode(it, path, depth + 1) }
                val kind = if (childNodes.isNotEmpty()) DataNodeKind.OBJECT else inferScalarKind(item.valStr)
                val preview = if (childNodes.isNotEmpty()) "${childNodes.size} items" else item.valStr.ifBlank { "{ }" }
                return DataNode("yml_${counter++}", item.key, kind, preview, item.valStr, path, depth, childNodes.size, childNodes)
            }
            roots += topItems.map { toDataNode(it, "", 0) }
        }

        return StructuredParseResult(
            formatLabel = ext.uppercase(),
            roots = roots,
            prettyFormatted = source,
            totalNodes = counter
        )
    }

    private fun inferScalarKind(v: String): DataNodeKind = when {
        v.equals("true", true) || v.equals("false", true) -> DataNodeKind.BOOLEAN
        v.equals("null", true) || v == "~" -> DataNodeKind.NULL
        v.toDoubleOrNull() != null -> DataNodeKind.NUMBER
        else -> DataNodeKind.STRING
    }

    /**
     * Sanitizes SVG strictly for offline-only WebView rendering:
     * removes `<script>`, `<foreignObject>`, inline `on*=` attributes,
     * `javascript:` URLs, and external `http(s)://` URLs.
     */
    fun inspectAndSanitizeSvg(rawSvg: String, darkBackdrop: Boolean = true): SvgInspectionResult {
        var safe = rawSvg
        safe = safe.replace(Regex("""<script\b[^>]*>.*?</script>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
        safe = safe.replace(Regex("""<foreignObject\b[^>]*>.*?</foreignObject>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
        safe = safe.replace(Regex("""\son[a-z]+\s*=\s*(?:"[^"]*"|'[^']*'|[^\s>]+)""", RegexOption.IGNORE_CASE), "")
        safe = safe.replace(Regex("""javascript\s*:""", RegexOption.IGNORE_CASE), "blocked:")
        safe = safe.replace(Regex("""https?://[^\s"'<>)]+""", RegexOption.IGNORE_CASE), "#blocked-external")

        val svgMatch = Regex("""<svg\b[^>]*>.*?</svg>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(safe)
        val cleanSvg = svgMatch?.value ?: safe

        val svgOpenTag = Regex("""<svg\b([^>]*)>""", RegexOption.IGNORE_CASE).find(cleanSvg)?.groupValues?.getOrNull(1).orEmpty()
        val viewBox = Regex("""viewBox\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(svgOpenTag)?.groupValues?.getOrNull(1) ?: "auto"
        val width = Regex("""\bwidth\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(svgOpenTag)?.groupValues?.getOrNull(1) ?: "auto"
        val height = Regex("""\bheight\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(svgOpenTag)?.groupValues?.getOrNull(1) ?: "auto"

        val pathCount = Regex("""<path\b""", RegexOption.IGNORE_CASE).findAll(cleanSvg).count()
        val shapeCount = Regex("""<(rect|circle|ellipse|polygon|polyline|line)\b""", RegexOption.IGNORE_CASE).findAll(cleanSvg).count()
        val groupCount = Regex("""<g\b""", RegexOption.IGNORE_CASE).findAll(cleanSvg).count()

        val paletteHexes = Regex("""#(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{3})\b""")
            .findAll(cleanSvg)
            .map { it.value.uppercase() }
            .distinct()
            .take(12)
            .toList()

        val bgCss = if (darkBackdrop) "#0D111C" else "#FFFFFF"
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=6.0, user-scalable=yes"/>
              <style>
                html, body {
                  margin: 0; padding: 12px; width: 100%; height: 100%;
                  box-sizing: border-box;
                  background: $bgCss;
                  display: flex; align-items: center; justify-content: center;
                }
                svg {
                  max-width: 100%; max-height: 100%; width: auto; height: auto;
                  display: block;
                }
              </style>
            </head>
            <body>$cleanSvg</body>
            </html>
        """.trimIndent()

        return SvgInspectionResult(
            sanitizedHtmlDocument = html,
            rawSvg = cleanSvg,
            viewBox = viewBox,
            widthLabel = width,
            heightLabel = height,
            pathCount = pathCount,
            shapeCount = shapeCount,
            groupCount = groupCount,
            paletteHexes = paletteHexes
        )
    }

    /** Parses subtitles (`.srt`, `.vtt`, `.ass`), lyrics (`.lrc`), and playlists (`.m3u`, `.m3u8`, `.pls`). */
    fun parseTimedTracks(source: String, extension: String): List<TimedCue> {
        val ext = extension.lowercase().removePrefix(".")
        if (source.isBlank()) return emptyList()
        return when (ext) {
            "srt" -> parseSrtOrVtt(source, isVtt = false)
            "vtt", "webvtt" -> parseSrtOrVtt(source, isVtt = true)
            "lrc" -> parseLrc(source)
            "ass", "ssa" -> parseAss(source)
            "m3u", "m3u8", "pls" -> parsePlaylist(source, ext)
            else -> emptyList()
        }
    }

    private fun parseSrtOrVtt(source: String, isVtt: Boolean): List<TimedCue> {
        val blocks = source.replace("\r\n", "\n").split(Regex("""\n{2,}"""))
        val cues = mutableListOf<TimedCue>()
        blocks.forEach { block ->
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) return@forEach
            if (isVtt && (lines.first().startsWith("WEBVTT") || lines.first().startsWith("NOTE"))) return@forEach
            val timeLineIdx = lines.indexOfFirst { it.contains("-->") }
            if (timeLineIdx >= 0) {
                val timeRange = lines[timeLineIdx].substringBefore(" align:").substringBefore(" position:").trim()
                val text = lines.drop(timeLineIdx + 1).joinToString(" ").replace(Regex("""<[^>]+>"""), "")
                if (text.isNotBlank()) {
                    cues += TimedCue(cues.size + 1, timeRange, if (isVtt) "WebVTT" else "SRT", text)
                }
            }
        }
        return cues
    }

    private fun parseLrc(source: String): List<TimedCue> {
        val cues = mutableListOf<TimedCue>()
        val timeRegex = Regex("""\[(\d{1,2}:\d{2}(?:\.\d{1,3})?)]""")
        source.lines().forEach { line ->
            val trimmed = line.trim()
            val match = timeRegex.find(trimmed)
            if (match != null) {
                val ts = match.groupValues[1]
                val lyric = trimmed.replace(timeRegex, "").trim()
                if (lyric.isNotBlank()) cues += TimedCue(cues.size + 1, ts, "Lyric", lyric)
            }
        }
        return cues
    }

    private fun parseAss(source: String): List<TimedCue> {
        val cues = mutableListOf<TimedCue>()
        source.lines().forEach { line ->
            if (line.startsWith("Dialogue:", ignoreCase = true)) {
                val parts = line.substringAfter(":").split(",", limit = 10)
                if (parts.size >= 10) {
                    val start = parts[1].trim()
                    val end = parts[2].trim()
                    val style = parts[3].trim()
                    val txt = parts[9].replace(Regex("""\{[^}]*\}"""), "").replace("\\N", " ").trim()
                    if (txt.isNotBlank()) cues += TimedCue(cues.size + 1, "$start → $end", style, txt)
                }
            }
        }
        return cues
    }

    private fun parsePlaylist(source: String, ext: String): List<TimedCue> {
        val cues = mutableListOf<TimedCue>()
        if (ext == "pls") {
            val titles = mutableMapOf<String, String>()
            val files = linkedMapOf<String, String>()
            source.lines().forEach { line ->
                val t = line.trim()
                if (t.startsWith("File", true) && t.contains("=")) {
                    val key = t.substringBefore("=").drop(4)
                    files[key] = t.substringAfter("=")
                } else if (t.startsWith("Title", true) && t.contains("=")) {
                    val key = t.substringBefore("=").drop(5)
                    titles[key] = t.substringAfter("=")
                }
            }
            files.forEach { (k, url) ->
                cues += TimedCue(cues.size + 1, "#${cues.size + 1}", titles[k].orEmpty().ifBlank { "Track" }, url)
            }
        } else {
            var pendingTitle = ""
            source.lines().forEach { raw ->
                val line = raw.trim()
                if (line.isEmpty()) return@forEach
                if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                    pendingTitle = line.substringAfter(",", "").trim()
                } else if (!line.startsWith("#")) {
                    cues += TimedCue(
                        index = cues.size + 1,
                        timeRange = "Track ${cues.size + 1}",
                        speakerOrMeta = pendingTitle.ifBlank { "Stream" },
                        text = line
                    )
                    pendingTitle = ""
                }
            }
        }
        return cues
    }
}
