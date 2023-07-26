package com.ljuntyg.gitnotes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

// Any notes created during application should only be handled via this ViewModel which
// will then pass any data through the NotesRepository to the DAO of the database
class NotesViewModel(private val repository: NotesRepository) : ViewModel() {

    // Use a backing property to hide the mutable LiveData from the UI
    private val _allNotes = MutableLiveData<List<Note>>()
    val allNotes: LiveData<List<Note>> get() = _allNotes

    init {
        // Automatically updates the list of notes when the database changes
        viewModelScope.launch {
            repository.allNotes.collect { notes ->
                _allNotes.value = notes
            }
        }
    }

    fun insertAsync(note: Note): Deferred<Long> {
        return viewModelScope.async {
            repository.insert(note)
        }
    }

    fun update(note: Note) {
        viewModelScope.launch {
            repository.update(note)
        }
    }

    fun delete(note: Note) {
        viewModelScope.launch {
            repository.delete(note)
        }
    }
}

// For ViewModel classes with non-empty constructor, ViewModelProvider
// requires class implementing the ViewModelProvider.Factory interface
class NotesViewModelFactory(private val repository: NotesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}