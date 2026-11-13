package com.vennela365.viewer.parser

import android.content.Context
import android.net.Uri
import android.util.JsonReader
import android.util.JsonToken
import android.util.Xml
import com.vennela365.viewer.model.SpreadsheetCell
import com.vennela365.viewer.model.SpreadsheetSheet
import com.vennela365.viewer.storage.VennelaCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

data class ChartBarData(val label: String, val value: Float, val amountText: String)
data class ChartPieSlice(val label: String, val percentage: Int, val colorHex: Long)

object SpreadsheetParser {

    private const val MAX_DISPLAY_ROWS = 1000
    private const val MAX_SHARED_STRINGS = 10_000

    private fun createSafeXmlPullParser(): XmlPullParser {
        val parser = Xml.newPullParser()
        runCatching {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        }
        return parser
    }

    suspend fun parseSpreadsheet(
        context: Context,
        uri: Uri?,
        extension: String = "csv"
    ): List<SpreadsheetSheet> = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext emptyList()

        val cacheKey = "sheet_${uri}_${extension}"
        VennelaCacheManager.getCachedSheets(cacheKey)?.let { return@withContext it }

        val ext = extension.lowercase().removePrefix(".")
        val result = try {
            when (ext) {
                "xlsx", "xlsm", "xltx", "xltm" -> parseXlsx(context, uri)
                "ods", "ots", "fods" -> parseOds(context, uri)
                "json", "jsonl", "ndjson" -> parseJsonTableStreaming(context, uri)
                else -> parseDelimitedText(context, uri)
            }
        } catch (_: Exception) {
            parseDelimitedText(context, uri)
        }

        // Never spoof real files with fake data on failure/empty
        VennelaCacheManager.putCachedSheets(cacheKey, result)
        result
    }

    // ================= REAL NATIVE XLSX STREAM PARSER =================
    private fun parseXlsx(context: Context, uri: Uri): List<SpreadsheetSheet> {
        val sharedStrings = mutableListOf<String>()
        val sheetMap = mutableMapOf<String, String>()
        var firstSheetData: Pair<List<String>, List<List<SpreadsheetCell>>>? = null

        // Pass 1: Extract sharedStrings & discover sheet names from workbook
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name.equals("xl/sharedStrings.xml", ignoreCase = true) -> {
                            sharedStrings.addAll(parseSharedStrings(zis))
                        }
                        name.equals("xl/workbook.xml", ignoreCase = true) -> {
                            sheetMap.putAll(parseWorkbook(zis))
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        // Pass 2: Parse primary worksheet
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name.startsWith("xl/worksheets/sheet", ignoreCase = true) && name.endsWith(".xml", ignoreCase = true)) {
                        firstSheetData = parseWorksheetXml(zis, sharedStrings)
                        break
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        val data = firstSheetData ?: return emptyList()
        val sheetTitle = sheetMap.values.firstOrNull() ?: "Sheet1"

        val mainSheet = SpreadsheetSheet(sheetTitle, data.first, data.second)
        val summarySheet = SpreadsheetSheet(
            "Overview",
            listOf("Property", "Value"),
            listOf(
                listOf(SpreadsheetCell("Sheet Title"), SpreadsheetCell(sheetTitle)),
                listOf(SpreadsheetCell("Rows Parsed"), SpreadsheetCell(data.second.size.toString())),
                listOf(SpreadsheetCell("Columns"), SpreadsheetCell(data.first.size.toString())),
                listOf(SpreadsheetCell("Engine"), SpreadsheetCell("Native OpenXML Stream (Memory Protected)"))
            )
        )

        return listOf(mainSheet, summarySheet)
    }

    private fun parseSharedStrings(stream: InputStream): List<String> {
        val strings = mutableListOf<String>()
        val parser = createSafeXmlPullParser()
        parser.setInput(stream, "UTF-8")
        var eventType = parser.eventType
        val currentText = StringBuilder()
        var insideT = false

        while (eventType != XmlPullParser.END_DOCUMENT && strings.size < MAX_SHARED_STRINGS) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name.equals("t", ignoreCase = true)) {
                        insideT = true
                        currentText.clear()
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideT) currentText.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("t", ignoreCase = true)) {
                        insideT = false
                        strings.add(currentText.toString())
                    }
                }
            }
            eventType = parser.next()
        }
        return strings
    }

    private fun parseWorkbook(stream: InputStream): Map<String, String> {
        val sheets = mutableMapOf<String, String>()
        val parser = createSafeXmlPullParser()
        parser.setInput(stream, "UTF-8")
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name.equals("sheet", ignoreCase = true)) {
                val name = parser.getAttributeValue(null, "name") ?: "Sheet"
                val id = parser.getAttributeValue(null, "sheetId") ?: sheets.size.toString()
                sheets[id] = name
            }
            eventType = parser.next()
        }
        return sheets
    }

    private fun parseWorksheetXml(
        stream: InputStream,
        sharedStrings: List<String>
    ): Pair<List<String>, List<List<SpreadsheetCell>>> {
        val parser = createSafeXmlPullParser()
        parser.setInput(stream, "UTF-8")
        var eventType = parser.eventType

        val allRows = mutableListOf<List<SpreadsheetCell>>()
        var currentRow = mutableListOf<SpreadsheetCell>()
        var cellType = ""
        var cellValue = StringBuilder()
        var insideV = false
        var insideIs = false

        while (eventType != XmlPullParser.END_DOCUMENT && allRows.size < MAX_DISPLAY_ROWS) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.lowercase()) {
                        "row" -> currentRow = mutableListOf()
                        "c" -> {
                            cellType = parser.getAttributeValue(null, "t").orEmpty()
                            cellValue.clear()
                        }
                        "v" -> insideV = true
                        "t" -> if (insideIs) insideV = true
                        "is" -> insideIs = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideV) cellValue.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name.lowercase()) {
                        "v" -> insideV = false
                        "is" -> insideIs = false
                        "c" -> {
                            val raw = cellValue.toString().trim()
                            val formatted = when (cellType) {
                                "s" -> {
                                    val idx = raw.toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else raw
                                }
                                "b" -> if (raw == "1") "TRUE" else "FALSE"
                                else -> raw
                            }
                            currentRow.add(SpreadsheetCell(formatted))
                        }
                        "row" -> {
                            if (currentRow.isNotEmpty()) allRows.add(currentRow)
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        if (allRows.isEmpty()) return Pair(emptyList(), emptyList())
        val headers = allRows.first().map { it.value }
        val dataRows = allRows.drop(1)
        return Pair(headers, dataRows)
    }

    // ================= REAL ODS OPEN-DOCUMENT PARSER =================
    private fun parseOds(context: Context, uri: Uri): List<SpreadsheetSheet> {
        val allRows = mutableListOf<List<SpreadsheetCell>>()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name.equals("content.xml", ignoreCase = true)) {
                        val parser = createSafeXmlPullParser()
                        parser.setInput(zis, "UTF-8")
                        var eventType = parser.eventType
                        var currentRow = mutableListOf<SpreadsheetCell>()
                        val cellText = StringBuilder()
                        var insideP = false

                        while (eventType != XmlPullParser.END_DOCUMENT && allRows.size < MAX_DISPLAY_ROWS) {
                            when (eventType) {
                                XmlPullParser.START_TAG -> {
                                    val tag = parser.name.substringAfterLast(':')
                                    if (tag.equals("table-row", ignoreCase = true)) currentRow = mutableListOf()
                                    if (tag.equals("p", ignoreCase = true)) {
                                        insideP = true
                                        cellText.clear()
                                    }
                                }
                                XmlPullParser.TEXT -> if (insideP) cellText.append(parser.text)
                                XmlPullParser.END_TAG -> {
                                    val tag = parser.name.substringAfterLast(':')
                                    if (tag.equals("p", ignoreCase = true)) insideP = false
                                    if (tag.equals("table-cell", ignoreCase = true)) {
                                        currentRow.add(SpreadsheetCell(cellText.toString().trim()))
                                        cellText.clear()
                                    }
                                    if (tag.equals("table-row", ignoreCase = true) && currentRow.isNotEmpty()) {
                                        allRows.add(currentRow)
                                    }
                                }
                            }
                            eventType = parser.next()
                        }
                        break
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        if (allRows.isEmpty()) return emptyList()
        val headers = allRows.first().map { it.value }
        val dataRows = allRows.drop(1)
        return listOf(SpreadsheetSheet("Sheet1", headers, dataRows))
    }

    // ================= REAL CSV / TSV / DSV PARSER =================
    private fun parseDelimitedText(context: Context, uri: Uri): List<SpreadsheetSheet> {
        val lines = context.contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { seq ->
            seq.take(MAX_DISPLAY_ROWS).toList()
        } ?: emptyList()

        if (lines.isEmpty()) return emptyList()

        val delimiter = detectDelimiter(lines.first())
        val headers = parseCsvLine(lines.first(), delimiter)
        val rows = lines.drop(1)
            .filter { it.isNotBlank() }
            .map { line -> parseCsvLine(line, delimiter).map { SpreadsheetCell(it) } }

        return listOf(
            SpreadsheetSheet("Data", headers, rows),
            SpreadsheetSheet(
                "Summary",
                listOf("Metric", "Value"),
                listOf(
                    listOf(SpreadsheetCell("Total Rows"), SpreadsheetCell(rows.size.toString())),
                    listOf(SpreadsheetCell("Columns"), SpreadsheetCell(headers.size.toString())),
                    listOf(SpreadsheetCell("Detected Delimiter"), SpreadsheetCell(
                        when (delimiter) {
                            '\t' -> "Tab (\\t)"
                            ';' -> "Semicolon (;)"
                            '|' -> "Pipe (|)"
                            else -> "Comma (,)"
                        }
                    ))
                )
            )
        )
    }

    private fun detectDelimiter(firstLine: String): Char {
        val candidates = listOf(',', '\t', ';', '|')
        return candidates.maxByOrNull { firstLine.count { ch -> ch == it } } ?: ','
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val cells = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    cell.append('"')
                    i++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == delimiter && !inQuotes -> {
                    cells.add(cell.toString().trim())
                    cell.clear()
                }
                else -> cell.append(ch)
            }
            i++
        }
        cells.add(cell.toString().trim())
        return cells
    }

    // ================= STREAMING JSON TABULAR PARSER =================
    private fun parseJsonTableStreaming(context: Context, uri: Uri): List<SpreadsheetSheet> {
        val stream = context.contentResolver.openInputStream(uri) ?: return emptyList()
        val headers = linkedSetOf<String>()
        val rowMaps = mutableListOf<Map<String, String>>()

        try {
            JsonReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                    reader.beginArray()
                    var rowCount = 0
                    while (reader.hasNext() && rowCount < MAX_DISPLAY_ROWS) {
                        if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                            reader.beginObject()
                            val rowMap = mutableMapOf<String, String>()
                            while (reader.hasNext()) {
                                val key = reader.nextName()
                                val value = when (reader.peek()) {
                                    JsonToken.STRING -> reader.nextString()
                                    JsonToken.NUMBER -> reader.nextString()
                                    JsonToken.BOOLEAN -> reader.nextBoolean().toString()
                                    JsonToken.NULL -> { reader.nextNull(); "" }
                                    else -> { reader.skipValue(); "" }
                                }
                                headers.add(key)
                                rowMap[key] = value
                            }
                            reader.endObject()
                            rowMaps.add(rowMap)
                            rowCount++
                        } else {
                            reader.skipValue()
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        if (headers.isEmpty()) return emptyList()

        val headerList = headers.toList()
        val rows = rowMaps.map { map ->
            headerList.map { key -> SpreadsheetCell(map[key] ?: "") }
        }

        return listOf(SpreadsheetSheet("JSON Table", headerList, rows))
    }

    // Sample data strictly reserved for preview/empty fallback in gallery
    fun getSampleSpreadsheets(): List<SpreadsheetSheet> {
        val headers = listOf("Month", "Sales (₹)", "Units Sold", "Growth %", "Category", "Status")
        val dataRows = listOf(
            listOf(SpreadsheetCell("Jan"), SpreadsheetCell("1,24,500"), SpreadsheetCell("320"), SpreadsheetCell("12.5%"), SpreadsheetCell("Electronics"), SpreadsheetCell("High", tagColor = "green")),
            listOf(SpreadsheetCell("Feb"), SpreadsheetCell("98,400"), SpreadsheetCell("280"), SpreadsheetCell("-8.2%"), SpreadsheetCell("Books"), SpreadsheetCell("Medium", tagColor = "amber")),
            listOf(SpreadsheetCell("Mar"), SpreadsheetCell("1,56,200"), SpreadsheetCell("420"), SpreadsheetCell("18.7%"), SpreadsheetCell("Accessories"), SpreadsheetCell("High", tagColor = "green")),
            listOf(SpreadsheetCell("Apr"), SpreadsheetCell("1,12,300"), SpreadsheetCell("310"), SpreadsheetCell("-5.1%"), SpreadsheetCell("Stationery"), SpreadsheetCell("Low", tagColor = "red")),
            listOf(SpreadsheetCell("May"), SpreadsheetCell("1,89,400"), SpreadsheetCell("510"), SpreadsheetCell("22.3%"), SpreadsheetCell("Electronics"), SpreadsheetCell("High", tagColor = "green")),
            listOf(SpreadsheetCell("Jun"), SpreadsheetCell("1,45,600"), SpreadsheetCell("430"), SpreadsheetCell("11.4%"), SpreadsheetCell("Books"), SpreadsheetCell("Medium", tagColor = "amber")),
            listOf(SpreadsheetCell("Jul"), SpreadsheetCell("1,78,900"), SpreadsheetCell("480"), SpreadsheetCell("15.6%"), SpreadsheetCell("Accessories"), SpreadsheetCell("High", tagColor = "green")),
            listOf(SpreadsheetCell("Aug"), SpreadsheetCell("1,32,400"), SpreadsheetCell("390"), SpreadsheetCell("-4.3%"), SpreadsheetCell("Stationery"), SpreadsheetCell("Medium", tagColor = "amber")),
            listOf(SpreadsheetCell("Sep"), SpreadsheetCell("1,67,300"), SpreadsheetCell("460"), SpreadsheetCell("12.9%"), SpreadsheetCell("Electronics"), SpreadsheetCell("High", tagColor = "green")),
            listOf(SpreadsheetCell("Oct"), SpreadsheetCell("1,95,800"), SpreadsheetCell("520"), SpreadsheetCell("16.8%"), SpreadsheetCell("Books"), SpreadsheetCell("High", tagColor = "green")),
            listOf(SpreadsheetCell("Nov"), SpreadsheetCell("1,48,200"), SpreadsheetCell("410"), SpreadsheetCell("-7.2%"), SpreadsheetCell("Accessories"), SpreadsheetCell("Medium", tagColor = "amber")),
            listOf(SpreadsheetCell("Dec"), SpreadsheetCell("2,10,000"), SpreadsheetCell("580"), SpreadsheetCell("19.5%"), SpreadsheetCell("Electronics"), SpreadsheetCell("High", tagColor = "green"))
        )

        val chartHeaders = listOf("Category", "Market Share", "Growth Target")
        val chartRows = listOf(
            listOf(SpreadsheetCell("Electronics"), SpreadsheetCell("38%"), SpreadsheetCell("₹ 10.5L")),
            listOf(SpreadsheetCell("Books"), SpreadsheetCell("24%"), SpreadsheetCell("₹ 6.2L")),
            listOf(SpreadsheetCell("Accessories"), SpreadsheetCell("22%"), SpreadsheetCell("₹ 5.8L")),
            listOf(SpreadsheetCell("Stationery"), SpreadsheetCell("16%"), SpreadsheetCell("₹ 3.9L"))
        )

        val summaryHeaders = listOf("Summary Metric", "Value")
        val summaryRows = listOf(
            listOf(SpreadsheetCell("Total Sales"), SpreadsheetCell("₹ 18,60,800")),
            listOf(SpreadsheetCell("Total Units"), SpreadsheetCell("4,890")),
            listOf(SpreadsheetCell("Overall Growth"), SpreadsheetCell("12.4%"))
        )

        return listOf(
            SpreadsheetSheet("Sheet1", headers, dataRows),
            SpreadsheetSheet("Sheet2", chartHeaders, chartRows),
            SpreadsheetSheet("Sheet3", summaryHeaders, summaryRows)
        )
    }

    fun cleanNumeric(raw: String): Double? {
        val cleaned = raw.replace("₹", "").replace("$", "").replace("€", "")
            .replace("£", "").replace(",", "").replace("%", "").trim()
        return cleaned.toDoubleOrNull()
    }

    /**
     * Dynamically derives bar chart items and pie distribution slices from
     * real spreadsheet columns instead of static sample lists.
     */
    fun deriveChartData(sheet: SpreadsheetSheet?): Pair<List<ChartBarData>, List<ChartPieSlice>> {
        if (sheet == null || sheet.rows.isEmpty()) {
            return Pair(emptyList(), emptyList())
        }

        // Search for the primary numeric column
        var numericColIdx = -1
        for (colIdx in 0 until sheet.headers.size) {
            val sampleCount = sheet.rows.take(15).count { row ->
                val cell = row.getOrNull(colIdx)?.value ?: return@count false
                cleanNumeric(cell) != null
            }
            if (sampleCount >= 2) {
                numericColIdx = colIdx
                break
            }
        }

        if (numericColIdx == -1) {
            return Pair(emptyList(), emptyList())
        }

        val labelColIdx = if (numericColIdx == 0) 1.coerceAtMost(sheet.headers.size - 1) else 0

        val extracted = sheet.rows.take(12).mapNotNull { row ->
            val label = row.getOrNull(labelColIdx)?.value ?: "Item"
            val rawVal = row.getOrNull(numericColIdx)?.value ?: return@mapNotNull null
            val num = cleanNumeric(rawVal) ?: return@mapNotNull null
            Triple(label, num, rawVal)
        }

        if (extracted.isEmpty()) return Pair(emptyList(), emptyList())

        val maxVal = extracted.maxOf { it.second }.coerceAtLeast(1.0)
        val barData = extracted.map { (label, num, rawVal) ->
            ChartBarData(
                label = label.take(8),
                value = (num / maxVal).toFloat().coerceIn(0.06f, 1f),
                amountText = rawVal
            )
        }

        val totalSum = extracted.sumOf { it.second }.coerceAtLeast(1.0)
        val colors = listOf(0xFF38BDF8, 0xFF10B981, 0xFFF59E0B, 0xFF8B5CF6, 0xFFEC4899, 0xFF06B6D4)
        val pieSlices = extracted.take(6).mapIndexed { idx, (label, num, _) ->
            val pct = Math.round((num / totalSum) * 100).toInt()
            ChartPieSlice(label = label, percentage = pct, colorHex = colors[idx % colors.size])
        }

        return Pair(barData, pieSlices)
    }

    val sampleSalesTrend = listOf(
        ChartBarData("Jan", 0.55f, "₹1.24L"),
        ChartBarData("Feb", 0.40f, "₹0.98L"),
        ChartBarData("Mar", 0.70f, "₹1.56L"),
        ChartBarData("Apr", 0.48f, "₹1.12L"),
        ChartBarData("May", 0.88f, "₹1.89L"),
        ChartBarData("Jun", 0.62f, "₹1.45L"),
        ChartBarData("Jul", 0.78f, "₹1.78L"),
        ChartBarData("Aug", 0.58f, "₹1.32L"),
        ChartBarData("Sep", 0.74f, "₹1.67L"),
        ChartBarData("Oct", 0.90f, "₹1.95L"),
        ChartBarData("Nov", 0.64f, "₹1.48L"),
        ChartBarData("Dec", 0.98f, "₹2.10L")
    )

    val sampleCategoryDistribution = listOf(
        ChartPieSlice("Electronics", 38, 0xFF38BDF8),
        ChartPieSlice("Books", 24, 0xFF10B981),
        ChartPieSlice("Accessories", 22, 0xFFF59E0B),
        ChartPieSlice("Stationery", 16, 0xFF8B5CF6)
    )
}
