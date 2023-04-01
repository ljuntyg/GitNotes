package com.example.gitnotes.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.gitnotes.activities.MainActivity;
import com.example.gitnotes.data.GitHelper;
import com.example.gitnotes.viewmodels.ButtonContainerViewModel;
import com.example.gitnotes.R;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GitHubInputDialogFragment extends DialogFragment {
    private ButtonContainerViewModel viewModel;
    private GitHelper gitHelper;
    private Spinner repoSpinner;
    private MainActivity mainActivity;

    private int customOptionPosition;
    private String customOptionString;

    public static GitHubInputDialogFragment newInstance() {
        return new GitHubInputDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        viewModel = new ViewModelProvider(requireActivity()).get(ButtonContainerViewModel.class);
        gitHelper = viewModel.getGitHelper();
        customOptionString = getString(R.string.choose_other_repo);

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.git_dialog_layout, null);
        mainActivity = (MainActivity) requireActivity();

        EditText input = dialogView.findViewById(R.id.repo_link_input);
        Button cloneButton = dialogView.findViewById(R.id.clone_button);
        Button pullButton = dialogView.findViewById(R.id.pull_button);
        Button pushButton = dialogView.findViewById(R.id.push_button);
        Button createRepoButton = dialogView.findViewById(R.id.create_repo_button);
        Button deleteRepoButton = dialogView.findViewById(R.id.delete_repo_button);

        repoSpinner = dialogView.findViewById(R.id.repo_spinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(mainActivity, R.layout.spinner_item_layout, new ArrayList<>());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repoSpinner.setAdapter(adapter);

        viewModel.getRepositories().observe(this, repositories -> {
            adapter.clear();

            repositories.keySet().stream().forEach(key -> adapter.add(key.getName()));

            adapter.add(customOptionString);
            customOptionPosition = adapter.getCount() - 1;

            // Set default selection if no other selection is available
            if (repositories.isEmpty()) {
                repoSpinner.setSelection(customOptionPosition);
            }

            // Set the spinner's selection to the last chosen option stored in the viewModel
            int lastSelectedPosition = viewModel.getSelectedSpinnerPosition().getValue();
            if (lastSelectedPosition >= 0 && lastSelectedPosition < adapter.getCount()) {
                repoSpinner.setSelection(lastSelectedPosition);
                String selectedItem = adapter.getItem(lastSelectedPosition);
                Consumer<String> updateInputState = s -> {
                    if (s.equals(customOptionString)) {
                        input.setText("");
                        input.setEnabled(true);
                    } else {
                        File repoFile = repositories.keySet().stream()
                                .filter(file -> file.getName().equals(s))
                                .findFirst()
                                .orElse(null);

                        if (repoFile != null) {
                            String repoLink = repositories.get(repoFile);
                            input.setText(repoLink);
                            gitHelper.setSelectedRepository(repoFile);
                            input.setEnabled(repoLink.equals(getString(R.string.repo_link_missing)));
                        }
                    }
                };
                updateInputState.accept(selectedItem);
            }
        });

        gitHelper.scanForGitRepositories(viewModel, mainActivity.getFilesDir(), getString(R.string.repo_link_missing));

        repoSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = (String) parent.getItemAtPosition(position);
                Consumer<String> updateInputState = s -> {
                    if (s.equals(customOptionString)) {
                        input.setText("");
                        input.setEnabled(true);
                    } else {
                        File repoFile = viewModel.getRepositories().getValue().keySet().stream()
                                .filter(file -> file.getName().equals(s))
                                .findFirst()
                                .orElse(null);

                        if (repoFile != null) {
                            String repoLink = viewModel.getRepositories().getValue().get(repoFile);
                            input.setText(repoLink);
                            gitHelper.setSelectedRepository(repoFile);
                            input.setEnabled(repoLink.equals(getString(R.string.repo_link_missing)));
                        }
                    }
                };
                updateInputState.accept(selectedItem);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });



        // Delete selected repository
        deleteRepoButton.setOnClickListener(b -> {
            if (gitHelper != null && repoSpinner.getSelectedItemPosition() != customOptionPosition) {
                File repoToRemove = gitHelper.getSelectedRepository();
                if (repoToRemove != null) { // Add null check
                    String repoName = repoToRemove.getName();

                    // Show a confirmation dialog
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Delete Repository")
                            .setMessage("Are you sure you want to delete the repository " + repoName + "?")
                            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                // Delete the repository folder
                                if (mainActivity.getPeripheralDataHelper().deleteRecursive(repoToRemove)) {
                                    viewModel.removeRepository(repoToRemove);

                                    // Set selected repository to null if it was deleted
                                    if (gitHelper.getSelectedRepository() != null && gitHelper.getSelectedRepository().equals(repoToRemove)) {
                                        gitHelper.setSelectedRepository(null);
                                        gitHelper.scanForGitRepositories(viewModel, mainActivity.getFilesDir(), getString(R.string.repo_link_missing));

                                        // Update spinner selection after deletion
                                        int newPosition = Math.max(0, repoSpinner.getSelectedItemPosition() - 1);
                                        viewModel.setSelectedSpinnerPosition(newPosition);
                                        repoSpinner.setSelection(newPosition);
                                    }

                                    gitHelper.scanForGitRepositories(viewModel,
                                            mainActivity.getFilesDir(), getString(R.string.repo_link_missing));
                                    mainActivity.showAlertDialog("Repo " + repoName + " removed successfully.");
                                } else {
                                    mainActivity.showAlertDialog("Failed to remove the repository " + repoName + ".");
                                }
                            })
                            .setNegativeButton(android.R.string.no, null)
                            .setIcon(R.drawable.warning_48px)
                            .show();
                }
            }
        });



        // Create new repository
        createRepoButton.setOnClickListener(v -> {
            mainActivity.showInputDialog("Enter repo name:", Pattern.compile("^\\s*[a-zA-Z0-9-_]+\\s*$"),
                    "Invalid repository name. Please use only alphanumeric characters and hyphens.", repoName -> {
                        File repoPath = mainActivity.getFilesDir();
                        gitHelper.initGitRepository(repoPath.toString(), repoName.trim(), viewModel.getNotes().getValue(), result -> {
                            mainActivity.runOnUiThread(() -> {
                                if (result == 0) {
                                    gitHelper.scanForGitRepositories(viewModel, mainActivity.getFilesDir(), getString(R.string.repo_link_missing)); // Update repositories
                                    mainActivity.showAlertDialog("Created repository with name " + repoName + ".");
                                    findRepoAndSetSpinnerPosition(repoName);
                                } else if (result == 1) {
                                    mainActivity.showAlertDialog("Repository with name " + repoName +
                                            " already exists, no new repository created.");
                                    findRepoAndSetSpinnerPosition(repoName);
                                } else if (result == -1) {
                                    mainActivity.showAlertDialog("Big problem!!!");
                                }
                            });
                        });
                    });
        });



        // Push selected repository
        pushButton.setOnClickListener(v -> {
            if (repoSpinner.getSelectedItemPosition() != customOptionPosition && gitHelper.isGit(gitHelper.getSelectedRepository())) {
                File selectedRepository = gitHelper.getSelectedRepository();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                String commitMessage = "Commit at: " + formatter.format(new Date());
                Repository tempRepository = null;
                Log.d("MYLOG", "repo directory for building repository object: " + selectedRepository);
                try {
                    tempRepository = new FileRepositoryBuilder()
                            .setWorkTree(selectedRepository)
                            .readEnvironment()
                            .findGitDir()
                            .build();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                final Repository repository = tempRepository;
                gitHelper.addToGit(repository, viewModel.getNotes().getValue(), () ->
                                gitHelper.commitToGit(repository, commitMessage, () ->
                                                gitHelper.pushToGit(repository, viewModel.getRepositories().getValue().get(selectedRepository), mainActivity.getPeripheralDataHelper().retrieveToken(), () -> {
                                                    mainActivity.runOnUiThread(() -> {
                                                        gitHelper.scanForGitRepositories(viewModel, requireActivity().getFilesDir(), getString(R.string.repo_link_missing)); // Update repositories
                                                        mainActivity.showAlertDialog("Successfully pushed changes to the remote repository!");
                                                    });
                                                }, e -> {
                                                    mainActivity.runOnUiThread(() -> {
                                                        mainActivity.showAlertDialog("Error while pushing changes: " + e.getMessage());
                                                    });
                                                }),
                                        e -> {
                                            mainActivity.runOnUiThread(() -> {
                                                mainActivity.showAlertDialog("Error while committing changes: " + e.getMessage());
                                            });
                                        }),
                        e -> {
                            mainActivity.runOnUiThread(() -> {
                                mainActivity.showAlertDialog("Error while adding changes: " + e.getMessage());
                            });
                        });
            }
        });



        // Pull selected repository
        pullButton.setOnClickListener(v -> {
            if (repoSpinner.getSelectedItemPosition() != customOptionPosition && gitHelper.isGit(gitHelper.getSelectedRepository())) {
                String repoName = gitHelper.extractRepoName(input.getText().toString());
                if (Pattern.matches("^[a-zA-Z0-9-_]+$", repoName) && input.getText().toString().contains("github.com")) {
                    String repoPath = requireActivity().getFilesDir().toString() + "/" + repoName;
                    FileRepositoryBuilder builder = new FileRepositoryBuilder();
                    try {
                        Repository repository = builder.setGitDir(new File(repoPath, ".git")).build();
                        gitHelper.pullFromGit(repository, input.getText().toString(), mainActivity.getPeripheralDataHelper().retrieveToken(), () -> {
                            mainActivity.runOnUiThread(() -> {
                                gitHelper.saveRepoMetadata(new File(mainActivity.getFilesDir().toString() + "/" + repoName), input.getText().toString()); // Save metadata (repo link)
                                gitHelper.scanForGitRepositories(viewModel, requireActivity().getFilesDir(), getString(R.string.repo_link_missing)); // Update repositories
                                viewModel.readAndAddNotesFromDirectory(repository.getDirectory().getParentFile()); // Create note objects from possible new files
                                mainActivity.showAlertDialog("Pull from repository " + repoName + " successful!");
                                dismiss(); // Dismiss the current AlertDialog
                            });
                        }, e -> {
                            mainActivity.runOnUiThread(() -> {
                                mainActivity.showAlertDialog("Error while pulling changes: " + e.getMessage());
                            });
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                        mainActivity.showAlertDialog("Failed to open the local repository.");
                    }
                } else {
                    mainActivity.showAlertDialog("Invalid repository name or URL. Please ensure the repository name contains only alphanumeric characters and hyphens, and the URL is a valid GitHub URL.");
                }
            }
        });



        // Clone selected repository
        cloneButton.setOnClickListener(v -> {
            if (repoSpinner.getSelectedItemPosition() == customOptionPosition) {
                String repoLink = input.getText().toString();
                String repoName = gitHelper.extractRepoName(input.getText().toString().trim());
                String repoPath = mainActivity.getFilesDir().toString() + "/" + repoName;
                // Check if the repoName matches the regular expression and if the URL contains github.com
                if (Pattern.matches("^[a-zA-Z0-9-_]+$", repoName) && repoLink.contains("github.com")) {
                    UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(mainActivity.getPeripheralDataHelper().retrieveUsername(), mainActivity.getPeripheralDataHelper().retrieveToken());
                    gitHelper.cloneGitRepository(repoLink, credentialsProvider, repoPath, () -> {
                        // The cloning operation has finished, update the UI on the main UI thread.
                        mainActivity.runOnUiThread(() -> {
                            gitHelper.saveRepoMetadata(new File(repoPath), repoLink); // Save metadata (repo link)
                            gitHelper.scanForGitRepositories(viewModel, mainActivity.getFilesDir(), getString(R.string.repo_link_missing)); // Update repositories
                            viewModel.readAndAddNotesFromDirectory(new File(repoPath)); // Create note objects from possible new files
                            mainActivity.showAlertDialog("Cloned repository " + repoName + " successfully!");
                            dismiss(); // Dismiss the current AlertDialog
                        });
                    }, e -> {
                        mainActivity.runOnUiThread(() -> {
                            mainActivity.showAlertDialog("Error while cloning the repository: " + e.getMessage());
                        });
                    });
                } else {
                    // If either condition is not met, show an AlertDialog with an error message
                    mainActivity.showAlertDialog("Invalid repository name or URL. Please ensure the repository name contains only alphanumeric characters and hyphens, and the URL is a valid GitHub URL.");
                }
            }
        });



        return new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.selected_repo))
                .setView(dialogView)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                    dismiss();
                })
                .create();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        int selectedPosition = repoSpinner.getSelectedItemPosition();
        viewModel.setSelectedSpinnerPosition(selectedPosition);
    }

    private boolean findRepoAndSetSpinnerPosition(String repoName) {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) repoSpinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equals(repoName)) {
                repoSpinner.setSelection(i);
                return true;
            }
        }
        return false;
    }

    public static String TAG = "GitHubInputDialog";
}