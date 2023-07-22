package com.example.gitnotes

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.gitnotes.databinding.FragmentNoteBinding
import com.google.android.material.snackbar.Snackbar

class NoteFragment : Fragment() {
    private var _binding: FragmentNoteBinding? = null
    private val binding get() = _binding!!

    private lateinit var note: Note
    private lateinit var notesViewModel: NotesViewModel

    private var newNote: Boolean = false

    // Used to handle getParcelable(String key) deprecation (deprecated in API level 33)
    // in favor of the new getParcelable(String key, Class class) if version >= API 33
    private inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelable(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelable(key) as? T
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { // See nav_graph.xml for fragments passing data to this Fragment
            note = it.parcelable("note")?: Note()
            newNote = it.getBoolean("new_note")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (newNote) {
            Snackbar.make(
                view,
                "New note created",
                Snackbar.LENGTH_SHORT
            ).show()
        }

        val notesDao = NotesDatabase.getDatabase(requireActivity().applicationContext).notesDao()
        val repository = NotesRepository(notesDao)
        val viewModelFactory = NotesViewModelFactory(repository)
        notesViewModel = ViewModelProvider(requireActivity(), viewModelFactory)[NotesViewModel::class.java]

        binding.editTextTitle.setText(note.title)
        binding.editTextBody.setText(note.body)

        (requireActivity() as MenuHost).addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.menu_note, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.action_delete -> {
                        notesViewModel.delete(note)
                        Snackbar.make(
                            view,
                            "Note deleted",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        findNavController().popBackStack()
                        return true
                    }
                }
                return false
            }
        }, viewLifecycleOwner)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        note.title = binding.editTextTitle.text.toString()
        note.body = binding.editTextBody.text.toString()

        // Ensure Note exists in database, if so update it
        val allNotes = notesViewModel.allNotes.value

        if (allNotes != null && allNotes.any { n -> n.id == note.id }) {
            notesViewModel.update(note)
        } else {
            // TODO: Better handling of this case, this case reached could mean Note has been deleted
        }

        _binding = null
    }
}
