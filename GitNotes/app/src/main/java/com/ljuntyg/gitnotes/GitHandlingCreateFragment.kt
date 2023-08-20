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

        val editText1 = binding.textInputLayoutHandlingCreate1.editText!!
        val editText2 = binding.textInputLayoutHandlingCreate2.editText!!

        val textWatcher2 = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                val repoLink = p0.toString().trim()

                if (repoLink.isValidHTTPSlink()) {
                    val remoteName = repoLink.getRepoNameFromUrl()
                    val localName = editText1.text.toString().trim()

                    if (remoteName == localName) {
                        binding.textInputLayoutHandlingCreate2.isErrorEnabled = false
                    } else {
                        binding.textInputLayoutHandlingCreate2.error = getString(
                            R.string.repo_name_mismatch,
                            remoteName,
                            localName
                        )
                    }
                } else {
                    if (repoLink.isNotEmpty()) {
                        binding.textInputLayoutHandlingCreate2.error =
                            getString(R.string.invalid_link)
                    } else {
                        binding.textInputLayoutHandlingCreate2.isErrorEnabled = false
                    }
                }
            }
        }

        val textWatcher1 = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                textWatcher2.afterTextChanged(editText2.text)
            }
        }

        editText1.addTextChangedListener(textWatcher1)
        editText2.addTextChangedListener(textWatcher2)

        // Create repository button
        binding.buttonHandlingCreate.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val repoName = binding.textInputLayoutHandlingCreate1.editText!!.text.toString()
                val repoLink = binding.textInputLayoutHandlingCreate2.editText!!.text.toString()
                val selectedUser = userProfilesViewModel.selectedUserProfile.value!!
                if (repoName.isEmpty()) {
                    view.showShortSnackbar(getString(R.string.enter_repo_name))
                    return@launch
                } else {
                    if (repoLink.isNotEmpty() && !repoLink.isValidHTTPSlink()) {
                        view.showShortSnackbar(getString(R.string.invalid_link))

                        return@launch
                    }

                    val newRepo = Repository(
                        profileName = selectedUser.profileName,
                        name = repoName,
                        httpsLink = repoLink
                    )
                    val insertionSuccessful =
                        userProfilesViewModel.insertRepoForUserAsync(newRepo, selectedUser).await()
                    if (insertionSuccessful) {
                        view.showShortSnackbar(getString(R.string.created_repo, repoName))

                        userProfilesViewModel.setSelectedRepository(newRepo)
                    } else {
                        view.showShortSnackbar(getString(R.string.failed_create_repo))
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