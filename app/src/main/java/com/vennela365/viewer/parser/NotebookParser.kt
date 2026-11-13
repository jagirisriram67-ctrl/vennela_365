package com.vennela365.viewer.parser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

sealed interface NotebookOutput {
    data class TextStream(val name: String, val text: String, val isError: Boolean = false) : NotebookOutput
    data class InlineImage(val mimeType: String, val bitmap: Bitmap) : NotebookOutput
}

data class NotebookCell(
    val index: Int,
    val cellType: String, // "markdown", "code", "raw"
    val executionCount: Int?,
    val source: String,
    val outputs: List<NotebookOutput> = emptyList()
)

data class ParsedNotebook(
    val kernelName: String,
    val language: String,
    val nbFormat: String,
    val cells: List<NotebookCell>,
    val error: String? = null
) {
    val codeCount: Int get() = cells.count { it.cellType == "code" }
    val markdownCount: Int get() = cells.count { it.cellType == "markdown" }
    val outputImageCount: Int get() = cells.sumOf { cell -> cell.outputs.count { it is NotebookOutput.InlineImage } }
}

/**
 * Pure Kotlin + org.json Jupyter (.ipynb) and Quarto/RMarkdown (.qmd, .rmd) parser.
 * Cells are strictly read-only — never executed. Base64 PNG/JPEG outputs decode offline.
 */
object NotebookParser {

    private const val MAX_INLINE_IMAGE_BYTES = 4 * 1024 * 1024

    fun parse(source: String, extension: String): ParsedNotebook {
        val ext = extension.lowercase().removePrefix(".")
        if (source.isBlank()) {
            return ParsedNotebook(
                kernelName = "Unknown",
                language = "python",
                nbFormat = "",
                cells = emptyList(),
                error = "Notebook source is empty."
            )
        }
        return if (ext in setOf("rmd", "qmd")) {
            parseRMarkdown(source, ext)
        } else {
            parseIpynb(source)
        }
    }

    private fun parseIpynb(source: String): ParsedNotebook = runCatching {
        val root = JSONObject(source)
        val meta = root.optJSONObject("metadata")
        val kernelspec = meta?.optJSONObject("kernelspec")
        val langInfo = meta?.optJSONObject("language_info")
        val kernelName = kernelspec?.optString("display_name")?.takeIf { it.isNotBlank() }
            ?: langInfo?.optString("name")?.takeIf { it.isNotBlank() }
            ?: "Jupyter Kernel"
        val language = kernelspec?.optString("language")?.takeIf { it.isNotBlank() }
            ?: langInfo?.optString("name")?.takeIf { it.isNotBlank() }
            ?: "python"
        val nbMajor = root.optInt("nbformat", 4)
        val nbMinor = root.optInt("nbformat_minor", 0)
        val cellsArr = root.optJSONArray("cells") ?: JSONArray()

        val parsedCells = buildList {
            for (i in 0 until minOf(cellsArr.length(), 400)) {
                val obj = cellsArr.optJSONObject(i) ?: continue
                val type = obj.optString("cell_type", "code").lowercase()
                val execCount = if (obj.isNull("execution_count")) null else obj.optInt("execution_count", -1).takeIf { it >= 0 }
                val cellSource = readStringOrLines(obj.opt("source"))
                val outputs = parseOutputs(obj.optJSONArray("outputs"))
                add(
                    NotebookCell(
                        index = i,
                        cellType = type,
                        executionCount = execCount,
                        source = cellSource,
                        outputs = outputs
                    )
                )
            }
        }
        ParsedNotebook(
            kernelName = kernelName,
            language = language.lowercase(),
            nbFormat = "nbformat $nbMajor.$nbMinor",
            cells = parsedCells
        )
    }.getOrElse { err ->
        ParsedNotebook(
            kernelName = "Malformed JSON",
            language = "json",
            nbFormat = "",
            cells = emptyList(),
            error = err.message ?: "Could not parse Jupyter JSON."
        )
    }

    private fun parseOutputs(arr: JSONArray?): List<NotebookOutput> {
        if (arr == null || arr.length() == 0) return emptyList()
        return buildList {
            for (i in 0 until minOf(arr.length(), 32)) {
                val out = arr.optJSONObject(i) ?: continue
                when (out.optString("output_type")) {
                    "stream" -> {
                        val name = out.optString("name", "stdout")
                        val txt = readStringOrLines(out.opt("text")).trimEnd()
                        if (txt.isNotEmpty()) {
                            add(NotebookOutput.TextStream(name = name, text = txt, isError = name == "stderr"))
                        }
                    }
                    "execute_result", "display_data" -> {
                        val data = out.optJSONObject("data")
                        if (data != null) {
                            decodeBase64Plot(data, "image/png")?.let { add(NotebookOutput.InlineImage("image/png", it)) }
                            decodeBase64Plot(data, "image/jpeg")?.let { add(NotebookOutput.InlineImage("image/jpeg", it)) }
                            val plain = readStringOrLines(data.opt("text/plain")).trimEnd()
                            if (plain.isNotEmpty() && !plain.startsWith("<Figure ")) {
                                add(NotebookOutput.TextStream(name = "out", text = plain))
                            } else if (plain.isEmpty()) {
                                val html = readStringOrLines(data.opt("text/html"))
                                    .replace(Regex("""<[^>]+>"""), " ")
                                    .replace(Regex("""\s+"""), " ")
                                    .trim()
                                if (html.isNotEmpty()) {
                                    add(NotebookOutput.TextStream(name = "html preview", text = html.take(1500)))
                                }
                            }
                        }
                    }
                    "error" -> {
                        val ename = out.optString("ename", "Error")
                        val evalue = out.optString("evalue", "")
                        val trace = out.optJSONArray("traceback")?.let { tb ->
                            (0 until minOf(tb.length(), 20))
                                .joinToString("\n") { stripAnsi(tb.optString(it)) }
                        }.orEmpty()
                        val msg = buildString {
                            append(ename)
                            if (evalue.isNotEmpty()) append(": ").append(evalue)
                            if (trace.isNotEmpty()) append('\n').append(trace)
                        }
                        add(NotebookOutput.TextStream(name = "error", text = msg, isError = true))
                    }
                }
            }
        }
    }

    private fun decodeBase64Plot(data: JSONObject, mime: String): Bitmap? {
        val raw = readStringOrLines(data.opt(mime)).replace("\n", "").trim()
        if (raw.isEmpty() || raw.length > MAX_INLINE_IMAGE_BYTES * 2) return null
        return runCatching {
            val bytes = Base64.decode(raw, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    private fun readStringOrLines(value: Any?): String = when (value) {
        is String -> value
        is JSONArray -> buildString {
            for (i in 0 until value.length()) {
                append(value.optString(i))
            }
        }
        else -> ""
    }

    private fun stripAnsi(input: String): String =
        input.replace(Regex("""\u001B\[[;?\d]*m"""), "")

    private fun parseRMarkdown(source: String, ext: String): ParsedNotebook {
        val cells = mutableListOf<NotebookCell>()
        val mdBuf = StringBuilder()
        var inCode = false
        var codeLang = if (ext == "qmd") "python" else "r"
        val codeBuf = StringBuilder()

        fun flushMd() {
            val text = mdBuf.toString().trim()
            if (text.isNotEmpty()) {
                cells += NotebookCell(cells.size, "markdown", null, text)
            }
            mdBuf.setLength(0)
        }

        source.lines().forEach { line ->
            val trimmed = line.trim()
            if (!inCode && trimmed.startsWith("```{")) {
                flushMd()
                inCode = true
                codeLang = trimmed.removePrefix("```{").substringBefore('}').substringBefore(' ').trim()
                    .ifBlank { if (ext == "qmd") "python" else "r" }
                codeBuf.setLength(0)
            } else if (inCode && trimmed == "```") {
                cells += NotebookCell(cells.size, "code", cells.count { it.cellType == "code" } + 1, codeBuf.toString().trimEnd())
                inCode = false
                codeBuf.setLength(0)
            } else if (inCode) {
                codeBuf.appendLine(line)
            } else {
                mdBuf.appendLine(line)
            }
        }
        if (inCode && codeBuf.isNotEmpty()) {
            cells += NotebookCell(cells.size, "code", cells.count { it.cellType == "code" } + 1, codeBuf.toString().trimEnd())
        } else {
            flushMd()
        }

        return ParsedNotebook(
            kernelName = if (ext == "qmd") "Quarto Document" else "R Markdown",
            language = codeLang,
            nbFormat = ext.uppercase(),
            cells = cells
        )
    }
}
