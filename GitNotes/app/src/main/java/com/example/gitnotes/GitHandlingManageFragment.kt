package com.example.gitnotes

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.gitnotes.databinding.FragmentGitHandlingManageBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.PullCommand
import org.eclipse.jgit.api.PushCommand
import org.eclipse.jgit.lib.ConfigConstants
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.lib.Repository as JRepository
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.util.FS
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter

class GitHandlingManageFragment(private var selectedRepository: Repository) : Fragment() {
    private var _binding: FragmentGitHandlingManageBinding? = null
    private val binding get() = _binding!!

    private lateinit var userProfilesViewModel: UserProfilesViewModel
    private lateinit var notesViewModel: NotesViewModel

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
        val userProfilesDao = ProfilesReposDatabase.getDatabase(requireActivity().applicationContext).userProfilesDao()
        val repositoriesDao = ProfilesReposDatabase.getDatabase(requireActivity().applicationContext).repositoriesDao()
        val profilesReposRepository = ProfilesReposRepository(userProfilesDao, repositoriesDao)
        val userProfilesViewModelFactory = UserProfilesViewModelFactory(requireActivity().application, profilesReposRepository)
        userProfilesViewModel = ViewModelProvider(requireActivity(), userProfilesViewModelFactory)[UserProfilesViewModel::class.java]

        // Get reference to NotesViewModel
        val notesDao = NotesDatabase.getDatabase(requireActivity().applicationContext).notesDao()
        val notesRepository = NotesRepository(notesDao)
        val notesViewModelFactory = NotesViewModelFactory(notesRepository)
        notesViewModel = ViewModelProvider(requireActivity(), notesViewModelFactory)[NotesViewModel::class.java]

        // TODO: Implement selectedRepository livedata to ViewModel to share info between GitHandlingFragment spinner and this GitHandlingManagerFragment
        userProfilesViewModel.selectedUserRepositories.observe(viewLifecycleOwner) { repos ->
            if (repos.isNullOrEmpty()) {
                return@observe
            } else {
                selectedRepository = repos.last()
            }
        }

        if (selectedRepository.httpsLink.isEmpty()) {
            binding.editTextHandlingManage1.setText(R.string.no_repo_link)
            binding.linearLayoutHandlingManage.visibility = View.GONE
        } else {
            binding.editTextHandlingManage1.setText(selectedRepository.httpsLink)
            binding.linearLayoutHandlingManage.visibility = View.VISIBLE
        }

        // Push button
        binding.buttonHandlingManage1.setOnClickListener {
            lifecycleScope.launch {
                if (userProfilesViewModel.selectedUserPrefs.getCredentials().first == userProfilesViewModel.selectedUserProfile.value?.profileName
                    && userProfilesViewModel.selectedUserPrefs.getCredentials().second?.isNotEmpty() == true
                ) { // Possibly valid token exists for user
                    Log.d("MYLOG", "Credentials matching user found")

                    val jgitRepo = getOrCreateJGitRepository(selectedRepository)
                    createNoteFiles(jgitRepo, notesViewModel.allNotes.value!!)

                    Snackbar.make(
                        view,
                        "Pushing repository...",
                        Snackbar.LENGTH_SHORT
                    ).show()

                    if ((addAndCommitToJGit(jgitRepo, DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
                        && pushJGitToRemote(jgitRepo, selectedRepository.httpsLink, userProfilesViewModel.selectedUserPrefs.getCredentials().second!!)))
                    {
                        Snackbar.make(
                            view,
                            "Successfully pushed repository",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } else {
                        Snackbar.make(
                            view,
                            "Failed to push repository",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                } else { // Request token from user
                    // Open login fragment
                    val loginFragment = GitLoginFragment(selectedRepository)
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

                    Snackbar.make(
                        view,
                        "Pulling repository...",
                        Snackbar.LENGTH_SHORT
                    ).show()

                    if (pullToJGitFromRemote(jgitRepo, selectedRepository.httpsLink, userProfilesViewModel.selectedUserPrefs.getCredentials().second!!)) {
                        Snackbar.make(
                            view,
                            "Successfully pulled repository",
                            Snackbar.LENGTH_SHORT
                        ).show()

                        initNotesFromFiles(jgitRepo)
                    } else {
                        Snackbar.make(
                            view,
                            "Failed to pull repository",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                } else { // Request token from user
                    // Open login fragment
                    val loginFragment = GitLoginFragment(selectedRepository)
                    loginFragment.show(requireActivity().supportFragmentManager, "GitLoginFragment")
                }
            }
        }

        // Delete button
        binding.buttonHandlingManage3.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val deletionSuccessful = userProfilesViewModel.deleteRepoForUserAsync(selectedRepository, userProfilesViewModel.selectedUserProfile.value!!).await()
                if (deletionSuccessful) {
                    Snackbar.make(
                        view,
                        "Deleted repository ${selectedRepository.name}",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    Snackbar.make(
                        view,
                        "Failed to delete repository",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }

                binding.editTextHandlingManage1.setText("")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // TODO: Change to take Git object instead of File object
    /** fun isGitNotesRepository(dir: File): Boolean {
        val files = dir.listFiles()
        if (files == null || files.isEmpty()) return true

        return files.filter { it.isFile && !it.name.startsWith(".git") }
            .all { file ->
                file.extension == "txt" && file.bufferedReader().use { it.readLine() }.startsWith("Title: ")
            }
    } */

    private suspend fun createNoteFiles(jgit: Git, notes: List<Note>) = withContext(Dispatchers.IO) {
        val dir = jgit.repository.directory.parentFile
        val noteFileNames = notes.map { "${it.id}.txt" }.toSet()

        dir.listFiles()?.forEach { file ->
            if (file.name !in noteFileNames) {
                file.delete()
                jgit.rm().addFilepattern(file.name).call()
            }
        }

        for (note in notes) {
            val noteFile = File(dir, "${note.id}.txt")
            if (!noteFile.exists()) {
                noteFile.writeText("Title: ${note.title}\n\n${note.body}")
                jgit.add().addFilepattern(noteFile.name).call()
            }
        }
    }

    private suspend fun initNotesFromFiles(jgit: Git) = withContext(Dispatchers.IO) {
        val repoDir = jgit.repository.directory.parentFile

        // Delete all current notes
        notesViewModel.allNotes.value?.forEach { note ->
            notesViewModel.delete(note)
        }

        // Insert all notes from the Git repository
        repoDir?.listFiles()?.forEach { file ->
            if (file.isFile && file.extension == "txt") {
                val content = file.readText()

                val noteId = file.nameWithoutExtension.toLongOrNull() // Parse ID from filename
                val noteTitle = content.substringAfter("Title: ").substringBefore("\n") // Parse note title
                val noteBody = content.substringAfter("\n\n") // Parse note body

                if (noteId != null) {
                    val note = Note(noteId, noteTitle, noteBody)
                    notesViewModel.insertAsync(note)
                }
            }
        }
    }

    private suspend fun getOrCreateJGitRepository(repo: Repository): Git = withContext(Dispatchers.IO) {
        val repoDir = File(requireActivity().filesDir, repo.name)

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

    // TODO: Snackbar for exception?
    private suspend fun addAndCommitToJGit(jgit: Git, commitMessage: String): Boolean = withContext(
        Dispatchers.IO) {
        return@withContext try {
            jgit.add().addFilepattern(".").call() // TODO: Remove? Already using add/rm in createNoteFiles, if so rename this method to only commit

            val commit = jgit.commit().setMessage(commitMessage).call()

            commit != null
        } catch (e: Exception) {
            Log.d("MYLOG", "Exception when adding/committing: $e")
            false
        }
    }

    // TODO: Snackbar for exception?
    private suspend fun pushJGitToRemote(jgit: Git, httpsLink: String, token: String): Boolean = withContext(
        Dispatchers.IO) {
        return@withContext try {
            val pushCommand: PushCommand = jgit.push()
            pushCommand.remote = httpsLink
            pushCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
            // pushCommand.isForce = true
            pushCommand.setPushAll()

            val results = pushCommand.call()

            var success = true
            for (result in results) {
                for (update in result.remoteUpdates) {
                    if (update.status != RemoteRefUpdate.Status.OK &&
                        update.status != RemoteRefUpdate.Status.UP_TO_DATE) {
                        success = false
                        break
                    }
                }
            }
            success
        } catch (e: Exception) {
            Log.d("MYLOG", "Exception when pushing: $e")
            false
        }
    }

    // TODO: Snackbar for exception?
    private suspend fun pullToJGitFromRemote(jgit: Git, httpsLink: String, token: String): Boolean = withContext(
        Dispatchers.IO) {
        return@withContext try {
            val repoConfig = jgit.repository.config
            repoConfig.setString(ConfigConstants.CONFIG_REMOTE_SECTION, "origin", "url", httpsLink)
            repoConfig.setString(ConfigConstants.CONFIG_REMOTE_SECTION, "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*")
            repoConfig.save()

            val pullCommand: PullCommand = jgit.pull()
            pullCommand.remote = "origin"
            pullCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))

            val pullResult = pullCommand.call()

            pullResult.mergeResult.mergeStatus.isSuccessful
        } catch (e: Exception) {
            Log.d("MYLOG", "Exception when pulling: $e")
            false
        }
    }
}