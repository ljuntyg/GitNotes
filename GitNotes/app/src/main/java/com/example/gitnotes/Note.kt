package com.example.gitnotes

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Database
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * The Room database in this app is only for handling Note data,
 * so the database related methods are also defined here
 */

@Entity
data class Note(
    // Automatically generates a unique key (id) for each Note (for the Room database)
    @PrimaryKey(autoGenerate = true) val id: Int?,
    val title: String,
    val body: String
)

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

    fun insert(note: Note) = viewModelScope.launch {
        repository.insert(note)
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

/**
 * RecyclerView related members
 */

class NoteListAdapter(notes: List<Note>) : RecyclerView.Adapter<NoteListAdapter.NoteViewHolder>() {

    var notes: List<Note> = notes
        set(value) {
            field = value
            notifyDataSetChanged() // TODO: Change to more efficient solution
        }

    class NoteViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val noteTextView: TextView = view.findViewById(R.id.text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_row, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.noteTextView.text = notes[position].title
    }

    override fun getItemCount() = notes.size
}

/**
 * Room database related members
 */

@Dao
interface NotesDao {
    @Query("SELECT * FROM Note")
    fun getAllNotes(): Flow<List<Note>>

    @Insert
    fun insert(note: Note)
}

// Uses singleton pattern so database can be created when needed and else be accessed when needed
// from anywhere, e.g. in  the fragment to display the RecyclerView containing all the Note objects
@Database(entities = [Note::class], version = 1)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao

    companion object {
        private var INSTANCE: NotesDatabase? = null
        fun getDatabase(context: Context): NotesDatabase {
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE = Room.databaseBuilder(context,
                        NotesDatabase::class.java, "notes_database").build()
                }
            }
            return INSTANCE!!
        }
    }
}

// Provides a layer between the DAO for the database and anything trying to access
// that data, e.g. NotesViewModel which maintains all the notes during app run
class NotesRepository(private val notesDao: NotesDao) {

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    val allNotes: Flow<List<Note>> = notesDao.getAllNotes()

    fun insert(note: Note) {
        CoroutineScope(Dispatchers.IO).launch {
            notesDao.insert(note)
        }
    }
}
