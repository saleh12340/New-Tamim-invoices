package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
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

        // Keep the existing word-by-word suggestions, but also save the
        // complete item phrase. This makes multi-word suggestions work too.
        val normalizedPhrase = item.name.trim().replace(Regex("\\s+"), " ")
        if (normalizedPhrase.length > 1) {
            val existingPhrase = noteDao.getSuggestionByWord(normalizedPhrase)
            if (existingPhrase != null) {
                noteDao.insertSuggestion(existingPhrase.copy(count = existingPhrase.count + 1))
            } else {
                noteDao.insertSuggestion(Suggestion(word = normalizedPhrase))
            }
        }

        normalizedPhrase.split(" ").forEach { word ->
            if (word.length > 1) {
                val existing = noteDao.getSuggestionByWord(word)
                if (existing != null) {
                    noteDao.insertSuggestion(existing.copy(count = existing.count + 1))
                } else {
                    noteDao.insertSuggestion(Suggestion(word = word))
                }
            }
        }
    }

    suspend fun deleteItem(item: NoteItem) = noteDao.deleteItem(item)
    suspend fun clearNote(noteId: Long) = noteDao.clearItemsForNote(noteId)

    private val LAST_NOTE_ID = longPreferencesKey("last_note_id")
    val lastNoteId: Flow<Long?> = context.dataStore.data.map { it[LAST_NOTE_ID] }
    suspend fun setLastNoteId(id: Long) {
        context.dataStore.edit { it[LAST_NOTE_ID] = id }
    }
}
