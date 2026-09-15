package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "notes")
@Serializable
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val customerName: String = "",
    val invoiceNumber: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val fontSize: Int = 14,
    val scrollEnabled: Boolean = true
)

@Entity(tableName = "note_items")
@Serializable
data class NoteItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val name: String,
    val quantity: Double,
    val price: Double = 0.0,
    val section: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "suggestions")
@Serializable
data class Suggestion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val count: Int = 1
)
