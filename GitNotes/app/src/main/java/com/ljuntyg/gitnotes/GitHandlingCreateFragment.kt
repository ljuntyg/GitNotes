package com.ljuntyg.gitnotes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ljuntyg.gitnotes.databinding.FragmentGitHandlingCreateBinding
import kotlinx.coroutines.launch


class GitHandlingCreateFragment : Fragment() {
    private var _binding: FragmentGitHandlingCreateBinding? = null
    private val binding get() = _binding!!

    private lateinit var userProfilesViewModel: UserProfilesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        binding.textInputLayoutHandlingCreate2.editText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                binding.textInputLayoutHandlingCreate2.validateLink()
            }
        })

        // Create repository button
        binding.buttonHandlingCreate.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val repoName = binding.textInputLayoutHandlingCreate1.editText!!.text.toString()
                val repoLink = binding.textInputLayoutHandlingCreate2.editText!!.text.toString()
                val selectedUser = userProfilesViewModel.selectedUserProfile.value!!
                if (repoName.isEmpty()) {
                    view.showShortSnackbar("Please enter a name for the repository")
                    return@launch
                } else {
                    if (repoLink.isNotEmpty() && !repoLink.isValidHTTPSlink()) {
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

                binding.textInputLayoutHandlingCreate1.editText!!.setText("")
                binding.textInputLayoutHandlingCreate2.editText!!.setText("")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}