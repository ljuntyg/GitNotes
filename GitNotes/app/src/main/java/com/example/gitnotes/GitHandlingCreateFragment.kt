package com.example.gitnotes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.gitnotes.databinding.FragmentGitHandlingCreateBinding
import kotlinx.coroutines.launch


class GitHandlingCreateFragment : Fragment() {
    private var _binding: FragmentGitHandlingCreateBinding? = null
    private val binding get() = _binding!!

    private lateinit var userProfilesViewModel: UserProfilesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentGitHandlingCreateBinding.inflate(inflater, container, false)
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

        // Create repository button
        binding.buttonHandlingCreate.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val repoName = binding.editTextHandlingCreate1.text.toString()
                val repoLink = binding.editTextHandlingCreate2.text.toString()
                val selectedUser = userProfilesViewModel.selectedUserProfile.value!!
                if (repoName.isEmpty()) {
                    view.showShortSnackbar("Please enter a name for the repository")
                    return@launch
                } else {
                    if (repoLink.isNotEmpty() && !isValidHTTPSlink(repoLink)) {
                        view.showShortSnackbar("Invalid HTTPS link")

                        return@launch
                    }

                    val newRepo = Repository(
                        profileName = selectedUser.profileName,
                        name = repoName,
                        httpsLink = repoLink)
                    val insertionSuccessful = userProfilesViewModel.insertRepoForUserAsync(newRepo, selectedUser).await()
                    if (insertionSuccessful) {
                        view.showShortSnackbar("Created new repository $repoName")

                        userProfilesViewModel.setSelectedRepository(newRepo)
                    } else {
                        view.showShortSnackbar("Failed to create new repository")
                    }
                }

                binding.editTextHandlingCreate1.setText("")
                binding.editTextHandlingCreate2.setText("")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Regular expression courtesy of https://stackoverflow.com/questions/2514859/regular-expression-for-git-repository
    private fun isValidHTTPSlink(link: String): Boolean {
        val pattern = "((git|ssh|http(s)?)|(git@[\\w\\.]+))(:(//)?)([\\w\\.@\\:/\\-~]+)(\\.git)(/)?".toRegex()
        return link.trim().matches(pattern)
    }
}