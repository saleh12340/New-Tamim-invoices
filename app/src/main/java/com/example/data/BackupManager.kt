package com.example.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
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
        json.encodeToString(AppBackup(notes = notes, items = items, suggestions = suggestions, lastNoteId = lastNoteId))

    fun parseBackup(text: String): AppBackup = json.decodeFromString(text)

    fun saveTextFile(relativeFolder: String, displayName: String, text: String): Boolean {
        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + relativeFolder)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0); resolver.update(uri, values, null, null)
            }
            true
        } catch (_: Exception) { false }
    }

    fun invoiceFileName(note: Note): String {
        val customer = note.customerName.trim().ifEmpty { "بدون_عميل" }
            .replace(Regex("[^\\p{L}\\p{N}_-]"), "_")
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "${customer}_فاتورة_${note.invoiceNumber.ifEmpty { "غير_مرقمة" }}_$time.txt"
    }
}
