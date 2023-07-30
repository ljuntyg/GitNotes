package com.ljuntyg.gitnotes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.ljuntyg.gitnotes.databinding.RecyclerViewRowBinding

class NoteListAdapter(private val navController: NavController) : RecyclerView.Adapter<NoteListAdapter.NoteViewHolder>() {

    var notes: List<Note> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged() // TODO: Change to more efficient solution, consider using ListAdapter for DiffUtil
        }

    class NoteViewHolder(private val binding: RecyclerViewRowBinding, private val navController: NavController) : RecyclerView.ViewHolder(binding.root) {
        fun bind(note: Note) {
            val displayTitle = note.title.ifBlank {
                val fromBody = note.body.split("\n").firstOrNull().orEmpty()
                if (fromBody.isBlank()) {
                    "Empty note"
                } else {
                    "…$fromBody"
                }
            }

            binding.textView.text = displayTitle
            binding.dateTextView.text = note.lastUpdatedAt.toFormattedDateString()

            binding.root.setOnClickListener {
                val action = RecyclerViewFragmentDirections.actionRecyclerViewFragmentToNoteFragment(note, false)
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