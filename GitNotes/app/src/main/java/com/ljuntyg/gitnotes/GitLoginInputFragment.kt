package com.ljuntyg.gitnotes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ljuntyg.gitnotes.databinding.FragmentGitLoginInputBinding

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

        // Initialize class members
        arguments?.let {
            cardText1 = it.getString(CARD_TEXT_1) ?: ""
            cardText2 = it.getString(CARD_TEXT_2) ?: ""
            hint1 = it.getString(HINT_1) ?: ""
            hint2 = it.getString(HINT_2) ?: ""
            hint3 = it.getString(HINT_3) ?: ""
            text1 = it.getString(TEXT_1) ?: ""
            buttonText = it.getString(BUTTON_TEXT) ?: ""
            fragmentType = FragmentType.valueOf(it.getString(FRAGMENT_TYPE) ?: "CloneRemote")
        }

        // Get reference to UserProfilesViewModel
        val userProfilesDao = ProfilesReposDatabase.getDatabase(requireActivity().applicationContext).userProfilesDao()
        val repositoriesDao = ProfilesReposDatabase.getDatabase(requireActivity().applicationContext).repositoriesDao()
        val profilesReposRepository = ProfilesReposRepository(userProfilesDao, repositoriesDao)
        val userProfilesViewModelFactory = UserProfilesViewModelFactory(requireActivity().application, profilesReposRepository)
        userProfilesViewModel = ViewModelProvider(requireActivity(), userProfilesViewModelFactory)[UserProfilesViewModel::class.java]

        val editText1 = binding.textInputLayoutLoginInput1.editText!!
        val editText2 = binding.textInputLayoutLoginInput2.editText!!
        val editText3 = binding.textInputLayoutLoginInput3.editText!!

        when (fragmentType) {
            FragmentType.CloneRemote -> { // Define the layout for login and remote clone
                binding.textInputLayoutLoginInput3.visibility = View.VISIBLE

                binding.textFieldLoginInput1.text = cardText1
                binding.textFieldLoginInput2.text = cardText2
                editText1.hint = hint1
                editText2.hint = hint2
                editText3.hint = hint3
                binding.buttonLoginInput.text = buttonText

                editText2.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                    override fun afterTextChanged(p0: Editable?) {}

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        binding.textInputLayoutLoginInput2.validateToken()
                    }
                })

                editText3.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                    override fun afterTextChanged(p0: Editable?) {}

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        binding.textInputLayoutLoginInput3.validateLink()
                    }
                })
            }

            FragmentType.CreateLocal -> { // Define the layout for creating a new local repository
                binding.textInputLayoutLoginInput3.visibility = View.GONE

                binding.textFieldLoginInput1.text = cardText1
                binding.textFieldLoginInput2.text = cardText2
                binding.textInputLayoutLoginInput1.editText!!.hint = hint1
                binding.textInputLayoutLoginInput2.editText!!.hint = hint2
                binding.buttonLoginInput.text = buttonText
            }

            FragmentType.ProvideToken -> { // Define layout for providing PAT
                binding.textInputLayoutLoginInput3.visibility = View.GONE

                binding.textFieldLoginInput1.text = cardText1
                binding.textFieldLoginInput2.text = cardText2
                binding.textInputLayoutLoginInput1.editText!!.setText(text1)
                binding.textInputLayoutLoginInput1.editText!!.isEnabled = false
                binding.textInputLayoutLoginInput2.editText!!.hint = "Personal Access Token"
                binding.buttonLoginInput.text = buttonText

                editText2.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                    override fun afterTextChanged(p0: Editable?) {
                        binding.textInputLayoutLoginInput2.validateToken()
                    }
                })
            }
        }

        binding.buttonLoginInput.setOnClickListener {
            when (fragmentType) {
                FragmentType.CloneRemote -> { // Remote repository to clone
                    // TODO: Implement cloning
                }

                FragmentType.CreateLocal -> { // New local repository to create
                    val profileName = binding.textInputLayoutLoginInput1.editText!!.text.toString()
                    val repoName = binding.textInputLayoutLoginInput2.editText!!.text.toString()

                    if (profileName.isEmpty() || repoName.isEmpty()) {
                        view.showShortSnackbar("Please fill in all fields")

                        return@setOnClickListener
                    }

                    val newRepository = Repository(profileName = profileName, name = repoName, httpsLink = "")
                    val newProfile = UserProfile(profileName, mutableListOf(newRepository))

                    userProfilesViewModel.insert(newProfile)
                    userProfilesViewModel.setSelectedUserProfile(newProfile)
                    userProfilesViewModel.loggedIn = true
                    userProfilesViewModel.selectedUserPrefs.insertOrReplace(profileName, "")

                    requireActivity().findViewById<View>(android.R.id.content).showShortSnackbar("Created new local repository")

                    parentFragmentManager.findFragmentByTag("GitLoginFragment")?.let {
                        (it as DialogFragment).dismiss()
                    }
                }

                FragmentType.ProvideToken -> {
                    val token = binding.textInputLayoutLoginInput2.editText!!.text.toString()
                    if (token.isPersonalAccessToken()) {
                        val userProfileName = userProfilesViewModel.selectedUserProfile.value?.profileName ?: return@setOnClickListener
                        userProfilesViewModel.selectedUserPrefs.insertOrReplace(userProfileName, token)

                        view.showShortSnackbar("Successfully registered token for $userProfileName")

                        // TODO: Figure out way to automatically get back to handling dialog fragment
                    } else {
                        view.showShortSnackbar("Not a valid token")
                    }
                }
            }
        }
    }

    companion object {
        private const val CARD_TEXT_1 = "cardText1"
        private const val CARD_TEXT_2 = "cardText2"
        private const val HINT_1 = "hint1"
        private const val HINT_2 = "hint2"
        private const val HINT_3 = "hint3"
        private const val TEXT_1 = "text1"
        private const val BUTTON_TEXT = "buttonText"
        private const val FRAGMENT_TYPE = "fragmentType"

        fun newInstanceRemoteLogin(cardText1: String, cardText2: String, hint1: String, hint2: String, hint3: String,
                                   buttonText: String) = GitLoginInputFragment().apply {
            arguments = Bundle().apply {
                putString(CARD_TEXT_1, cardText1)
                putString(CARD_TEXT_2, cardText2)
                putString(HINT_1, hint1)
                putString(HINT_2, hint2)
                putString(HINT_3, hint3)
                putString(TEXT_1, "")
                putString(BUTTON_TEXT, buttonText)
                putSerializable(FRAGMENT_TYPE, FragmentType.CloneRemote)
            }
        }

        fun newInstanceNewLocal(cardText1: String, cardText2: String, hint1: String, hint2: String,
                                buttonText: String) = GitLoginInputFragment().apply {
            arguments = Bundle().apply {
                putString(CARD_TEXT_1, cardText1)
                putString(CARD_TEXT_2, cardText2)
                putString(HINT_1, hint1)
                putString(HINT_2, hint2)
                putString(HINT_3, "")
                putString(TEXT_1, "")
                putString(BUTTON_TEXT, buttonText)
                putSerializable(FRAGMENT_TYPE, FragmentType.CreateLocal)
            }
        }

        fun newInstanceProvideToken(cardText1: String, cardText2: String, text1: String,
                                    buttonText: String) = GitLoginInputFragment().apply {
            arguments = Bundle().apply {
                putString(CARD_TEXT_1, cardText1)
                putString(CARD_TEXT_2, cardText2)
                putString(HINT_1, "")
                putString(HINT_2, "")
                putString(HINT_3, "")
                putString(TEXT_1, text1)
                putString(BUTTON_TEXT, buttonText)
                putString(FRAGMENT_TYPE, fragmentType.name)
            }
        }
    }
}

