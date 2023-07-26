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
import com.ljuntyg.gitnotes.databinding.FragmentGitHandlingBinding

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
        binding.spinnerHandling.adapter = adapter

        userProfilesViewModel.selectedUserRepositories.observe(viewLifecycleOwner) { repositories ->
            // Update the data for the spinner
            val updatedData = mutableListOf("New repository")
            updatedData.addAll(repositories.map { repository -> repository.name })

            // Update the spinner
            adapter.clear()
            adapter.addAll(updatedData)

            // Set spinner to selected repository if there is one
            val selectedRepository = userProfilesViewModel.selectedRepository.value
            val selectedUserProfile = userProfilesViewModel.selectedUserProfile.value
            if (selectedRepository != null && selectedRepository.profileName == selectedUserProfile?.profileName) {
                if (selectedRepository.name == "NEW REPOSITORY") { // If NEW REPOSITORY repo selected, then set spinner position to 0
                    binding.spinnerHandling.setSelection(0)
                } else { // Else find the position of the selected repository in the updated data list
                    val selectedPos = updatedData.indexOf(selectedRepository.name)
                    if (selectedPos != -1) {
                        binding.spinnerHandling.setSelection(selectedPos)
                    }
                }
            }
        }

        val createRepoFragment = GitHandlingCreateFragment()
        val manageRepoFragment = GitHandlingManageFragment()

        // Set spinner on item selected listener
        binding.spinnerHandling.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (position == 0) { // "New repository" selected
                    childFragmentManager.beginTransaction()
                        .replace(R.id.container_handling, createRepoFragment)
                        .commit()

                    userProfilesViewModel.setSelectedRepository(
                        userProfilesViewModel.selectedUserRepositories.value?.find {
                                repo -> repo.name == "NEW REPOSITORY"
                        } ?: Repository(
                            profileName = userProfilesViewModel.selectedUserProfile.value!!.profileName,
                            name = "NEW REPOSITORY",
                            httpsLink = ""
                        )
                    )
                } else { // Some repository selected
                    val selectedRepository = userProfilesViewModel.selectedUserRepositories.value?.find {
                        repo -> repo.name == binding.spinnerHandling.selectedItem.toString()
                    }

                    // TODO: Will this behave well on null?
                    userProfilesViewModel.setSelectedRepository(selectedRepository ?: return)

                    childFragmentManager.beginTransaction()
                        .replace(R.id.container_handling, manageRepoFragment)
                        .commit()
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
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