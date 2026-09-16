package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OmniViewModel(private val repository: NoteRepository) : ViewModel() {
    private val _currentNoteId = MutableStateFlow<Long?>(null)
    val currentNoteId: StateFlow<Long?> = _currentNoteId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentNote: StateFlow<Note?> = _currentNoteId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.allNotes.map { notes -> notes.find { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentItems: StateFlow<List<NoteItem>> = _currentNoteId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getItemsForNote(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<Note>> = repository.allNotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val suggestions: StateFlow<List<Suggestion>> = repository.suggestions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.lastNoteId.collect { id ->
                if (id != null && _currentNoteId.value == null) _currentNoteId.value = id
                else if (_currentNoteId.value == null) createNewNote()
            }
        }
    }

    fun selectNote(id: Long) { _currentNoteId.value = id; viewModelScope.launch { repository.setLastNoteId(id) } }

    fun createNewNote() {
        viewModelScope.launch {
            val notes = repository.allNotes.first()
            val nextNumber = if (notes.isEmpty()) "1" else ((notes.mapNotNull { it.invoiceNumber.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()
            val id = repository.saveNote(Note(title = "فاتورة $nextNumber", invoiceNumber = nextNumber))
            selectNote(id)
        }
    }

    fun addItem(name: String, quantity: Double, price: Double, section: String) {
        val noteId = _currentNoteId.value ?: return
        viewModelScope.launch {
            repository.saveItem(NoteItem(noteId = noteId, name = name, quantity = quantity, price = price, section = section))
            repository.allNotes.first().find { it.id == noteId }?.let { note ->
                repository.saveInvoiceFile(note, repository.getItemsForNote(noteId).first())
            }
        }
    }

    fun updateCustomerName(name: String) {
        val note = currentNote.value ?: return
        viewModelScope.launch { repository.saveNote(note.copy(customerName = name)) }
    }

    fun updateInvoiceNumber(number: String) {
        val note = currentNote.value ?: return
        viewModelScope.launch { repository.saveNote(note.copy(invoiceNumber = number)) }
    }

    fun deleteItem(item: NoteItem) { viewModelScope.launch { repository.deleteItem(item) } }

    fun updateItem(item: NoteItem) {
        viewModelScope.launch {
            repository.saveItem(item)
            repository.allNotes.first().find { it.id == item.noteId }?.let { note ->
                repository.saveInvoiceFile(note, repository.getItemsForNote(item.noteId).first())
            }
        }
    }

    fun clearCurrentNote() { _currentNoteId.value?.let { id -> viewModelScope.launch { repository.clearNote(id) } } }

    fun updateNoteSettings(fontSize: Int, scrollEnabled: Boolean) {
        val note = currentNote.value ?: return
        viewModelScope.launch { repository.saveNote(note.copy(fontSize = fontSize.coerceIn(8, 32), scrollEnabled = scrollEnabled)) }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
            if (_currentNoteId.value == note.id) {
                val remaining = repository.allNotes.first()
                if (remaining.isNotEmpty()) selectNote(remaining.first().id) else createNewNote()
            }
        }
    }

    suspend fun createBackupJson(): String = repository.createFullBackup()
    suspend fun restoreBackupJson(text: String): Long? = repository.restoreFullBackup(text)
    suspend fun saveCurrentInvoiceFile(): Boolean {
        val note = currentNote.value ?: return false
        return repository.saveInvoiceFile(note, currentItems.value)
    }
}
