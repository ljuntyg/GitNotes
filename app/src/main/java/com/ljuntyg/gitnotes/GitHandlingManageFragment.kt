package com.ljuntyg.gitnotes

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.ljuntyg.gitnotes.databinding.FragmentGitHandlingManageBinding
import kotlinx.coroutines.launch
import com.ljuntyg.gitnotes.GitHandler.GitResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git

class GitHandlingManageFragment : Fragment() {
    private var _binding: FragmentGitHandlingManageBinding? = null
    private val binding get() = _binding!!

    private lateinit var userProfilesViewModel: UserProfilesViewModel
    private lateinit var notesViewModel: NotesViewModel
    private lateinit var selectedRepository: Repository
    private lateinit var gitHandler: GitHandler

    private var textWatcher: TextWatcher? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentGitHandlingManageBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O) // For timestamp passed to commit string
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

        // Get reference to NotesViewModel
        val notesDao = NotesDatabase.getDatabase(requireActivity().applicationContext).notesDao()
        val notesRepository = NotesRepository(notesDao)
        val notesViewModelFactory = NotesViewModelFactory(notesRepository)
        notesViewModel =
            ViewModelProvider(requireActivity(), notesViewModelFactory)[NotesViewModel::class.java]

        gitHandler = GitHandler(requireActivity().applicationContext, notesViewModel)

        // TODO: Messy cursor on selection, sometimes visible sometimes not
        // Keeps selectedRepository up-to-date based on e.g. changes based on spinner in GitHandlingFragment
        selectedRepository = userProfilesViewModel.selectedRepository.value!!
        userProfilesViewModel.selectedRepository.observe(viewLifecycleOwner) {
            selectedRepository = it

            val editText = binding.textInputLayoutHandlingManage.editText!!
            val selectedRepository = userProfilesViewModel.selectedRepository.value!!

            // Clear any previous focus and hide any previously active keyboard on the editText
            editText.clearFocus()
            val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(editText.windowToken, 0)

            // Remove any previous textWatcher, it will be recreated after initial text checks
            editText.removeTextChangedListener(textWatcher)

            // Check the text of this new selected repository and set EditText accordingly
            if (selectedRepository.httpsLink.isValidHTTPSlink()) { // Valid HTTPS link
                val remoteName = selectedRepository.httpsLink.trim().getRepoNameFromUrl()
                val localName = selectedRepository.name.trim()

                if (remoteName == localName) {
                    // All good, set text to link and select end of text to ensure repo name is seen
                    editText.setText(selectedRepository.httpsLink)
                    editText.setSelection(editText.text.length)
                    binding.linearLayoutHandlingManage.visibility = View.VISIBLE
                    binding.textInputLayoutHandlingManage.isErrorEnabled = false
                } else {
                    // Repo name does not match local name, no good, show link with error
                    editText.setText(selectedRepository.httpsLink)
                    editText.setSelection(editText.text.length)
                    binding.linearLayoutHandlingManage.visibility = View.GONE
                    binding.textInputLayoutHandlingManage.error = getString(
                        R.string.repo_name_mismatch,
                        remoteName,
                        localName
                    )
                }
            } else { // Not a valid HTTPS link
                if (selectedRepository.httpsLink.isNotEmpty()) {
                    // Invalid HTTPS link, show text with error
                    editText.setText(selectedRepository.httpsLink)
                    editText.setSelection(editText.text.length)
                    binding.linearLayoutHandlingManage.visibility = View.GONE
                    binding.textInputLayoutHandlingManage.error = getString(R.string.invalid_link)
                } else {
                    // No text at all, show hint
                    editText.setText("")
                    editText.hint = getString(R.string.no_repo_link)
                    binding.linearLayoutHandlingManage.visibility = View.GONE
                    binding.textInputLayoutHandlingManage.isErrorEnabled = false
                }
            }

            // Reinitialize textWatcher
            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                // Do repeat checks according to the logic above every time the text changes
                override fun afterTextChanged(p0: Editable?) {
                    val newRepoLink = p0.toString()
                    selectedRepository.httpsLink = newRepoLink
                    userProfilesViewModel.viewModelScope.launch {
                        userProfilesViewModel.updateRepoForUserAsync(
                            selectedRepository,
                            userProfilesViewModel.selectedUserProfile.value!!
                        )
                    }
                    if (newRepoLink.isValidHTTPSlink()) {
                        val remoteName = newRepoLink.trim().getRepoNameFromUrl()
                        val localName = selectedRepository.name.trim()

                        if (remoteName == localName) {
                            binding.linearLayoutHandlingManage.visibility = View.VISIBLE
                            binding.textInputLayoutHandlingManage.isErrorEnabled = false
                        } else {
                            binding.linearLayoutHandlingManage.visibility = View.GONE
                            binding.textInputLayoutHandlingManage.error = getString(
                                R.string.repo_name_mismatch,
                                remoteName,
                                localName
                            )
                        }
                    } else {
                        if (newRepoLink.isNotEmpty()) {
                            binding.linearLayoutHandlingManage.visibility = View.GONE
                            binding.textInputLayoutHandlingManage.error =
                                getString(R.string.invalid_link)
                        } else {
                            editText.hint = getString(R.string.no_repo_link)
                            binding.linearLayoutHandlingManage.visibility = View.GONE
                            binding.textInputLayoutHandlingManage.isErrorEnabled = false
                        }
                    }
                }
            }

            // Add textWatcher to editText
            editText.addTextChangedListener(textWatcher)
        }

        // Push button
        binding.buttonHandlingManage1.setOnClickListener {
            lifecycleScope.launch {
                if (userProfilesViewModel.selectedUserPrefs.getCredentials().first == userProfilesViewModel.selectedUserProfile.value?.profileName
                    && userProfilesViewModel.selectedUserPrefs.getCredentials().second?.isNotEmpty() == true
                ) { // Possibly valid token exists for user
                    Log.d("MYLOG", "Credentials matching user found")

                    val jgitRepo = gitHandler.getOrCreateJGitRepository(selectedRepository)
                    view.showIndefiniteSnackbar(getString(R.string.pushing_repo))
                    gitHandler.createNoteFiles(jgitRepo, notesViewModel.allNotes.value!!)

                    val commitResult = gitHandler.commitToJGit(jgitRepo, System.currentTimeMillis().toFormattedDateString())
                    val pushResult = gitHandler.verifyAndPush(jgitRepo, selectedRepository.httpsLink, userProfilesViewModel.selectedUserPrefs.getCredentials().second!!)

                    when (commitResult) {
                        is GitResult.Success -> {
                            when (pushResult) {
                                is GitResult.Success -> {
                                    view.showShortSnackbar(getString(R.string.success_push))
                                }
                                is GitResult.Failure -> {
                                    if (pushResult.exception == null) view.showShortSnackbar(getString(R.string.failed_push))
                                        else view.showShortSnackbar(getString(R.string.error, pushResult.exception.message))

                                    if (pushResult.needForce) {
                                        // Handle force push scenario with yes/no dialog
                                        val dialogFragment = WarningDialogFragment.newInstance(
                                            getString(R.string.push_failed_attempt_force),
                                            getString(R.string.push_attempt_force_explanation),
                                            getString(R.string.warning_force_push)
                                        )
                                        dialogFragment.setPositiveButtonListener {
                                            forcePush(jgitRepo, selectedRepository.httpsLink, userProfilesViewModel.selectedUserPrefs.getCredentials().second!!)
                                        }
                                        dialogFragment.show(requireActivity().supportFragmentManager, "WarningDialogFragment")
                                    }
                                }
                            }
                        }
                        is GitResult.Failure -> {
                            if (commitResult.exception == null) view.showShortSnackbar(getString(R.string.failed_commit))
                                else view.showShortSnackbar(getString(R.string.error, commitResult.exception.message))
                        }
                    }
                } else { // Request token from user
                    // Open login fragment
                    val loginFragment = GitLoginFragment()
                    loginFragment.show(requireActivity().supportFragmentManager, "GitLoginFragment")
                }
            }
        }

        // Pull button
        binding.buttonHandlingManage2.setOnClickListener {
            lifecycleScope.launch {
                if (userProfilesViewModel.selectedUserPrefs.getCredentials().first == userProfilesViewModel.selectedUserProfile.value?.profileName
                    && userProfilesViewModel.selectedUserPrefs.getCredentials().second?.isNotEmpty() == true
                ) { // Possibly valid token exists for user
                    Log.d("MYLOG", "Credentials matching user found")

                    val jgitRepo = gitHandler.getOrCreateJGitRepository(selectedRepository)
                    view.showIndefiniteSnackbar(getString(R.string.pulling_repo))

                    when (val pullResult = gitHandler.verifyAndPull(jgitRepo, selectedRepository.httpsLink, userProfilesViewModel.selectedUserPrefs.getCredentials().second!!)) {
                        is GitResult.Success -> {
                            view.showShortSnackbar(getString(R.string.success_pull))
                            gitHandler.initNotesFromFiles(jgitRepo)
                        }
                        is GitResult.Failure -> {
                            if (pullResult.exception == null) view.showShortSnackbar(getString(R.string.failed_pull))
                                else view.showShortSnackbar(getString(R.string.error, pullResult.exception.message))

                            if (pullResult.needForce) {
                                // TODO: Handle force pull scenario
                            }
                        }
                    }
                } else { // Request token from user
                    // Open login fragment
                    val loginFragment = GitLoginFragment()
                    loginFragment.show(requireActivity().supportFragmentManager, "GitLoginFragment")
                }
            }
        }

        // Delete button
        binding.buttonHandlingManage3.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val deletionSuccessful = userProfilesViewModel.deleteRepoForUserAsync(
                    selectedRepository,
                    userProfilesViewModel.selectedUserProfile.value!!
                ).await()
                if (deletionSuccessful) {
                    view.showShortSnackbar(
                        getString(
                            R.string.deleted_repo,
                            selectedRepository.name
                        )
                    )
                } else {
                    view.showShortSnackbar(getString(R.string.failed_delete_repo))
                }

                binding.textInputLayoutHandlingManage.editText!!.setText("")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun forcePush(jgit: Git, httpsLink: String, token: String) {
        lifecycleScope.launch {
            view?.showIndefiniteSnackbar(getString(R.string.pushing_repo))
            when (gitHandler.pushJGitToRemote(jgit, httpsLink, token, true)) {
                is GitResult.Success -> {
                    view?.showShortSnackbar(getString(R.string.success_push))
                }

                is GitResult.Failure -> {
                    view?.showShortSnackbar(getString(R.string.failed_push))
                }
            }
        }
    }

    // TODO: Not sure under what situation useful
    private fun forcePull(jgit: Git, httpsLink: String, token: String) {
        lifecycleScope.launch {
            view?.showIndefiniteSnackbar(getString(R.string.pulling_repo))
            when (gitHandler.pullToJGitFromRemote(jgit, httpsLink, token, true)) {
                is GitResult.Success -> {
                    view?.showShortSnackbar(getString(R.string.success_pull))
                }

                is GitResult.Failure -> {
                    view?.showShortSnackbar(getString(R.string.failed_pull))
                }
            }
        }
    }
}