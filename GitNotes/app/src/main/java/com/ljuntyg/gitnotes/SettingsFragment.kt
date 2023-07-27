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
import androidx.lifecycle.viewModelScope
import com.ljuntyg.gitnotes.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var userProfilesViewModel: UserProfilesViewModel
    private lateinit var notesViewModel: NotesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get reference to UserProfilesViewModel
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
            requireActivity(), userProfilesViewModelFactory
        )[UserProfilesViewModel::class.java]

        // Get reference to NotesViewModel
        val notesDao = NotesDatabase.getDatabase(requireActivity().applicationContext).notesDao()
        val notesRepository = NotesRepository(notesDao)
        val notesViewModelFactory = NotesViewModelFactory(notesRepository)
        notesViewModel =
            ViewModelProvider(requireActivity(), notesViewModelFactory)[NotesViewModel::class.java]

        // Change user
        binding.button1.setOnClickListener { }

        // Reset token
        binding.button2.setOnClickListener {
            val user = userProfilesViewModel.selectedUserPrefs.getCredentials().first
            if (user != null) {
                userProfilesViewModel.selectedUserPrefs.insertOrReplace(user, "")
                view.showShortSnackbar(getString(R.string.token_deleted))
            } else {
                view.showShortSnackbar(getString(R.string.no_user_token_not_deleted))
            }
        }

        // Delete all notes
        binding.button3.setOnClickListener {
            notesViewModel.viewModelScope.launch {
                val notes = notesViewModel.allNotes.value
                if (notes != null) {
                    for (note in notes) {
                        notesViewModel.delete(note)
                    }

                    if (notesViewModel.allNotes.value?.isNotEmpty() == true) {
                        view.showShortSnackbar(getString(R.string.notes_deleted))
                    } else {
                        view.showShortSnackbar(getString(R.string.incomplete_note_deletion))
                    }
                } else {
                    view.showShortSnackbar(getString(R.string.no_notes_notes_not_deleted))
                }
            }
        }

        // Delete all Git repositories (by deleting any repository folders), doesn't delete database repositories
        binding.button4.setOnClickListener {
            userProfilesViewModel.viewModelScope.launch {
                val filesDir = requireActivity().filesDir

                filesDir.walk().forEach { file ->
                    if (file.isDirectory && file.resolve(".git").exists()) {
                        file.deleteRecursively()
                    }
                }

                view.showShortSnackbar(getString(R.string.git_repos_deleted))
            }
        }

        // Menu handling
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        }, viewLifecycleOwner)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}