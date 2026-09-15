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

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM note_items WHERE noteId = :noteId ORDER BY timestamp ASC")
    fun getItemsForNote(noteId: Long): Flow<List<NoteItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: NoteItem)

    @Delete
    suspend fun deleteItem(item: NoteItem)
    
    @Query("DELETE FROM note_items WHERE noteId = :noteId")
    suspend fun clearItemsForNote(noteId: Long)

    @Query("SELECT * FROM suggestions ORDER BY count DESC LIMIT 50")
    fun getSuggestions(): Flow<List<Suggestion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestion(suggestion: Suggestion)

    @Query("SELECT * FROM suggestions WHERE word = :word")
    suspend fun getSuggestionByWord(word: String): Suggestion?
}

@Database(entities = [Note::class, NoteItem::class, Suggestion::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
