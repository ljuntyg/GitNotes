package com.ljuntyg.gitnotes

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.PullCommand
import org.eclipse.jgit.api.PushCommand
import org.eclipse.jgit.lib.ConfigConstants
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.util.FS
import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.io.path.createTempDirectory
import org.eclipse.jgit.lib.Repository as JRepository

class GitHandlingManageFragment : Fragment() {
    private var _binding: FragmentGitHandlingManageBinding? = null
    private val binding get() = _binding!!

    private lateinit var userProfilesViewModel: UserProfilesViewModel
    private lateinit var notesViewModel: NotesViewModel
    private lateinit var selectedRepository: Repository

    private var textWatcher: TextWatcher? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
                if (selectedRepository.httpsLink.getRepoNameFromUrl() == selectedRepository.name.trim()) {
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
                        selectedRepository.httpsLink.getRepoNameFromUrl(),
                        selectedRepository.name.trim()
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
                        val repoName = newRepoLink.getRepoNameFromUrl()
                        if (repoName == selectedRepository.name.trim()) {
                            binding.linearLayoutHandlingManage.visibility = View.VISIBLE
                            binding.textInputLayoutHandlingManage.isErrorEnabled = false
                        } else {
                            binding.linearLayoutHandlingManage.visibility = View.GONE
                            binding.textInputLayoutHandlingManage.error = getString(
                                R.string.repo_name_mismatch,
                                selectedRepository.httpsLink.getRepoNameFromUrl(),
                                selectedRepository.name.trim()
                            )
                        }
                    } else {
                        if (newRepoLink.isNotEmpty()) {
                            binding.linearLayoutHandlingManage.visibility = View.GONE
                            binding.textInputLayoutHandlingManage.error = getString(R.string.invalid_link)
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

                    val jgitRepo = getOrCreateJGitRepository(selectedRepository)
                    view.showIndefiniteSnackbar("Pushing repository...")
                    createNoteFiles(jgitRepo, notesViewModel.allNotes.value!!)

                    if ((commitToJGit(jgitRepo, DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
                                && verifyAndPush(
                            jgitRepo,
                            selectedRepository.httpsLink,
                            userProfilesViewModel.selectedUserPrefs.getCredentials().second!!
                        ))
                    ) {
                        view.showShortSnackbar("Successfully pushed repository")
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
                    // TODO: Pull with credentials

                    val jgitRepo = getOrCreateJGitRepository(selectedRepository)
                    view.showIndefiniteSnackbar("Pulling repository...")

                    if (verifyAndPull(
                            jgitRepo,
                            selectedRepository.httpsLink,
                            userProfilesViewModel.selectedUserPrefs.getCredentials().second!!
                        )
                    ) {
                        view.showShortSnackbar("Successfully pulled repository")

                        initNotesFromFiles(jgitRepo)
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
                    view.showShortSnackbar("Deleted repository ${selectedRepository.name}")
                } else {
                    view.showShortSnackbar("Failed to delete repository")
                }

                binding.textInputLayoutHandlingManage.editText!!.setText("")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun isGitNotesRepository(git: Git): Boolean {
        val dir = git.repository.directory.parentFile

        val files = dir.listFiles()
        if (files == null || files.isEmpty()) return true

        return files.filter { it.isFile && !it.name.startsWith(".git") }
            .all { file ->
                file.extension == "txt" && file.bufferedReader().use { it.readLine() }
                    .startsWith("Title: ")
            }
    }

    private suspend fun createNoteFiles(jgit: Git, notes: List<Note>) =
        withContext(Dispatchers.IO) {
            val dir = jgit.repository.directory.parentFile
            val noteFileNames = notes.map { "${it.id}.txt" }.toSet()

            dir.listFiles()?.forEach { file ->
                if (file.name !in noteFileNames) {
                    jgit.rm().addFilepattern(file.name).call()
                }
            }

            for (note in notes) {
                val noteFile = File(dir, "${note.id}.txt")
                noteFile.writeText("Title: ${note.title}\n\n${note.body}")
                jgit.add().addFilepattern(noteFile.name).call()
            }
        }

    private suspend fun initNotesFromFiles(jgit: Git) = withContext(Dispatchers.IO) {
        val repoDir = jgit.repository.directory.parentFile

        // Delete all current notes (they will be re-initiated below)
        notesViewModel.allNotes.value?.forEach { note ->
            notesViewModel.delete(note)
        }

        // Insert all notes from the Git repository
        repoDir?.listFiles()?.forEach { file ->
            if (file.isFile && file.extension == "txt") {
                val content = file.readText()

                val noteId = file.nameWithoutExtension.toLongOrNull() // Parse ID from filename
                val noteTitle =
                    content.substringAfter("Title: ").substringBefore("\n") // Parse note title
                val noteBody = content.substringAfter("\n\n") // Parse note body

                if (noteId != null) {
                    val note = Note(noteId, noteTitle, noteBody)
                    notesViewModel.insertAsync(note)
                }
            }
        }
    }

    private suspend fun getOrCreateJGitRepository(repo: Repository): Git =
        withContext(Dispatchers.IO) {
            val repoDir = File(requireActivity().filesDir, repo.name.trim())

            if (!repoDir.exists()) {
                repoDir.mkdirs()
            }

            val gitDir = File(repoDir, ".git")

            val repoExists = RepositoryCache.FileKey.isGitRepository(gitDir, FS.DETECTED)

            val repository: JRepository = if (repoExists) {
                RepositoryBuilder().setGitDir(gitDir).readEnvironment().findGitDir().build()
            } else {
                Git.init().setDirectory(repoDir).call().repository
            }

            return@withContext Git.wrap(repository)
        }

    private suspend fun commitToJGit(jgit: Git, commitMessage: String): Boolean = withContext(
        Dispatchers.IO
    ) {
        return@withContext try {
            val commit = jgit.commit().setMessage(commitMessage).call()

            commit != null
        } catch (e: Exception) {
            Log.d("MYLOG", "Exception when committing: $e")
            view?.showShortSnackbar("ERROR: ${e.message}")
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O) // For Path to File conversion
    private suspend fun verifyAndPush(jgit: Git, remoteLink: String, token: String): Boolean =
        withContext(Dispatchers.IO) {
            val tempDirPath: Path = createTempDirectory("gitnotes")
            val tempDir = tempDirPath.toFile()
            val tempJgit: Git

            try {
                // Clone the remote repo into a temporary local repo
                tempJgit = Git.cloneRepository()
                    .setURI(remoteLink)
                    .setDirectory(tempDir)
                    .setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
                    .call()

                if (!isGitNotesRepository(tempJgit)) {
                    // If the verification fails, delete the temporary repo and return false
                    tempJgit.repository.close()
                    tempDir.deleteRecursively()
                    view?.showShortSnackbar("ERROR: Remote is not a GitNotes repository")
                    return@withContext false
                }

                // If the verification succeeds, push the changes to the remote repo
                return@withContext pushJGitToRemote(jgit, remoteLink, token)
            } catch (e: Exception) {
                Log.d("MYLOG", "Exception when verifying and pushing: $e")
                view?.showShortSnackbar("ERROR: ${e.message}")
                return@withContext false
            }
        }

    private suspend fun pushJGitToRemote(jgit: Git, httpsLink: String, token: String): Boolean =
        withContext(
            Dispatchers.IO
        ) {
            return@withContext try {
                val pushCommand: PushCommand = jgit.push()
                pushCommand.remote = httpsLink
                pushCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
                pushCommand.setPushAll()

                val results = pushCommand.call()

                var success = true
                for (result in results) {
                    for (update in result.remoteUpdates) {
                        if (update.status != RemoteRefUpdate.Status.OK &&
                            update.status != RemoteRefUpdate.Status.UP_TO_DATE
                        ) {
                            success = false
                            break
                        }
                    }
                }
                success
            } catch (e: Exception) {
                Log.d("MYLOG", "Exception when pushing: $e")
                view?.showShortSnackbar("ERROR: ${e.message}")
                false
            }
        }

    @RequiresApi(Build.VERSION_CODES.O) // For Path to File conversion
    private suspend fun verifyAndPull(jgit: Git, remoteLink: String, token: String): Boolean =
        withContext(Dispatchers.IO) {
            val tempDirPath: Path = createTempDirectory("gitnotes")
            val tempDir = tempDirPath.toFile()
            val tempJgit: Git

            try {
                // Clone the remote repo into a temporary local repo
                tempJgit = Git.cloneRepository()
                    .setURI(remoteLink)
                    .setDirectory(tempDir)
                    .setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
                    .call()

                if (!isGitNotesRepository(tempJgit)) {
                    // If the verification fails, delete the temporary repo and return false
                    tempJgit.repository.close()
                    tempDir.deleteRecursively()
                    view?.showShortSnackbar("ERROR: Remote is not a GitNotes repository")
                    return@withContext false
                }

                // If the verification succeeds, pull the changes into the actual repo
                return@withContext pullToJGitFromRemote(jgit, remoteLink, token)
            } catch (e: Exception) {
                Log.d("MYLOG", "Exception when verifying and pulling: $e")
                view?.showShortSnackbar("ERROR: ${e.message}")
                return@withContext false
            }
        }

    private suspend fun pullToJGitFromRemote(jgit: Git, httpsLink: String, token: String): Boolean =
        withContext(
            Dispatchers.IO
        ) {
            return@withContext try {
                val repoConfig = jgit.repository.config
                repoConfig.setString(
                    ConfigConstants.CONFIG_REMOTE_SECTION,
                    "origin",
                    "url",
                    httpsLink
                )
                repoConfig.setString(
                    ConfigConstants.CONFIG_REMOTE_SECTION,
                    "origin",
                    "fetch",
                    "+refs/heads/*:refs/remotes/origin/*"
                )
                repoConfig.save()

                val pullCommand: PullCommand = jgit.pull()
                pullCommand.remote = "origin"
                pullCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))

                val pullResult = pullCommand.call()

                pullResult.mergeResult.mergeStatus.isSuccessful
            } catch (e: Exception) {
                Log.d("MYLOG", "Exception when pulling: $e")
                view?.showShortSnackbar("ERROR: ${e.message}")
                false
            }
        }
}