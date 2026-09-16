package com.example.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@Serializable
data class AppBackup(
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: List<Note>,
    val items: List<NoteItem>,
    val suggestions: List<Suggestion>,
    val lastNoteId: Long? = null
)

class BackupManager(private val context: Context) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun createBackup(notes: List<Note>, items: List<NoteItem>, suggestions: List<Suggestion>, lastNoteId: Long?): String =
        json.encodeToString(AppBackup.serializer(), AppBackup(notes = notes, items = items, suggestions = suggestions, lastNoteId = lastNoteId))

    fun parseBackup(text: String): AppBackup =
        json.decodeFromString(AppBackup.serializer(), text)

    fun saveTextFile(relativeFolder: String, displayName: String, text: String): Boolean {
        return try {
            val resolver = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + relativeFolder)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
                resolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, relativeFolder)
                if (!targetDir.exists()) targetDir.mkdirs()
                val file = File(targetDir, displayName)
                FileOutputStream(file).use { it.write(text.toByteArray(Charsets.UTF_8)) }
                true
            }
        } catch (_: Exception) { false }
    }

    fun invoiceFileName(note: Note): String {
        val invNum = note.invoiceNumber.trim().ifEmpty { note.id.toString() }
        val customer = note.customerName.trim().ifEmpty { "بدون_عميل" }
            .replace(Regex("[^\\p{L}\\p{N}_-]"), "_")
        return "فاتورة_${invNum}_${customer}.txt"
    }

    fun saveOrUpdateInvoiceFile(relativeFolder: String, note: Note, text: String): Boolean {
        val invNum = note.invoiceNumber.trim().ifEmpty { note.id.toString() }
        val customer = note.customerName.trim().ifEmpty { "بدون_عميل" }
            .replace(Regex("[^\\p{L}\\p{N}_-]"), "_")
        val displayName = "فاتورة_${invNum}_${customer}.txt"
        val prefix = "فاتورة_${invNum}_"

        return try {
            val resolver = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME)
                val selection = "${MediaStore.Downloads.RELATIVE_PATH} LIKE ? AND (${MediaStore.Downloads.DISPLAY_NAME} = ? OR ${MediaStore.Downloads.DISPLAY_NAME} LIKE ?)"
                val selectionArgs = arrayOf("%$relativeFolder%", displayName, "$prefix%")

                var existingUri: Uri? = null
                var nameNeedsUpdate = false

                resolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                        val currentName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME))
                        existingUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                        if (currentName != displayName) {
                            nameNeedsUpdate = true
                        }
                    }
                }

                if (existingUri != null) {
                    if (nameNeedsUpdate) {
                        val nameValues = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                        }
                        resolver.update(existingUri!!, nameValues, null, null)
                    }
                    resolver.openOutputStream(existingUri!!, "wt")?.use { out ->
                        out.write(text.toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                    true
                } else {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                        put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + relativeFolder)
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
                    resolver.openOutputStream(uri, "wt")?.use { out ->
                        out.write(text.toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    true
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, relativeFolder)
                if (!targetDir.exists()) targetDir.mkdirs()

                // Delete any older file for this invoice number with previous customer name
                targetDir.listFiles { _, name -> name.startsWith(prefix) && name.endsWith(".txt") }?.forEach { oldFile ->
                    if (oldFile.name != displayName) {
                        oldFile.delete()
                    }
                }

                val file = File(targetDir, displayName)
                FileOutputStream(file).use { out ->
                    out.write(text.toByteArray(Charsets.UTF_8))
                    out.flush()
                }
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun shareReceipt(title: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(intent, "طباعة أو مشاركة الفاتورة")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooser)
    }
}
