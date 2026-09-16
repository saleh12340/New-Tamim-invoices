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
        val text = buildString {
            append("${note.title}\n")
            if (note.invoiceNumber.isNotBlank()) append("رقم الفاتورة: ${note.invoiceNumber}\n")
            if (note.customerName.isNotBlank()) append("العميل: ${note.customerName}\n")
            append("التاريخ: $date\n")
            append("------------------------------\n")
            items.forEach { item ->
                append("${item.name}\n")
                append("الكمية: ${item.quantity}  الإجمالي: ${formatMoney(item.quantity * item.price)}\n")
            }
            append("------------------------------\n")
            append("الإجمالي: ${formatMoney(total)}\n")
        }
        return BackupManager(context).saveTextFile("New-Tamim-invoices/Invoices", BackupManager(context).invoiceFileName(note), text)
    }

    private fun formatMoney(value: Double): String =
        if (value.isFinite() && value % 1.0 == 0.0) value.toLong().toString()
        else "%.2f".format(java.util.Locale.US, value)
}
