package com.ljuntyg.gitnotes

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
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
import kotlin.io.path.createTempDirectory

class GitHandler(private val appContext: Context, private val notesViewModel: NotesViewModel) {
    private val filesDir = appContext.filesDir

    data class GitResult(
        val success: Boolean,
        val exception: Exception?
    )

    inner class NotGitNotesRepositoryException : Exception(appContext.getString(R.string.remote_not_gitnotes_repo))

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

    suspend fun createNoteFiles(jgit: Git, notes: List<Note>) = withContext(Dispatchers.IO) {
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

    suspend fun initNotesFromFiles(jgit: Git) = withContext(Dispatchers.IO) {
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
                    note.id = notesViewModel.insertAsync(note).await()
                }
            }
        }
    }

    suspend fun getOrCreateJGitRepository(repo: Repository): Git = withContext(Dispatchers.IO) {
        val repoDir = File(filesDir, repo.name.trim())

        if (!repoDir.exists()) {
            repoDir.mkdirs()
        }

        val gitDir = File(repoDir, ".git")

        val repoExists = RepositoryCache.FileKey.isGitRepository(gitDir, FS.DETECTED)

        val repository: org.eclipse.jgit.lib.Repository = if (repoExists) {
            RepositoryBuilder().setGitDir(gitDir).readEnvironment().findGitDir().build()
        } else {
            Git.init().setDirectory(repoDir).call().repository
        }

        return@withContext Git.wrap(repository)
    }

    suspend fun commitToJGit(jgit: Git, commitMessage: String): GitResult = withContext(
        Dispatchers.IO
    ) {
        return@withContext try {
            val commit = jgit.commit().setMessage(commitMessage).call()

            GitResult(commit != null, null)
        } catch (e: Exception) {
            Log.d("MYLOG", "Exception when committing: $e")
            GitResult(false, e)
        }
    }

    // TODO: TransportException if there's no internet connection
    // TODO: Can't push unrelated local repository to unrelated remote
    @RequiresApi(Build.VERSION_CODES.O) // For Path to File conversion
    suspend fun verifyAndPush(jgit: Git, remoteLink: String, token: String): GitResult = withContext(Dispatchers.IO) {
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
                return@withContext GitResult(false, NotGitNotesRepositoryException())
            }

            // If the verification succeeds, push the changes to the remote repo
            return@withContext pushJGitToRemote(jgit, remoteLink, token)
        } catch (e: Exception) {
            Log.d("MYLOG", "Exception when verifying and pushing: $e")
            return@withContext GitResult(false, e)
        }
    }

    private suspend fun pushJGitToRemote(jgit: Git, httpsLink: String, token: String): GitResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val pushCommand: PushCommand = jgit.push()
            pushCommand.remote = httpsLink.trim()
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
            GitResult(success, null)
        } catch (e: Exception) {
            Log.d("MYLOG", "Exception when pushing: $e")
            GitResult(false, e)
        }
    }

    // TODO: Doesn't delete old files from non-related local repository when pulling from non-related remote
    @RequiresApi(Build.VERSION_CODES.O) // For Path to File conversion
    suspend fun verifyAndPull(jgit: Git, remoteLink: String, token: String): GitResult = withContext(Dispatchers.IO) {
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
                return@withContext GitResult(false, NotGitNotesRepositoryException())
            }

            // If the verification succeeds, pull the changes into the actual repo
            return@withContext pullToJGitFromRemote(jgit, remoteLink, token)
        } catch (e: Exception) {
            Log.d("MYLOG", "Exception when verifying and pulling: $e")
            return@withContext GitResult(false, e)
        }
    }

    private suspend fun pullToJGitFromRemote(jgit: Git, httpsLink: String, token: String): GitResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val repoConfig = jgit.repository.config
            repoConfig.setString(
                ConfigConstants.CONFIG_REMOTE_SECTION,
                "origin",
                "url",
                httpsLink.trim()
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

            GitResult(pullResult.mergeResult.mergeStatus.isSuccessful, null)
        } catch (e: Exception) {
            Log.d("MYLOG", "Exception when pulling: $e")
            GitResult(false, e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O) // For Path to File conversion
    suspend fun verifyAndClone(newRepo: Repository, remoteLink: String, token: String): GitResult = withContext(Dispatchers.IO) {
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
                return@withContext GitResult(false, NotGitNotesRepositoryException())
            }

            // The cloned repository is valid so create a new Repository object for it
            val newJGit = getOrCreateJGitRepository(newRepo)
            val result = pullToJGitFromRemote(newJGit, remoteLink, token)
            initNotesFromFiles(newJGit)
            return@withContext result
        } catch (e: Exception) {
            Log.d("MYLOG", "Exception when cloning: $e")
            return@withContext GitResult(false, e)
        }
    }
}