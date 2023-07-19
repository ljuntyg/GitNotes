package com.example.gitnotes

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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
    private lateinit var userProfilesViewModel: UserProfilesViewModel

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
        // which is required since NotesViewModel has non-empty constructor
        val notesDao = NotesDatabase.getDatabase(requireActivity().applicationContext).notesDao()
        val notesRepository = NotesRepository(notesDao)
        val notesViewModelFactory = NotesViewModelFactory(notesRepository)
        notesViewModel = ViewModelProvider(this, notesViewModelFactory)[NotesViewModel::class.java]

        // Same as above but for the UserProfilesViewModel
        val userProfilesDao = ProfilesReposDatabase.getDatabase(requireActivity().applicationContext).userProfilesDao()
        val repositoriesDao = ProfilesReposDatabase.getDatabase(requireActivity().applicationContext).repositoriesDao()
        val profilesReposRepository = ProfilesReposRepository(userProfilesDao, repositoriesDao)
        val userProfilesViewModelFactory = UserProfilesViewModelFactory(profilesReposRepository)
        userProfilesViewModel = ViewModelProvider(this, userProfilesViewModelFactory)[UserProfilesViewModel::class.java]

        // Initialize RecyclerView with Adapter for notes
        val adapter = NoteListAdapter(findNavController())
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        // Observe allNotes LiveData from the NotesViewModel
        notesViewModel.allNotes.observe(viewLifecycleOwner) { notes ->
            // Update the cached copy of the notes in the adapter.
            notes?.let { adapter.notes = it }
        }

        /** Example of adding note. insert() will call the database repository
            insert() which will launch a coroutine to insert into the DAO
        val note1 = Note(null,"titleTEST", "body");
        notesViewModel.insert(note1); */

        // Set up FAB click listener
        binding.fab.setOnClickListener {
            val newNote = Note()
            val job = notesViewModel.insert(newNote)
            lifecycleScope.launch {
                newNote.id = job.await().toInt()  // This will suspend until result is available
                val action = RecyclerViewFragmentDirections.actionRecyclerViewFragmentToNoteFragment(newNote)
                findNavController().navigate(action)
            }
        }

        // Menu handling
        (requireActivity() as MenuHost).addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.action_git -> {
                        if (!userProfilesViewModel.loggedIn) {
                            val action = RecyclerViewFragmentDirections.actionRecyclerViewFragmentToGitLoginFragment()
                            findNavController().navigate(action)
                        } else {
                            val action = RecyclerViewFragmentDirections.actionRecyclerViewFragmentToGitHandlingFragment(userProfilesViewModel.selectedUserProfile)
                            findNavController().navigate(action)
                        }
                        return true
                    }
                }
                return false
            }
        }, viewLifecycleOwner)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}