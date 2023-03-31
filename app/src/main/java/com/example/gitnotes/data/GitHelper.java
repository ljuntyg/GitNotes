package com.example.gitnotes.data;

import android.util.Log;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitHelper {
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private Git git;
    private Repository selectedRepository;


    public Repository getSelectedRepository() {
        return selectedRepository;
    }

    public void setSelectedRepository(Repository repository) {
        selectedRepository = repository;
    }

    public interface GitInitCallback {
        void onResult(int result);
    }

    public void initGitRepository(String repoPath, String subfolderName, GitInitCallback callback) {
        executorService.submit(() -> {
            try {
                File baseDir = new File(repoPath, subfolderName);
                baseDir.mkdirs(); // Create the subfolder if it doesn't exist
                File gitDir = new File(baseDir, ".git");
                if (!gitDir.exists()) {
                    git = Git.init().setDirectory(baseDir).call();
                    Log.d("MYLOG", "repo created");
                    if (callback != null) {
                        callback.onResult(0); // new repository created
                    }
                } else {
                    git = Git.open(baseDir);
                    Log.d("MYLOG", "repo exists, opening current repo");
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

    public void addToGit(Repository repository, String notePath, Runnable callback) {
        executorService.submit(() -> {
            try (Git git = new Git(repository)) {
                git.add().addFilepattern(notePath).call();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }

            if (callback != null) {
                callback.run();
            }
        });
    }

    public void commitToGit(Repository repository, String commitMessage, Runnable callback) {
        executorService.submit(() -> {
            try (Git git = new Git(repository)) {
                git.commit().setMessage(commitMessage).call();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }

            if (callback != null) {
                callback.run();
            }
        });
    }

    public void pushToGit(Repository repository, String remoteUrl, String personalAccessToken, Runnable callback) {
        executorService.submit(() -> {
            try (Git git = new Git(repository)) {
                UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider("token", personalAccessToken);
                git.push().setCredentialsProvider(credentialsProvider).setRemote(remoteUrl).call();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }

            if (callback != null) {
                callback.run();
            }
        });
    }

    public void pullFromGit(Repository repository, String remoteUrl, String personalAccessToken, Runnable callback) {
        executorService.submit(() -> {
            try (Git git = new Git(repository)) {
                UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider("token", personalAccessToken);
                git.pull().setRemote(remoteUrl).setCredentialsProvider(credentialsProvider).call();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }

            if (callback != null) {
                callback.run();
            }
        });
    }

    public void cloneGitRepository(String remoteUrl, UsernamePasswordCredentialsProvider credentialsProvider, String clonePath, Runnable callback) {
        executorService.submit(() -> {
            try {
                Git.cloneRepository().setURI(remoteUrl).setDirectory(new File(clonePath)).setCredentialsProvider(credentialsProvider).call();
                Log.d("MYLOG", "post clone");
            } catch (GitAPIException e) {
                Log.d("MYLOG", "error, likely authorization issue");
                e.printStackTrace();
            } catch (Exception e) {
                Log.d("MYLOG", "Unhandled exception");
                e.printStackTrace();
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

    public void scanForGitRepositories(File directory, Map<File, String> repositories) {
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (new File(file, ".git").exists()) {
                        File repoPath = file.getAbsoluteFile();
                        String repoLink = readRepoMetadata(repoPath);
                        repositories.put(file, repoLink);
                    } else {
                        scanForGitRepositories(file, repositories);
                    }
                }
            }
        }
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

    public String readRepoMetadata(File repository) {
        File metadataFile = new File(repository, ".metadata");
        try (Scanner scanner = new Scanner(metadataFile)) {
            if (scanner.hasNextLine()) {
                return scanner.nextLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "No link for repository";
    }
}
