package com.example.gitnotes

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.gitnotes.databinding.FragmentGitHandlingBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GitHandlingFragment : DialogFragment() {
    private var _binding: FragmentGitHandlingBinding? = null
    private val binding get() = _binding!!

    private lateinit var userProfilesViewModel: UserProfilesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentGitHandlingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // This ensures that the custom white background with rounded corners is what's visible
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Get reference to UserProfilesViewModel
        val userProfilesDao = ProfilesReposDatabase.getDatabase(requireActivity().applicationContext).userProfilesDao()
        val repositoriesDao = ProfilesReposDatabase.getDatabase(requireActivity().applicationContext).repositoriesDao()
        val profilesReposRepository = ProfilesReposRepository(userProfilesDao, repositoriesDao)
        val userProfilesViewModelFactory = UserProfilesViewModelFactory(requireActivity().application, profilesReposRepository)
        userProfilesViewModel = ViewModelProvider(requireActivity(), userProfilesViewModelFactory)[UserProfilesViewModel::class.java]

        // Populate the spinner
        val adapter = CustomSpinnerAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf<String>())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.repoSpinner.adapter = adapter

        // TODO: Find way to do this via ViewModel, don't access DAO here
        // Observe the flow of the Repository lists for the selectedUser directly from the DAO
        userProfilesViewModel.selectedUserRepositories.observe(viewLifecycleOwner) { repositories ->
            // Update the data for the spinner
            val updatedData = mutableListOf("New repository")
            updatedData.addAll(repositories.map { repository -> repository.name })

            // Update the spinner
            adapter.clear()
            adapter.addAll(updatedData)
        }

        // Set spinner on item selected listener
        binding.repoSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) =
                if (position == 0) { // "New repository" selected
                    binding.editTextHandling.isEnabled = true
                    binding.editTextHandling.hint = "Repository name"

                    binding.editTextHandling2.visibility = View.VISIBLE
                    binding.editTextHandling2.hint = "Repository link"

                    binding.pullButton.visibility = View.GONE
                    binding.pushButton.visibility = View.GONE
                    binding.deleteButton.visibility = View.GONE
                    binding.buttonAdd.visibility = View.VISIBLE
                } else { // Some repository selected
                    val theRepo: Repository? = userProfilesViewModel.selectedUserRepositories.value?.find {
                            repo -> repo.name == binding.repoSpinner.selectedItem.toString()
                    }
                    if (theRepo == null) {
                        binding.editTextHandling.hint = "ERROR: Selected repository not found"
                    } else {
                        val link = theRepo.httpsLink
                        if (link == null) {
                            binding.editTextHandling.hint = "ERROR: Null HTTPS link"
                        } else if (link.isEmpty()) {
                            binding.editTextHandling.hint = "This repository lacks an HTTPS link"
                        } else {
                            binding.editTextHandling.hint = link
                        }
                    }

                    binding.editTextHandling.isEnabled = false
                    binding.pullButton.visibility = View.VISIBLE
                    binding.pushButton.visibility = View.VISIBLE
                    binding.deleteButton.visibility = View.VISIBLE
                    binding.buttonAdd.visibility = View.GONE
                    binding.editTextHandling2.visibility = View.GONE
                }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.deleteButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val theRepo: Repository? = userProfilesViewModel.selectedUserRepositories.value!!.find {
                        repo -> repo.name == binding.repoSpinner.selectedItem
                }
                if (theRepo == null) {
                    Snackbar.make(
                        view,
                        "ERROR: Repository to delete not found",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    val deletionSuccessful = userProfilesViewModel.deleteRepoForUserAsync(theRepo, userProfilesViewModel.selectedUserProfile.value!!).await()
                    if (deletionSuccessful) {
                        Snackbar.make(
                            view,
                            "Deleted repository ${theRepo.name}",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } else {
                        Snackbar.make(
                            view,
                            "Failed to delete repository",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }

                binding.editTextHandling.setText("")
            }
        }

        binding.buttonAdd.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val repoName = binding.editTextHandling.text.toString()
                val repoLink = binding.editTextHandling2.text.toString()
                val selectedUser = userProfilesViewModel.selectedUserProfile.value!!
                if (repoName.isEmpty()) {
                    Snackbar.make(
                        view,
                        "Please enter a name for the repository",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@launch
                } else {
                    val newRepo = Repository(
                        profileName = selectedUser.profileName,
                        name = repoName,
                        httpsLink = repoLink)
                    val insertionSuccessful = userProfilesViewModel.insertRepoForUserAsync(newRepo, selectedUser).await()
                    if (insertionSuccessful) {
                        Snackbar.make(
                            view,
                            "Created new repository $repoName",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } else {
                        Snackbar.make(
                            view,
                            "Failed to create new repository",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }

                binding.editTextHandling.setText("")
                binding.editTextHandling2.setText("")
            }
        }

        binding.pullButton.setOnClickListener {
            if (userProfilesViewModel.selectedUserPrefs.getCredentials().first == userProfilesViewModel.selectedUserProfile.value?.profileName
                && userProfilesViewModel.selectedUserPrefs.getCredentials().second?.isNotEmpty() == true
            ) {
                Log.d("MYLOG", "Credentials matching user found")
                // TODO: Pull with credentials
            } else {
                var selectedRepository = userProfilesViewModel.selectedUserRepositories.value?.find {
                        repo -> repo.name == binding.repoSpinner.selectedItem.toString()
                }

                selectedRepository = selectedRepository ?: Repository(profileName = "", name = "", httpsLink = "")

                // Open login fragment
                val loginFragment = GitLoginFragment(selectedRepository)
                loginFragment.show(requireActivity().supportFragmentManager, "GitLoginFragment")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}