package com.example.gitnotes

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gitnotes.databinding.FragmentRecyclerViewBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class RecyclerViewFragment : Fragment() {

    private var _binding: FragmentRecyclerViewBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var notesViewModel: NotesViewModel

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentRecyclerViewBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the NotesViewModel using custom NotesViewModelFactory
        // required since NotesViewModel has non-empty constructor
        val notesDao = NotesDatabase.getDatabase(requireActivity().applicationContext).notesDao()
        val repository = NotesRepository(notesDao)
        val viewModelFactory = NotesViewModelFactory(repository)
        notesViewModel = ViewModelProvider(this, viewModelFactory)[NotesViewModel::class.java]

        // Initialize RecyclerView with Adapter for notes
        val adapter = NoteListAdapter(emptyList())
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        // Observe allNotes LiveData from the NotesViewModel
        notesViewModel.allNotes.observe(viewLifecycleOwner) { notes ->
            // Update the cached copy of the notes in the adapter.
            notes?.let { adapter.notes = it }
        }

        // Set up FAB click listener
        binding.fab.setOnClickListener { fabView ->
            Snackbar.make(fabView, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAnchorView(binding.fab)
                .setAction("Action", null).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}