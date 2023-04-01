package com.example.gitnotes.data;

import android.util.Log;

import com.example.gitnotes.viewmodels.ButtonContainerViewModel;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GitHelper {
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private File selectedRepository;
    private Git git;


    public File getSelectedRepository() {
        return selectedRepository;
    }

    public void setSelectedRepository(File repository) {
        selectedRepository = repository;
    }

    public interface GitInitCallback {
        void onResult(int result);
    }

    public void initGitRepository(String repoPath, String subfolderName, List<Note> allNotes, GitInitCallback callback) {
        executorService.submit(() -> {
            try {
                File baseDir = new File(repoPath, subfolderName);
                baseDir.mkdirs(); // Create the subfolder if it doesn't exist
                createNoteTextFiles(baseDir, allNotes); // Create text files in new repo folder
                File gitDir = new File(baseDir, ".git");
                if (!gitDir.exists()) {
                    git = Git.init().setDirectory(baseDir).call();
                    if (callback != null) {
                        callback.onResult(0); // new repository created
                    }
                } else {
                    git = Git.open(baseDir);
                    if (callback != null) {
                        callback.onResult(1); // repository exists
                    }
                }
            } catch (IOException | GitAPIException e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onResult(-1); // error
                }
            }
        });
    }

    public Git getGit() {
        return git;
    }

    public void addToGit(Repository repository, List<Note> allNotes, Runnable callback, Consumer<Exception> errorCallback) {
        executorService.submit(() -> {
            try (Git git = new Git(repository)) {
                Log.d("MYLOG", "repository.getDirectory() in addToGit: " + repository.getDirectory());
                createNoteTextFiles(repository.getDirectory().getParentFile(), allNotes);
                git.add().addFilepattern(".").call();
            } catch (Exception e) {
                e.printStackTrace();
                if (errorCallback != null) {
                    errorCallback.accept(e);
                }
            }

            if (callback != null) {
                callback.run();
            }
        });
    }

    public void commitToGit(Repository repository, String commitMessage, Runnable callback, Consumer<Exception> errorCallback) {
        executorService.submit(() -> {
            try (Git git = new Git(repository)) {
                git.commit().setMessage(commitMessage).call();
            } catch (Exception e) {
                e.printStackTrace();
                if (errorCallback != null) {
                    errorCallback.accept(e);
                }
            }

            if (callback != null) {
                callback.run();
            }
        });
    }

    public void pushToGit(Repository repository, String remoteUrl, String personalAccessToken, Runnable successCallback, Consumer<Exception> errorCallback) {
        executorService.submit(() -> {
            try (Git git = new Git(repository)) {
                Log.d("MYLOG", "current branch in push: " + repository.getBranch());
                Log.d("MYLOG", "files in repository being pushed: " +  Arrays.toString(repository.getDirectory().getParentFile().listFiles()));
                UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider("token", personalAccessToken);
                git.push().setCredentialsProvider(credentialsProvider).setRemote(remoteUrl).call();
                if (successCallback != null) {
                    successCallback.run();
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (errorCallback != null) {
                    errorCallback.accept(e);
                }
            }
        });
    }

    public void pullFromGit(Repository repository, String remoteUrl, String personalAccessToken, Runnable callback, Consumer<Exception> errorCallback) {
        executorService.submit(() -> {
            try (Git git = new Git(repository)) {
                Log.d("MYLOG", "URL for pull: " + remoteUrl);
                UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider("token", personalAccessToken);
                git.pull().setCredentialsProvider(credentialsProvider).call();
            } catch (Exception e) {
                e.printStackTrace();
                if (errorCallback != null) {
                    errorCallback.accept(e);
                }
                return;
            }

            if (callback != null) {
                callback.run();
            }
        });
    }

    public void cloneGitRepository(String remoteUrl, UsernamePasswordCredentialsProvider credentialsProvider, String clonePath, Runnable callback, Consumer<Exception> errorCallback) {
        executorService.submit(() -> {
            try {
                Git.cloneRepository().setURI(remoteUrl).setDirectory(new File(clonePath)).setCredentialsProvider(credentialsProvider).setBare(false).call();
                Log.d("MYLOG", "post clone");
            } catch (Exception e) {
                e.printStackTrace();
                if (errorCallback != null) {
                    errorCallback.accept(e);
                }
                return;
            }

            if (callback != null) {
                callback.run();
            }
        });
    }

    public String extractRepoName(String url) {
        String repoName = "";
        Pattern pattern = Pattern.compile("(?<=\\/)[^\\/]+(?=(\\.git$|$))");
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            repoName = matcher.group(0);
        }

        return repoName.replaceFirst("\\.git$", "");
    }

    public void scanForGitRepositories(ButtonContainerViewModel viewModel, File directory, String ifMissingLink) {
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (new File(file, ".git").exists()) {
                        File repoPath = file.getAbsoluteFile();
                        String repoLink = readRepoMetadata(repoPath, ifMissingLink);
                        Log.d("MYLOG", "file found in scan for git repo: " + file);
                        Map<File, String> newRepos = viewModel.getRepositories().getValue();
                        newRepos.put(file, repoLink);
                        viewModel.setRepositories(newRepos);
                    } else {
                        scanForGitRepositories(viewModel, file, ifMissingLink);
                    }
                }
            }
        }
    }

    public String readRepoMetadata(File repository, String ifMissingLink) {
        File metadataFile = new File(repository, ".metadata");
        try (Scanner scanner = new Scanner(metadataFile)) {
            if (scanner.hasNextLine()) {
                return scanner.nextLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ifMissingLink;
    }

    public void saveRepoMetadata(File repository, String repoLink) {
        try {
            File metadataFile = new File(repository, ".metadata");
            try (FileWriter writer = new FileWriter(metadataFile)) {
                writer.write(repoLink);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isGit(File repo) {
        if (repo == null) {
            return false;
        }
        File gitDir = new File(repo, ".git");
        return gitDir.exists() && gitDir.isDirectory();
    }

    public void createNoteTextFiles(File directory, List<Note> allNotes) {
        if (allNotes != null) {
            for (Note note : allNotes) {
                try {
                    String sanitizedTitle = sanitizeTitle(note.getTitle());
                    if (sanitizedTitle.length() > 250) {
                        sanitizedTitle = sanitizedTitle.substring(0, 250);
                    }
                    File file = new File(directory, sanitizedTitle + ".txt");
                    FileWriter writer = new FileWriter(file);
                    writer.write(note.getBody());
                    writer.close();
                } catch (IOException e) {
                    Log.e("MYLOG", "Failed to create text file for note " + note.getTitle(), e);
                }
            }
        }

        Log.d("MYLOG", "directory contents after txt file creation (local directory scope): " + Arrays.stream(directory.listFiles())
                .map(File::getName)
                .collect(Collectors.joining(", ")));
    }

    private String sanitizeTitle(String title) {
        return title.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
