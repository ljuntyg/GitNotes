package com.ljuntyg.gitnotes

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Database
import android.content.Context
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Delete
import androidx.room.Room
import androidx.room.Update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

// TODO: Figure out how to make id val without getting: "error: Cannot find setter for field."
@Parcelize
@Entity
data class Note(
    // Automatically generates a unique key (id) for each Note (for the Room database)
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    var title: String = "",
    var body: String = "",

    @ColumnInfo(name = "created_at")
    var createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_updated_at")
    var lastUpdatedAt: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Room database related members
 */

// WARNING: Only access DAO through the repository, not directly
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
