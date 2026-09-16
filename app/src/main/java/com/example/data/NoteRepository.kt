package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class NoteRepository(private val noteDao: NoteDao, private val context: Context) {
    val allNotes = noteDao.getAllNotes()
    val suggestions = noteDao.getSuggestions()
    fun getItemsForNote(noteId: Long) = noteDao.getItemsForNote(noteId)

    suspend fun saveNote(note: Note) = noteDao.insertNote(note)
    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    suspend fun saveItem(item: NoteItem) {
        noteDao.insertItem(item)
        val normalizedPhrase = item.name.trim().replace(Regex("\\s+"), " ")
        if (normalizedPhrase.length > 1) {
            val existingPhrase = noteDao.getSuggestionByWord(normalizedPhrase)
            if (existingPhrase != null) noteDao.insertSuggestion(existingPhrase.copy(count = existingPhrase.count + 1))
            else noteDao.insertSuggestion(Suggestion(word = normalizedPhrase))
        }
        normalizedPhrase.split(" ").forEach { word ->
            if (word.length > 1) {
                val existing = noteDao.getSuggestionByWord(word)
                if (existing != null) noteDao.insertSuggestion(existing.copy(count = existing.count + 1))
                else noteDao.insertSuggestion(Suggestion(word = word))
            }
        }
    }

    suspend fun deleteItem(item: NoteItem) = noteDao.deleteItem(item)
    suspend fun clearNote(noteId: Long) = noteDao.clearItemsForNote(noteId)

    private val LAST_NOTE_ID = longPreferencesKey("last_note_id")
    val lastNoteId: Flow<Long?> = context.dataStore.data.map { it[LAST_NOTE_ID] }
    suspend fun setLastNoteId(id: Long) { context.dataStore.edit { it[LAST_NOTE_ID] = id } }

    suspend fun createFullBackup(): String {
        val manager = BackupManager(context)
        return manager.createBackup(
            noteDao.getAllNotesSnapshot(),
            noteDao.getAllItemsSnapshot(),
            noteDao.getAllSuggestionsSnapshot(),
            lastNoteId.first()
        )
    }

    suspend fun restoreFullBackup(text: String): Long? {
        val backup = BackupManager(context).parseBackup(text)
        noteDao.deleteAllItems()
        noteDao.deleteAllNotes()
        noteDao.deleteAllSuggestions()
        if (backup.notes.isNotEmpty()) noteDao.insertNotes(backup.notes)
        if (backup.items.isNotEmpty()) noteDao.insertItems(backup.items)
        if (backup.suggestions.isNotEmpty()) noteDao.insertSuggestions(backup.suggestions)
        backup.lastNoteId?.let { setLastNoteId(it) }
        return backup.lastNoteId
    }

    suspend fun saveInvoiceFile(note: Note, items: List<NoteItem>): Boolean {
        val total = items.sumOf { it.quantity * it.price }
        val date = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(note.timestamp))
        val invNum = note.invoiceNumber.trim().ifEmpty { note.id.toString() }
        val customer = note.customerName.trim().ifEmpty { "عميل عام" }

        val text = buildString {
            appendLine("================================")
            appendLine("         ${note.title.ifEmpty { "فاتورة مبيعات" }}")
            appendLine("================================")
            appendLine("رقم الفاتورة: $invNum")
            appendLine("العميل     : $customer")
            appendLine("التاريخ    : $date")
            appendLine("--------------------------------")
            appendLine(String.format(java.util.Locale.US, "%-14s %4s %5s %6s", "الصنف", "الكمية", "السعر", "الإجمالي"))
            appendLine("--------------------------------")
            for (item in items) {
                val itemTotal = item.quantity * item.price
                val nameShort = if (item.name.length > 14) item.name.take(13) + "." else item.name
                appendLine(
                    String.format(
                        java.util.Locale.US,
                        "%-14s %4s %5s %6s",
                        nameShort,
                        formatMoney(item.quantity),
                        formatMoney(item.price),
                        formatMoney(itemTotal)
                    )
                )
            }
            appendLine("--------------------------------")
            appendLine("المجموع الكلي: ${formatMoney(total)}")
            appendLine("عدد الأصناف  : ${items.size}")
            appendLine("================================")
            appendLine("       شكراً لتعاملكم معنا      ")
            appendLine("================================")
        }
        return BackupManager(context).saveOrUpdateInvoiceFile("New-Tamim-invoices/Invoices", note, text)
    }

    fun shareInvoiceReceipt(note: Note, items: List<NoteItem>) {
        val total = items.sumOf { it.quantity * it.price }
        val date = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(note.timestamp))
        val invNum = note.invoiceNumber.trim().ifEmpty { note.id.toString() }
        val customer = note.customerName.trim().ifEmpty { "عميل عام" }
        val title = "فاتورة_${invNum}_${customer}"
        val text = buildString {
            appendLine("================================")
            appendLine("         ${note.title.ifEmpty { "فاتورة مبيعات" }}")
            appendLine("================================")
            appendLine("رقم الفاتورة: $invNum")
            appendLine("العميل     : $customer")
            appendLine("التاريخ    : $date")
            appendLine("--------------------------------")
            appendLine(String.format(java.util.Locale.US, "%-14s %4s %5s %6s", "الصنف", "الكمية", "السعر", "الإجمالي"))
            appendLine("--------------------------------")
            for (item in items) {
                val itemTotal = item.quantity * item.price
                val nameShort = if (item.name.length > 14) item.name.take(13) + "." else item.name
                appendLine(
                    String.format(
                        java.util.Locale.US,
                        "%-14s %4s %5s %6s",
                        nameShort,
                        formatMoney(item.quantity),
                        formatMoney(item.price),
                        formatMoney(itemTotal)
                    )
                )
            }
            appendLine("--------------------------------")
            appendLine("المجموع الكلي: ${formatMoney(total)}")
            appendLine("عدد الأصناف  : ${items.size}")
            appendLine("================================")
            appendLine("       شكراً لتعاملكم معنا      ")
            appendLine("================================")
        }
        BackupManager(context).shareReceipt(title, text)
    }

    private fun formatMoney(value: Double): String =
        if (value.isFinite() && value % 1.0 == 0.0) value.toLong().toString()
        else "%.2f".format(java.util.Locale.US, value)
}
