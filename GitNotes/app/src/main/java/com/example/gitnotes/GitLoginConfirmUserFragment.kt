package com.example.gitnotes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gitnotes.databinding.FragmentGitLoginConfirmUserBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch


class GitLoginConfirmUserFragment : Fragment() {
    private var _binding: FragmentGitLoginConfirmUserBinding? = null
    private val binding get() = _binding!!

    private lateinit var userProfilesViewModel: UserProfilesViewModel

    private lateinit var profileName: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGitLoginConfirmUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get reference to UserProfilesViewModel
        val userProfilesDao = ProfilesReposDatabase.getDatabase(requireActivity().applicationContext).userProfilesDao()
        val repositoriesDao = ProfilesReposDatabase.getDatabase(requireActivity().applicationContext).repositoriesDao()
        val profilesReposRepository = ProfilesReposRepository(userProfilesDao, repositoriesDao)
        val userProfilesViewModelFactory = UserProfilesViewModelFactory(requireActivity().application, profilesReposRepository)
        userProfilesViewModel = ViewModelProvider(requireActivity(), userProfilesViewModelFactory)[UserProfilesViewModel::class.java]

        binding.buttonLoginConfirm.setOnClickListener {
            userProfilesViewModel.viewModelScope.launch {
                val userProfile = userProfilesViewModel.getUserProfileAsync(profileName).await()
                if (userProfile == null) {
                    Snackbar.make(
                        view,
                        "Unable to find user profile",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    userProfilesViewModel.setSelectedUserProfile(userProfile)
                    // If selected user does not have stored credentials, then overwrite the last user's token with a new empty token,
                    // if selected user DOES have stored credentials, then nothing will be done and the token will be kept as is for user
                    if (userProfilesViewModel.selectedUserPrefs.getCredentials().first != userProfile.profileName) {
                        userProfilesViewModel.selectedUserPrefs.insertOrReplace(userProfile.profileName, "")
                    }
                    userProfilesViewModel.loggedIn = true

                    Snackbar.make(
                        requireActivity().findViewById(android.R.id.content),
                        "Logged in as ${userProfile.profileName}",
                        Snackbar.LENGTH_SHORT
                    ).show()

                    (parentFragment as DialogFragment).dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setConfirmText(text: String) {
        binding.textViewLoginConfirm.text = getString(R.string.selected_profile, text)
        profileName = text
    }
}