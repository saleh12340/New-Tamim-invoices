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
    val currentNote: StateFlow<Note?> = _currentNoteId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.allNotes.map { notes -> notes.find { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentItems: StateFlow<List<NoteItem>> = _currentNoteId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getItemsForNote(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suggestions: StateFlow<List<Suggestion>> = repository.suggestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.lastNoteId.collect { id ->
                if (id != null && _currentNoteId.value == null) {
                    _currentNoteId.value = id
                } else if (_currentNoteId.value == null) {
                    // Create first note if none exists
                    createNewNote()
                }
            }
        }
    }

    fun selectNote(id: Long) {
        _currentNoteId.value = id
        viewModelScope.launch { repository.setLastNoteId(id) }
    }

    fun createNewNote() {
        viewModelScope.launch {
            val id = repository.saveNote(Note(title = "فاتورة جديدة"))
            selectNote(id)
        }
    }

    fun addItem(name: String, quantity: Double, price: Double, section: String) {
        val noteId = _currentNoteId.value ?: return
        viewModelScope.launch {
            repository.saveItem(NoteItem(noteId = noteId, name = name, quantity = quantity, price = price, section = section))
        }
    }

    fun updateCustomerName(name: String) {
        val note = currentNote.value ?: return
        viewModelScope.launch {
            repository.saveNote(note.copy(customerName = name))
        }
    }

    fun updateInvoiceNumber(number: String) {
        val note = currentNote.value ?: return
        viewModelScope.launch {
            repository.saveNote(note.copy(invoiceNumber = number))
        }
    }

    fun deleteItem(item: NoteItem) {
        viewModelScope.launch { repository.deleteItem(item) }
    }

    fun updateItem(item: NoteItem) {
        viewModelScope.launch { repository.saveItem(item) }
    }

    fun clearCurrentNote() {
        val noteId = _currentNoteId.value ?: return
        viewModelScope.launch { repository.clearNote(noteId) }
    }

    fun updateNoteSettings(fontSize: Int, scrollEnabled: Boolean) {
        val note = currentNote.value ?: return
        viewModelScope.launch {
            repository.saveNote(note.copy(fontSize = fontSize, scrollEnabled = scrollEnabled))
        }
    }
    
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
            if (_currentNoteId.value == note.id) {
                val remaining = repository.allNotes.first().filter { it.id != note.id }
                if (remaining.isNotEmpty()) {
                    selectNote(remaining.first().id)
                } else {
                    createNewNote()
                }
            }
        }
    }
}
