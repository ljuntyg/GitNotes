package com.ljuntyg.gitnotes

import android.os.Bundle
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ljuntyg.gitnotes.databinding.FragmentRecyclerViewBinding
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
    ): View {
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
        notesViewModel =
            ViewModelProvider(requireActivity(), notesViewModelFactory)[NotesViewModel::class.java]

        // Same as above but for the UserProfilesViewModel
        val userProfilesDao =
            ProfilesReposDatabase.getDatabase(requireActivity().applicationContext)
                .userProfilesDao()
        val repositoriesDao =
            ProfilesReposDatabase.getDatabase(requireActivity().applicationContext)
                .repositoriesDao()
        val profilesReposRepository = ProfilesReposRepository(userProfilesDao, repositoriesDao)
        val userProfilesViewModelFactory =
            UserProfilesViewModelFactory(requireActivity().application, profilesReposRepository)
        userProfilesViewModel = ViewModelProvider(
            requireActivity(),
            userProfilesViewModelFactory
        )[UserProfilesViewModel::class.java]

        // Initialize RecyclerView with Adapter for notes
        val adapter = NoteListAdapter(findNavController())
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        // Observe allNotes LiveData from the NotesViewModel
        notesViewModel.allNotes.observe(viewLifecycleOwner) { notes ->
            // Update the cached copy of the notes in the adapter.
            notes?.let { adapter.notes = it }
        }

        // Set up FAB click listener
        binding.fab.setOnClickListener {
            val newNote = Note()
            lifecycleScope.launch {
                newNote.id =
                    notesViewModel.insertAsync(newNote)
                        .await()  // This will suspend until result is available
                val action =
                    RecyclerViewFragmentDirections.actionRecyclerViewFragmentToNoteFragment(
                        newNote,
                        true
                    )
                findNavController().navigate(action)
            }
        }

        // Menu handling
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.menu_base, menu)
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.action_git -> {
                        if (!userProfilesViewModel.loggedIn) {
                            val gitLoginDialog = GitLoginFragment()
                            gitLoginDialog.show(
                                requireActivity().supportFragmentManager,
                                "GitLoginFragment"
                            )
                        } else {
                            val gitHandlingFragment = GitHandlingFragment()
                            gitHandlingFragment.show(
                                requireActivity().supportFragmentManager,
                                "GitHandlingFragment"
                            )
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