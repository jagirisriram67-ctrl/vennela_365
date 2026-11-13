package com.vennela365.viewer.parser

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class DbColumnSchema(
    val name: String,
    val type: String,
    val isPrimaryKey: Boolean,
    val notNull: Boolean
)

data class DbTableMetadata(
    val name: String,
    val type: String, // "table" or "view"
    val rowCount: Long = 0L,
    val columns: List<DbColumnSchema> = emptyList()
)

data class DbInspectionResult(
    val databaseName: String,
    val fileSizeFormatted: String,
    val tables: List<DbTableMetadata> = emptyList(),
    val error: String? = null
)

data class DbTableContent(
    val tableName: String,
    val columns: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
    val totalRowCount: Long = 0L,
    val error: String? = null
)

object DatabaseParser {

    private const val MAX_COPY_BYTES = 40 * 1024 * 1024 // 40 MB max copy buffer for budget phones

    suspend fun inspectDatabase(context: Context, uri: Uri?): DbInspectionResult = withContext(Dispatchers.IO) {
        if (uri == null) {
            return@withContext DbInspectionResult(
                databaseName = "Unknown",
                fileSizeFormatted = "0 B",
                error = "No database file selected"
            )
        }

        val tempDbFile = File(context.cacheDir, "vennela_inspect.sqlite")
        try {
            // Copy URI stream to private read-only cache file
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempDbFile).use { output ->
                    val buffer = ByteArray(32768)
                    var bytesRead = input.read(buffer)
                    var totalWritten = 0
                    while (bytesRead != -1 && totalWritten < MAX_COPY_BYTES) {
                        output.write(buffer, 0, bytesRead)
                        totalWritten += bytesRead
                        bytesRead = input.read(buffer)
                    }
                }
            } ?: return@withContext DbInspectionResult(
                databaseName = uri.lastPathSegment ?: "database",
                fileSizeFormatted = "0 B",
                error = "Cannot open file stream"
            )

            val fileSize = tempDbFile.length()
            val sizeFormatted = "${(fileSize / 1024).coerceAtLeast(1)} KB"

            var db: SQLiteDatabase? = null
            try {
                db = SQLiteDatabase.openDatabase(
                    tempDbFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
                )

                val tables = mutableListOf<DbTableMetadata>()
                val masterCursor: Cursor = db.rawQuery(
                    "SELECT name, type FROM sqlite_master WHERE type IN ('table', 'view') AND name NOT LIKE 'sqlite_%' ORDER BY name ASC",
                    null
                )

                masterCursor.use { cursor ->
                    val nameIdx = cursor.getColumnIndex("name")
                    val typeIdx = cursor.getColumnIndex("type")
                    while (cursor.moveToNext()) {
                        val tableName = cursor.getString(nameIdx)
                        val tableType = cursor.getString(typeIdx)

                        // Read column info
                        val columns = mutableListOf<DbColumnSchema>()
                        try {
                            db.rawQuery("PRAGMA table_info(\"$tableName\")", null).use { pragmaCursor ->
                                val colNameIdx = pragmaCursor.getColumnIndex("name")
                                val colTypeIdx = pragmaCursor.getColumnIndex("type")
                                val pkIdx = pragmaCursor.getColumnIndex("pk")
                                val notNullIdx = pragmaCursor.getColumnIndex("notnull")
                                while (pragmaCursor.moveToNext()) {
                                    columns.add(
                                        DbColumnSchema(
                                            name = pragmaCursor.getString(colNameIdx),
                                            type = pragmaCursor.getString(colTypeIdx).ifBlank { "TEXT" },
                                            isPrimaryKey = pragmaCursor.getInt(pkIdx) > 0,
                                            notNull = pragmaCursor.getInt(notNullIdx) > 0
                                        )
                                    )
                                }
                            }
                        } catch (_: Exception) {}

                        // Read row count
                        var rowCount = 0L
                        try {
                            db.rawQuery("SELECT COUNT(*) FROM \"$tableName\"", null).use { countCursor ->
                                if (countCursor.moveToFirst()) {
                                    rowCount = countCursor.getLong(0)
                                }
                            }
                        } catch (_: Exception) {}

                        tables.add(
                            DbTableMetadata(
                                name = tableName,
                                type = tableType,
                                rowCount = rowCount,
                                columns = columns
                            )
                        )
                    }
                }

                DbInspectionResult(
                    databaseName = uri.lastPathSegment ?: "database.sqlite",
                    fileSizeFormatted = sizeFormatted,
                    tables = tables
                )
            } finally {
                runCatching { db?.close() }
            }
        } catch (e: Exception) {
            DbInspectionResult(
                databaseName = uri.lastPathSegment ?: "database",
                fileSizeFormatted = "Unknown",
                error = e.message ?: "Not a valid SQLite database or file is encrypted"
            )
        }
    }

    suspend fun queryTableRows(
        context: Context,
        uri: Uri?,
        tableName: String,
        limit: Int = 50
    ): DbTableContent = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext DbTableContent(tableName, error = "No database URI")
        val tempDbFile = File(context.cacheDir, "vennela_inspect.sqlite")
        if (!tempDbFile.exists()) {
            return@withContext DbTableContent(tableName, error = "Database cache not initialized")
        }

        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(
                tempDbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
            )

            val safeLimit = limit.coerceIn(1, 200)
            val cursor = db.rawQuery("SELECT * FROM \"$tableName\" LIMIT $safeLimit", null)
            cursor.use { c ->
                val colNames = c.columnNames.toList()
                val rows = mutableListOf<List<String>>()
                while (c.moveToNext()) {
                    val row = mutableListOf<String>()
                    for (i in 0 until c.columnCount) {
                        val value = when (c.getType(i)) {
                            Cursor.FIELD_TYPE_NULL -> "NULL"
                            Cursor.FIELD_TYPE_INTEGER -> c.getLong(i).toString()
                            Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i).toString()
                            Cursor.FIELD_TYPE_STRING -> c.getString(i) ?: ""
                            Cursor.FIELD_TYPE_BLOB -> "<BLOB ${c.getBlob(i)?.size ?: 0} B>"
                            else -> c.getString(i) ?: ""
                        }
                        row.add(value)
                    }
                    rows.add(row)
                }

                var totalCount = rows.size.toLong()
                runCatching {
                    db.rawQuery("SELECT COUNT(*) FROM \"$tableName\"", null).use { countCursor ->
                        if (countCursor.moveToFirst()) totalCount = countCursor.getLong(0)
                    }
                }

                DbTableContent(
                    tableName = tableName,
                    columns = colNames,
                    rows = rows,
                    totalRowCount = totalCount
                )
            }
        } catch (e: Exception) {
            DbTableContent(tableName = tableName, error = e.message ?: "Failed to query table")
        } finally {
            runCatching { db?.close() }
        }
    }
}

