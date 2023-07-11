package com.example.gitnotes

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Database
import android.content.Context

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

/**
 * Room database related members
 */

@Dao
interface NoteDao {
    @Query("SELECT * FROM Note")
    fun getAllNotes(): List<Note>

    @Insert
    fun insert(note: Note)
}

@Database(entities = [Note::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
