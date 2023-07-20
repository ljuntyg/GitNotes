package com.example.gitnotes

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.gitnotes.databinding.FragmentGitLoginBinding
import com.example.gitnotes.databinding.FragmentGitLoginInputBinding

class GitLoginInputFragment : Fragment() {
    private var _binding: FragmentGitLoginInputBinding? = null
    private val binding get() = _binding!!

    private lateinit var userProfilesViewModel: UserProfilesViewModel

    private lateinit var cardText1: String
    private lateinit var cardText2: String
    private lateinit var hint1: String
    private lateinit var hint2: String
    private lateinit var buttonText: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGitLoginInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textViewCardViewLoginTitle.text = cardText1
        binding.textViewCardViewLoginBody.text = cardText2
        binding.editTextLogin1.hint = hint1
        binding.editTextLogin2.hint = hint2
        binding.confirmButton.text = buttonText

        // Get reference to UserProfilesViewModel
        val userProfilesDao = ProfilesReposDatabase.getDatabase(requireActivity().applicationContext).userProfilesDao()
        val repositoriesDao = ProfilesReposDatabase.getDatabase(requireActivity().applicationContext).repositoriesDao()
        val profilesReposRepository = ProfilesReposRepository(userProfilesDao, repositoriesDao)
        val userProfilesViewModelFactory = UserProfilesViewModelFactory(profilesReposRepository)
        userProfilesViewModel = ViewModelProvider(requireActivity(), userProfilesViewModelFactory)[UserProfilesViewModel::class.java]
    }

    companion object {
        fun newInstance(cardText1: String, cardText2: String, hint1: String, hint2: String, buttonText: String) = GitLoginInputFragment().apply {
            this.cardText1 = cardText1
            this.cardText2 = cardText2
            this.hint1 = hint1
            this.hint2 = hint2
            this.buttonText = buttonText
        }
    }
}
