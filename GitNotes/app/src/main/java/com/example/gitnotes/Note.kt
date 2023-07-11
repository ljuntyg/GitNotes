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
import androidx.recyclerview.widget.RecyclerView

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
 * RecyclerView related members
 */

class NoteListAdapter(private val notes: List<Note>) : RecyclerView.Adapter<NoteListAdapter.NoteViewHolder>() {

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
