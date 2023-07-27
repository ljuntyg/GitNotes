package com.ljuntyg.gitnotes

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ljuntyg.gitnotes.databinding.FragmentGitLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GitLoginFragment : DialogFragment() {
    private var _binding: FragmentGitLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var userProfilesViewModel: UserProfilesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGitLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // This ensures that the custom white background with rounded corners is what's visible
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

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

        // Populate the spinner
        userProfilesViewModel.allUserProfiles.observe(viewLifecycleOwner) { profiles ->
            profiles?.let {
                // This block will be executed whenever the data changes, including when the data is first loaded
                val data = mutableListOf(getString(R.string.new_user_profile))
                val allProfileNames = profiles.map { profile -> profile.profileName }
                data.addAll(allProfileNames)
                val adapter =
                    CustomSpinnerAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        data
                    )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerLogin.adapter = adapter

                // If there was a user logged in before (existing credentials) but no one logged in yet, set spinner to this user
                val prevProfile = allProfileNames.find { profileName ->
                    profileName == userProfilesViewModel.selectedUserPrefs.getCredentials().first
                }
                if (!userProfilesViewModel.loggedIn && prevProfile != null) {
                    binding.spinnerLogin.setSelection(data.indexOf(prevProfile))
                }

                // If user is logged in, set spinner to user and lock it
                if (userProfilesViewModel.loggedIn) {
                    for (i in 0 until binding.spinnerLogin.adapter.count) {
                        if (binding.spinnerLogin.getItemAtPosition(i).toString()
                            == userProfilesViewModel.selectedUserProfile.value?.profileName
                        ) {
                            binding.spinnerLogin.setSelection(i)
                            binding.spinnerLogin.isEnabled = false
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
            binding.spinnerLogin.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
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
                                        userProfile?.profileName
                                            ?: getString(R.string.no_user_profile_found)
                                    )
                                }
                            }
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
        } else { // User logged in, show fragment to request token from user
            val tokenInputFragment = GitLoginInputFragment.newInstanceProvideToken(
                getString(R.string.provide_token_title),
                getString(R.string.provide_token_body),
                userProfilesViewModel.selectedRepository.value!!.httpsLink,
                getString(R.string.provide_token_button)
            )

            childFragmentManager.beginTransaction()
                .replace(R.id.container_login, tokenInputFragment)
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()

        val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val defaultDisplay = windowManager.defaultDisplay

        val (widthPixels, heightPixels) = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val windowMetrics = windowManager.currentWindowMetrics
                val bounds: Rect = windowMetrics.bounds
                Pair(bounds.width().toDouble(), bounds.height().toDouble())
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val outMetrics = DisplayMetrics()
                defaultDisplay.getRealMetrics(outMetrics)
                Pair(outMetrics.widthPixels.toDouble(), outMetrics.heightPixels.toDouble())
            }

            else -> {
                val outMetrics = DisplayMetrics()
                defaultDisplay.getMetrics(outMetrics)
                Pair(outMetrics.widthPixels.toDouble(), outMetrics.heightPixels.toDouble())
            }
        }

        val dialogWidth = widthPixels * 0.8

        val params: WindowManager.LayoutParams? = dialog?.window?.attributes
        params?.width = dialogWidth.toInt()
        dialog?.window?.attributes = params as WindowManager.LayoutParams
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}