package com.example.gitnotes

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.gitnotes.databinding.FragmentGitLoginBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class GitLoginFragment : DialogFragment() {
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
                    ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, data)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.userSpinner.adapter = adapter
            }
        }

        // Set spinner on item selected listener
        binding.userSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) =
                if (position == 0) { // "New user profile" selected
                    binding.viewPager.visibility = View.VISIBLE
                    binding.cardViewLoginNoInput.visibility = View.GONE
                } else { // Some user profile selected
                    binding.viewPager.visibility = View.GONE
                    binding.cardViewLoginNoInput.visibility = View.VISIBLE
                    binding.textSelectedProfile.text = getString(R.string.selected_profile, binding.userSpinner.selectedItem)
                }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Set listener for confirm profile selection button (not the ViewPager button),
        // on button click, set the selectedUserProfile and loggedIn variables in ViewModel
        binding.buttonConfirm.setOnClickListener {
            lifecycleScope.launch {
                val selectedUserProfile = userProfilesViewModel.getUserProfileAsync(binding.userSpinner.selectedItem.toString()).await()
                if (selectedUserProfile == null) {
                    Snackbar.make(
                        requireActivity().findViewById(android.R.id.content),
                        "Unable to find profile ${binding.userSpinner.selectedItem}, unable to login",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                userProfilesViewModel.selectedUserProfile = selectedUserProfile
                userProfilesViewModel.loggedIn = true
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    "Logged in as ${selectedUserProfile.profileName}",
                    Snackbar.LENGTH_SHORT
                ).show()
                dismiss()
            }
        }

        // Set up the ViewPager
        val pagerAdapter = LoginViewPagerAdapter(requireActivity())
        binding.viewPager.adapter = pagerAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}