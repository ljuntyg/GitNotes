package com.example.gitnotes

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.core.view.size
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.gitnotes.databinding.FragmentGitLoginBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GitLoginFragment(private val selectedRepository: Repository) : DialogFragment() {
    private var _binding: FragmentGitLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var userProfilesViewModel: UserProfilesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGitLoginBinding.inflate(inflater, container, false)
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
        userProfilesViewModel.allUserProfiles.observe(viewLifecycleOwner) { profiles ->
            profiles?.let {
                // This block will be executed whenever the data changes, including when the data is first loaded
                val data = mutableListOf("New user profile")
                data.addAll(profiles.map { profile -> profile.profileName })
                val adapter =
                    CustomSpinnerAdapter(requireContext(), android.R.layout.simple_spinner_item, data)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerLogin.adapter = adapter

                // If user is logged in, set spinner to user and lock it
                if (userProfilesViewModel.loggedIn) {
                    for (i in 0 until binding.spinnerLogin.adapter.count) {
                        if (binding.spinnerLogin.getItemAtPosition(i).toString()
                            == userProfilesViewModel.selectedUserProfile.value?.profileName) {
                            binding.spinnerLogin.setSelection(i)
                            binding.spinnerLogin.isEnabled = false

                            // TODO: Show some view
                        }
                    }
                }
            }
        }

        // Initialize FrameLayout
        val confirmUserFragment = GitLoginConfirmUserFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.container_login, confirmUserFragment)
            .commit()

        // Initialize ViewPager2
        val pagerAdapter = LoginViewPagerAdapter(requireActivity())
        binding.viewpagerLogin.adapter = pagerAdapter

        // Set spinner on item selected listener if not logged in, if logged in then the spinner
        // will be set manually and locked to a position, so this listener needs to be disabled
        if (!userProfilesViewModel.loggedIn) {
            binding.spinnerLogin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    if (position == 0) { // "New user profile" selected
                        binding.containerLogin.visibility = View.GONE
                        binding.viewpagerLogin.visibility = View.VISIBLE
                    } else { // Some user profile selected
                        binding.containerLogin.visibility = View.VISIBLE
                        binding.viewpagerLogin.visibility = View.GONE

                        userProfilesViewModel.viewModelScope.launch {
                            val selectedProfileName =
                                binding.spinnerLogin.getItemAtPosition(position).toString()
                            val userProfileDeferred =
                                userProfilesViewModel.getUserProfileAsync(selectedProfileName)
                            val userProfile =
                                userProfileDeferred.await() // Await here to get the UserProfile

                            withContext(Dispatchers.Main) {
                                confirmUserFragment.setConfirmText(
                                    userProfile?.profileName ?: "No user profile found"
                                )
                            }
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        } else { // User logged in, show fragment to request token from user
            val tokenInputFragment = GitLoginInputFragment.newInstanceProvideToken(
                "Provide PAT for User",
                "No PAT (Personal Access Token) found for user. Please provide a PAT with access to the repository.",
                selectedRepository.httpsLink,
                "Provide PAT"
            )

            childFragmentManager.beginTransaction()
                .replace(R.id.container_login, tokenInputFragment)
                .commit()
        }

        /** // Set listener for confirm profile selection button (not the ViewPager button),
        // on button click, set the selectedUserProfile and loggedIn variables in ViewModel
        binding.button.setOnClickListener {
            lifecycleScope.launch {
                val selectedUserProfile = userProfilesViewModel.getUserProfileAsync(binding.spinnerLogin.selectedItem.toString()).await()
                if (selectedUserProfile == null) {
                    Snackbar.make(
                        requireActivity().findViewById(android.R.id.content),
                        "Unable to find profile ${binding.spinnerLogin.selectedItem}, unable to login",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                userProfilesViewModel.setSelectedUserProfile(selectedUserProfile)
                userProfilesViewModel.loggedIn = true
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    "Logged in as ${selectedUserProfile.profileName}",
                    Snackbar.LENGTH_SHORT
                ).show()
                dismiss()
            }
        } */

        /** // Set up the ViewPager/create token input fragment
        if (!userProfilesViewModel.loggedIn) {
            val pagerAdapter = LoginViewPagerAdapter(requireActivity())
            binding.viewPager.adapter = pagerAdapter
        } else {
            val tokenInputFragment = GitLoginInputFragment.newInstanceProvideToken(
                "Provide PAT for User",
                "No PAT (Personal Access Token) found for user. Please provide a PAT with access to the repository.",
                selectedRepository.httpsLink,
                "Provide PAT"
            )

            childFragmentManager.beginTransaction()
                .replace(R.id.git_input_fragment_container, tokenInputFragment)
                .commit()
        } */
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}