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
import org.eclipse.jgit.api.ResetCommand
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

    sealed class GitResult {
        data object Success : GitResult()
        data class Failure(val exception: Exception?, val needForce: Boolean = false) : GitResult()
    }

    inner class NotGitNotesRepositoryException : Exception(appContext.getString(R.string.remote_not_gitnotes_repo))

    private fun isGitNotesRepository(git: Git): Boolean {
        val dir = git.repository.directory.parentFile

        val files = dir.listFiles()
        if (files == null || files.isEmpty()) return true

        return files.filter { it.isFile && !it.name.startsWith(".git") }.all { file ->
            file.extension == "txt" && file.bufferedReader().use { reader ->
                reader.readLine().startsWith("ID: ")
                        && reader.readLine().startsWith("Created at: ")
                        && reader.readLine().startsWith("Updated at: ")
            }
        }
    }

    suspend fun createNoteFiles(jgit: Git, notes: List<Note>) = withContext(Dispatchers.IO) {
        val dir = jgit.repository.directory.parentFile

        val noteFileNames = notes.map { "${it.title.take(20).replace(" ", "_")}.txt" }.toSet()

        dir.listFiles()?.forEach { file ->
            if (file.name !in noteFileNames) {
                jgit.rm().addFilepattern(file.name).call()
            }
        }

        for (note in notes) {
            val noteFile = File(dir, "${note.title.take(20).replace(" ", "_")}.txt")
            noteFile.writeText("ID: ${note.id}\nCreated at: ${note.createdAt}\nUpdated at: ${note.lastUpdatedAt}\n\nTitle: ${note.title}\n\nBody: ${note.body}")
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
            if (file.isFile && file.name.endsWith(".txt")) {
                val content = file.readText()

                val noteId =
                    content.substringAfter("ID: ").substringBefore("\n").toLongOrNull() // Parse ID from filename
                val noteCreatedAt =
                    content.substringAfter("\nCreated at: ").substringBefore("\n").toLongOrNull() // Parse note creation time
                val noteUpdatedAt =
                    content.substringAfter("\nUpdated at: ").substringBefore("\n\n").toLongOrNull() // Parse note update time
                val noteTitle =
                    content.substringAfter("\n\nTitle: ").substringBefore("\n\n") // Parse note title
                val noteBody = content.substringAfter("\n\nBody: ") // Parse note body

                if (noteId != null && noteCreatedAt != null && noteUpdatedAt != null) {
                    val note = Note(noteId, noteTitle, noteBody, noteCreatedAt, noteUpdatedAt)
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

            if (commit != null) {
                GitResult.Success
            } else {
                GitResult.Failure(null)
            }
        } catch (e: Exception) {
            Log.d("MYLOG", "Exception when committing: $e")
            GitResult.Failure(e)
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
                return@withContext GitResult.Failure(NotGitNotesRepositoryException())
            }

            // If the verification succeeds, push the changes to the remote repo
            return@withContext pushJGitToRemote(jgit, remoteLink, token)
        } catch (e: Exception) {
            Log.d("MYLOG", "Exception when verifying and pushing: $e")
            return@withContext GitResult.Failure(e)
        }
    }

    suspend fun pushJGitToRemote(jgit: Git, httpsLink: String, token: String, force: Boolean = false): GitResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val pushCommand: PushCommand = jgit.push()
            pushCommand.remote = httpsLink.trim()
            pushCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
            pushCommand.setPushAll()
            pushCommand.isForce = force

            val results = pushCommand.call()

            var success = true
            var needForcePush = false
            for (result in results) {
                for (update in result.remoteUpdates) {
                    if (update.status != RemoteRefUpdate.Status.OK &&
                        update.status != RemoteRefUpdate.Status.UP_TO_DATE
                    ) {
                        success = false
                        if (update.status == RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD) {
                            needForcePush = true
                        }
                        break
                    }
                }
            }
            if (success) GitResult.Success else GitResult.Failure(null, needForcePush)
        } catch (e: Exception) {
            Log.d("MYLOG", "Exception when pushing: $e")
            GitResult.Failure(e)
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
                return@withContext GitResult.Failure(NotGitNotesRepositoryException())
            }

            // If the verification succeeds, pull the changes into the actual repo
            return@withContext pullToJGitFromRemote(jgit, remoteLink, token)
        } catch (e: Exception) {
            Log.d("MYLOG", "Exception when verifying and pulling: $e")
            return@withContext GitResult.Failure(e)
        }
    }

    // TODO: Makes sense to take force boolean here?
    suspend fun pullToJGitFromRemote(jgit: Git, httpsLink: String, token: String, force: Boolean = false): GitResult = withContext(Dispatchers.IO) {
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

            // Reset local branch to the state of the remote branch if force pull is enabled
            if (force) {
                jgit.reset().setMode(ResetCommand.ResetType.HARD).setRef("origin/master").call()
            }

            val pullCommand: PullCommand = jgit.pull()
            pullCommand.remote = "origin"
            pullCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))

            val pullResult = pullCommand.call()

            if (pullResult.mergeResult.mergeStatus.isSuccessful) {
                GitResult.Success
            } else {
                GitResult.Failure(null)
            }
        } catch (e: Exception) {
            Log.d("MYLOG", "Exception when pulling: $e")
            GitResult.Failure(e)
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
                return@withContext GitResult.Failure(NotGitNotesRepositoryException())
            }

            // The cloned repository is valid so create a new Repository object for it
            val newJGit = getOrCreateJGitRepository(newRepo)
            val result = pullToJGitFromRemote(newJGit, remoteLink, token)
            initNotesFromFiles(newJGit)
            return@withContext result
        } catch (e: Exception) {
            Log.d("MYLOG", "Exception when cloning: $e")
            return@withContext GitResult.Failure(e)
        }
    }
}