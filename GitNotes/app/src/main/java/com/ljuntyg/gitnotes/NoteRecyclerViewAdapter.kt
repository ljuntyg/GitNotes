package com.ljuntyg.gitnotes

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.ljuntyg.gitnotes.databinding.RecyclerViewRowBinding
import java.util.Locale

class NoteListAdapter(private val navController: NavController) : RecyclerView.Adapter<NoteListAdapter.NoteViewHolder>(),
    Filterable {

    var notes: List<Note> = listOf()
        set(value) {
            field = value
            notesFiltered = ArrayList(value)
            notifyDataSetChanged() // TODO: Change to more efficient solution, consider using ListAdapter for DiffUtil
        }

    var notesFiltered: ArrayList<Note> = ArrayList()

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
        val currentNote = notesFiltered[position]
        holder.bind(currentNote)
    }

    override fun getItemCount() = notesFiltered.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint.toString()
                notesFiltered = if (charString.isEmpty()) {
                    ArrayList(notes)
                } else {
                    val filteredList = arrayListOf<Note>()
                    for (note in notes) {
                        if (note.title.lowercase(Locale.ROOT).contains(charString.lowercase(Locale.ROOT)) ||
                            note.body.lowercase(Locale.ROOT).contains(charString.lowercase(Locale.ROOT))) {
                            filteredList.add(note)
                        }
                    }
                    filteredList
                }

                val filterResults = FilterResults()
                filterResults.values = notesFiltered
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notesFiltered = results?.values as ArrayList<Note>
                notifyDataSetChanged()
            }
        }
    }
}
