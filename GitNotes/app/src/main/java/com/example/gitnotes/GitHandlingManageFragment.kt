package com.example.gitnotes

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.gitnotes.databinding.FragmentGitHandlingManageBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class GitHandlingManageFragment(private val selectedRepository: Repository) : Fragment() {
    private var _binding: FragmentGitHandlingManageBinding? = null
    private val binding get() = _binding!!

    private lateinit var userProfilesViewModel: UserProfilesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentGitHandlingManageBinding.inflate(inflater, container, false)
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

        // TODO: Observe some data to update these when a repository is deleted
        if (selectedRepository.httpsLink.isEmpty()) {
            binding.editTextHandlingManage1.setText(R.string.no_repo_link)
            binding.linearLayoutHandlingManage.visibility = View.GONE
        } else {
            binding.editTextHandlingManage1.setText(selectedRepository.httpsLink)
            binding.linearLayoutHandlingManage.visibility = View.VISIBLE
        }

        // Push button
        binding.buttonHandlingManage1.setOnClickListener {
            // TODO: Implement
        }

        // Pull button
        binding.buttonHandlingManage2.setOnClickListener {
            if (userProfilesViewModel.selectedUserPrefs.getCredentials().first == userProfilesViewModel.selectedUserProfile.value?.profileName
                && userProfilesViewModel.selectedUserPrefs.getCredentials().second?.isNotEmpty() == true
            ) { // Possibly valid token exists for user
                Log.d("MYLOG", "Credentials matching user found")
                // TODO: Pull with credentials
            } else { // Request token from user
                // Open login fragment
                val loginFragment = GitLoginFragment(selectedRepository)
                loginFragment.show(requireActivity().supportFragmentManager, "GitLoginFragment")
            }
        }

        // Delete button
        binding.buttonHandlingManage3.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val deletionSuccessful = userProfilesViewModel.deleteRepoForUserAsync(selectedRepository, userProfilesViewModel.selectedUserProfile.value!!).await()
                if (deletionSuccessful) {
                    Snackbar.make(
                        view,
                        "Deleted repository ${selectedRepository.name}",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    Snackbar.make(
                        view,
                        "Failed to delete repository",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }

                binding.editTextHandlingManage1.setText("")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}