package com.example.gitnotes

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Database
import android.content.Context
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Delete
import androidx.room.Room
import androidx.room.Update
import com.example.gitnotes.databinding.RecyclerViewRowBinding
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

/**
 * The Room database in this app is only for handling Note data,
 * so the database related methods are also defined here
 */

// TODO: Figure out how to make id val without getting: "error: Cannot find setter for field."
@Parcelize
@Entity
data class Note(
    // Automatically generates a unique key (id) for each Note (for the Room database)
    @PrimaryKey(autoGenerate = true) var id: Int? = null,
    var title: String = "",
    var body: String = ""
) : Parcelable

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

    fun insert(note: Note): Deferred<Long> {
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

/**
 * RecyclerView related members
 */

class NoteListAdapter(private val navController: NavController) : RecyclerView.Adapter<NoteListAdapter.NoteViewHolder>() {

    var notes: List<Note> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged() // TODO: Change to more efficient solution, consider using ListAdapter for DiffUtil
        }

    class NoteViewHolder(private val binding: RecyclerViewRowBinding, private val navController: NavController) : RecyclerView.ViewHolder(binding.root) {
        fun bind(note: Note) {
            binding.textView.text = note.title
            binding.root.setOnClickListener {
                val action = RecyclerViewFragmentDirections.actionRecyclerViewFragmentToNoteFragment(note)
                navController.navigate(action)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = RecyclerViewRowBinding.inflate(layoutInflater, parent, false)
        return NoteViewHolder(binding, navController)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val currentNote = notes[position]
        holder.bind(currentNote)
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
    fun insert(note: Note): Long

    @Update
    fun update(note: Note)

    @Delete
    fun delete(note: Note)
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

// WARNING: Might get "Cannot access database on the main thread...", use CoroutineScope to avoid
class NotesRepository(private val notesDao: NotesDao) {

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    val allNotes: Flow<List<Note>> = notesDao.getAllNotes()

    suspend fun insert(note: Note): Long {
        return withContext(Dispatchers.IO) {
            notesDao.insert(note)
        }
    }

    suspend fun update(note: Note) {
        withContext(Dispatchers.IO) {
            notesDao.update(note)
        }
    }

    suspend fun delete(note: Note) {
        withContext(Dispatchers.IO) {
            notesDao.delete(note)
        }
    }
}
