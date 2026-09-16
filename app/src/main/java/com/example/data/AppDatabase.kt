package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<Note>)
    @Delete
    suspend fun deleteNote(note: Note)
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
    @Query("SELECT * FROM notes")
    suspend fun getAllNotesSnapshot(): List<Note>

    @Query("SELECT * FROM note_items WHERE noteId = :noteId ORDER BY timestamp ASC")
    fun getItemsForNote(noteId: Long): Flow<List<NoteItem>>
    @Query("SELECT * FROM note_items")
    suspend fun getAllItemsSnapshot(): List<NoteItem>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: NoteItem)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<NoteItem>)
    @Delete
    suspend fun deleteItem(item: NoteItem)
    @Query("DELETE FROM note_items WHERE noteId = :noteId")
    suspend fun clearItemsForNote(noteId: Long)
    @Query("DELETE FROM note_items")
    suspend fun deleteAllItems()

    @Query("SELECT * FROM suggestions ORDER BY count DESC LIMIT 50")
    fun getSuggestions(): Flow<List<Suggestion>>
    @Query("SELECT * FROM suggestions")
    suspend fun getAllSuggestionsSnapshot(): List<Suggestion>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestion(suggestion: Suggestion)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestions(items: List<Suggestion>)
    @Delete
    suspend fun deleteSuggestion(suggestion: Suggestion)
    @Query("DELETE FROM suggestions")
    suspend fun deleteAllSuggestions()
    @Query("SELECT * FROM suggestions WHERE word = :word")
    suspend fun getSuggestionByWord(word: String): Suggestion?
}

@Database(entities = [Note::class, NoteItem::class, Suggestion::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
