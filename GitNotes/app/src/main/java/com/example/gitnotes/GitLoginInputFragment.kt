package com.example.gitnotes

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.gitnotes.databinding.FragmentGitLoginBinding
import com.example.gitnotes.databinding.FragmentGitLoginInputBinding
import com.google.android.material.snackbar.Snackbar

class GitLoginInputFragment : Fragment() {
    private var _binding: FragmentGitLoginInputBinding? = null
    private val binding get() = _binding!!

    private lateinit var userProfilesViewModel: UserProfilesViewModel

    private lateinit var cardText1: String
    private lateinit var cardText2: String
    private lateinit var hint1: String
    private lateinit var hint2: String
    private lateinit var hint3: String
    private lateinit var text1: String
    private lateinit var buttonText: String

    private lateinit var fragmentType: FragmentType
    private enum class FragmentType {
        CloneRemote,
        CreateLocal,
        ProvideToken
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGitLoginInputBinding.inflate(inflater, container, false)
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

        when (fragmentType) {
            FragmentType.CloneRemote -> { // Define the layout for login and remote clone
                binding.editTextLoginInput3.visibility = View.VISIBLE
                binding.textFieldLoginInput1.text = cardText1
                binding.textFieldLoginInput2.text = cardText2
                binding.editTextLoginInput1.hint = hint1
                binding.editTextLoginInput2.hint = hint2
                binding.editTextLoginInput3.hint = hint3
                binding.buttonLoginInput.text = buttonText
            }

            FragmentType.CreateLocal -> { // Define the layout for creating a new local repository
                binding.editTextLoginInput3.visibility = View.GONE

                binding.textFieldLoginInput1.text = cardText1
                binding.textFieldLoginInput2.text = cardText2
                binding.editTextLoginInput1.hint = hint1
                binding.editTextLoginInput2.hint = hint2
                binding.buttonLoginInput.text = buttonText
            }

            FragmentType.ProvideToken -> { // Define layout for providing PAT
                binding.editTextLoginInput3.visibility = View.GONE

                binding.textFieldLoginInput1.text = cardText1
                binding.textFieldLoginInput2.text = cardText2
                binding.editTextLoginInput1.setText(text1)
                binding.editTextLoginInput1.isEnabled = false
                binding.editTextLoginInput2.hint = "Personal Access Token"
                binding.buttonLoginInput.text = buttonText
            }
        }

        binding.buttonLoginInput.setOnClickListener {
            when (fragmentType) {
                FragmentType.CloneRemote -> { // Remote repository to clone
                    // TODO: Implement cloning
                }

                FragmentType.CreateLocal -> { // New local repository to create
                    val profileName = binding.editTextLoginInput1.text.toString()
                    val repoName = binding.editTextLoginInput2.text.toString()

                    if (profileName.isEmpty() || repoName.isEmpty()) {
                        Snackbar.make(
                            view,
                            "Please fill in all fields",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    val newRepository = Repository(profileName = profileName, name = repoName, httpsLink = "")
                    val newProfile = UserProfile(profileName, mutableListOf(newRepository))

                    userProfilesViewModel.insert(newProfile)
                    userProfilesViewModel.setSelectedUserProfile(newProfile)
                    userProfilesViewModel.loggedIn = true
                    userProfilesViewModel.selectedUserPrefs.insertOrReplace(profileName, "")

                    Snackbar.make(
                        requireActivity().findViewById(android.R.id.content),
                        "Created new local repository",
                        Snackbar.LENGTH_SHORT
                    ).show()

                    parentFragmentManager.findFragmentByTag("GitLoginFragment")?.let {
                        (it as DialogFragment).dismiss()
                    }
                }

                FragmentType.ProvideToken -> {
                    // TODO: Implement actions for ProvideToken
                }
            }
        }
    }

    companion object {
        fun newInstanceRemoteLogin(cardText1: String, cardText2: String, hint1: String, hint2: String, hint3: String,
                                   buttonText: String) = GitLoginInputFragment().apply {
            this.cardText1 = cardText1
            this.cardText2 = cardText2
            this.hint1 = hint1
            this.hint2 = hint2
            this.hint3 = hint3
            this.text1 = ""
            this.buttonText = buttonText
            this.fragmentType = FragmentType.CloneRemote
        }

        fun newInstanceNewLocal(cardText1: String, cardText2: String, hint1: String, hint2: String,
                                buttonText: String) = GitLoginInputFragment().apply {
            this.cardText1 = cardText1
            this.cardText2 = cardText2
            this.hint1 = hint1
            this.hint2 = hint2
            this.hint3 = ""
            this.text1 = ""
            this.buttonText = buttonText
            this.fragmentType = FragmentType.CreateLocal
        }

        fun newInstanceProvideToken(cardText1: String, cardText2: String, text1: String,
                                    buttonText: String) = GitLoginInputFragment().apply {
            this.cardText1 = cardText1
            this.cardText2 = cardText2
            this.hint1 = ""
            this.hint2 = ""
            this.hint3 = ""
            this.text1 = text1
            this.buttonText = buttonText
            this.fragmentType = FragmentType.ProvideToken
        }
    }
}

