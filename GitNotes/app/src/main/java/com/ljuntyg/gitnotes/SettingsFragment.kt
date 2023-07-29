package com.ljuntyg.gitnotes

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.launch

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var userProfilesViewModel: UserProfilesViewModel
    private lateinit var notesViewModel: NotesViewModel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val loginStatusPreference: Preference? = findPreference("login_status")
        val selectedProfilePreference: Preference? = findPreference("selected_profile")
        userProfilesViewModel.loggedIn.observe(viewLifecycleOwner) { loggedIn ->
            if (loggedIn == true) {
                loginStatusPreference?.summary = getString(R.string.logged_in, getString(R.string.true_string))
                selectedProfilePreference?.summary = getString(R.string.selected_profile, userProfilesViewModel.selectedUserProfile.value!!.profileName)
            } else {
                loginStatusPreference?.summary = getString(R.string.logged_in, getString(R.string.false_string))
                selectedProfilePreference?.summary = getString(R.string.no_user_profile_selected)
            }
        }

        // Trigger a change on loggedIn so the above observe statement executes and sets the summaries
        userProfilesViewModel.setLoggedIn(userProfilesViewModel.loggedIn.value ?: false)

        val notesCountPreference: Preference? = findPreference("notes_count")
        notesViewModel.allNotes.observe(viewLifecycleOwner) { notes ->
            notesCountPreference?.summary = getString(R.string.notes_count, notes.size.toString())
        }

        val profilesCountPreference: Preference? = findPreference("profiles_count")
        userProfilesViewModel.allUserProfiles.observe(viewLifecycleOwner) { profiles ->
            profilesCountPreference?.summary = getString(R.string.profiles_count, profiles.size.toString())
        }

        val reposCountPreference: Preference? = findPreference("repos_count")
        userProfilesViewModel.allRepositories.observe(viewLifecycleOwner) { repositories ->
            reposCountPreference?.summary = getString(R.string.repos_count,
                repositories.map { repo -> repo.name != "NEW REPOSITORY" }.size.toString())
        }

        // Clear the menu
        (requireActivity() as MenuHost).addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean { return false }

        }, viewLifecycleOwner)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        preference.let {
            when(it.key) {
                "change_user_profile" -> {
                    // Handle user profile change
                    userProfilesViewModel.setLoggedIn(false)
                    val gitLoginDialog = GitLoginFragment()
                    gitLoginDialog.show(requireActivity().supportFragmentManager, "GitLoginFragment")
                }

                "reset_token" -> {
                    // Handle token reset
                    val user = userProfilesViewModel.selectedUserPrefs.getCredentials().first ?: ""
                    if (userProfilesViewModel.selectedUserPrefs.getCredentials().second?.isNotEmpty() == true) {
                        userProfilesViewModel.selectedUserPrefs.insertOrReplace(user, "")
                        view?.showShortSnackbar(getString(R.string.token_deleted))
                    } else {
                        view?.showShortSnackbar(getString(R.string.no_token_found))
                    }
                }

                "delete_all_notes" -> {
                    // Handle all notes deletion
                    notesViewModel.viewModelScope.launch {
                        val notes = notesViewModel.allNotes.value
                        if (notes != null) {
                            for (note in notes) {
                                notesViewModel.delete(note)
                            }

                            if (notesViewModel.allNotes.value?.isNotEmpty() == true) {
                                view?.showShortSnackbar(getString(R.string.notes_deleted))
                            } else {
                                view?.showShortSnackbar(getString(R.string.incomplete_note_deletion))
                            }
                        } else {
                            view?.showShortSnackbar(getString(R.string.no_notes_notes_not_deleted))
                        }
                    }
                }

                "delete_all_git_repos" -> {
                    // Handle all git repos deletion
                    userProfilesViewModel.viewModelScope.launch {
                        val filesDir = requireActivity().filesDir

                        filesDir.walk().forEach { file ->
                            if (file.isDirectory && file.resolve(".git").exists()) {
                                file.deleteRecursively()
                            }
                        }

                        view?.showShortSnackbar(getString(R.string.git_repos_deleted))
                    }
                }

                "delete_selected_profile" -> {
                    // Handle selected profile deletion
                    val selectedUserProfile = userProfilesViewModel.selectedUserProfile.value
                    if (selectedUserProfile == null || !userProfilesViewModel.loggedIn.value!!) {
                        Toast.makeText(requireContext(), getString(R.string.no_user_profile_selected), Toast.LENGTH_SHORT).show()
                    } else {
                        userProfilesViewModel.delete(selectedUserProfile)
                        userProfilesViewModel.setLoggedIn(false)
                        view?.showShortSnackbar(getString(R.string.deleted_profile, selectedUserProfile.profileName.trim()))
                    }
                }

                else -> {}
            }
        }
        return super.onPreferenceTreeClick(preference)
    }
}