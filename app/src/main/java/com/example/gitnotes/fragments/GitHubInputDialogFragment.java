package com.example.gitnotes.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
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
import com.example.gitnotes.viewmodels.ButtonContainerViewModel;
import com.example.gitnotes.R;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GitHubInputDialogFragment extends DialogFragment {

    private ButtonContainerViewModel viewModel;
    private Spinner repoSpinner;
    private int customOptionPosition;

    public static GitHubInputDialogFragment newInstance() {
        return new GitHubInputDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        viewModel = new ViewModelProvider(requireActivity()).get(ButtonContainerViewModel.class);

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.git_dialog_layout, null);

        EditText input = dialogView.findViewById(R.id.repo_link_input);
        Button cloneButton = dialogView.findViewById(R.id.clone_button);
        Button pullButton = dialogView.findViewById(R.id.pull_button);
        Button pushButton = dialogView.findViewById(R.id.push_button);
        Button createRepoButton = dialogView.findViewById(R.id.create_repo_button);

        repoSpinner = dialogView.findViewById(R.id.repo_spinner);

        // Set the spinner's selection based on the saved position in the ViewModel
        viewModel.getSelectedSpinnerPosition().observe(this, position -> {
            repoSpinner.setSelection(position);
        });

        // Create an adapter for your spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item_layout, new ArrayList<>());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repoSpinner.setAdapter(adapter);

        // Observe the repositories LiveData and update the spinner's adapter when it changes
        viewModel.getRepositories().observe(this, repositories -> {
            adapter.clear();

            String customOptionText = getString(R.string.choose_other_repo);

            if (repositories != null && !repositories.isEmpty()) {
                List<String> repoNames = repositories.keySet().stream()
                        .map(File::getName)
                        .collect(Collectors.toList());
                adapter.addAll(repoNames);
            }

            adapter.add(customOptionText);
            adapter.notifyDataSetChanged();
            customOptionPosition = adapter.getCount() - 1;
        });

        repoSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = (String) parent.getItemAtPosition(position);

                if (position == customOptionPosition) {
                    input.post(() -> input.setText(""));
                } else if (viewModel.getRepositories().getValue() != null) {
                    for (File repoFile : viewModel.getRepositories().getValue().keySet()) {
                        if (repoFile.getName().equals(selectedItem)) {
                            input.post(() -> input.setText(viewModel.getRepositories().getValue().get(repoFile)));
                            break;
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });



        // Clone repository
        cloneButton.setOnClickListener(v -> {
            MainActivity mainActivity = (MainActivity) requireActivity();
            String repoName = viewModel.getGitHelper().extractRepoName(input.getText().toString());
            Log.d("MYLOG", repoName);

            // Check if the repoName matches the regular expression and if the URL contains github.com
            if (Pattern.matches("^[a-zA-Z0-9-_]+$", repoName) && input.getText().toString().contains("github.com")) {
                UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(mainActivity.retrieveUsername(), mainActivity.retrieveToken());
                viewModel.getGitHelper().cloneGitRepository(input.getText().toString(), credentialsProvider, mainActivity.getFilesDir().toString() + "/" + repoName, () -> {
                    // The cloning operation has finished, update the UI on the main UI thread.
                    mainActivity.runOnUiThread(() -> {
                        viewModel.getGitHelper().saveRepoMetadata(new File(mainActivity.getFilesDir().toString() + "/" + repoName), input.getText().toString());
                        mainActivity.showAlertDialog("Cloned repository " + repoName + ".");
                        viewModel.getGitHelper().scanForGitRepositories(mainActivity.getFilesDir(), viewModel.getRepositories().getValue()); // Update repositories
                        dismiss(); // Dismiss the current AlertDialog
                    });
                });
            } else {
                // If either condition is not met, show an AlertDialog with an error message
                mainActivity.showAlertDialog("Invalid repository name or URL. Please ensure the repository name contains only alphanumeric characters and hyphens, and the URL is a valid GitHub URL.");
            }
        });



        // Pull repository
        pullButton.setOnClickListener(v -> {
            MainActivity mainActivity = (MainActivity) requireActivity();
            String repoName = viewModel.getGitHelper().extractRepoName(input.getText().toString());

            if (Pattern.matches("^[a-zA-Z0-9-_]+$", repoName) && input.getText().toString().contains("github.com")) {
                String repoPath = requireActivity().getFilesDir().toString() + "/" + repoName;
                FileRepositoryBuilder builder = new FileRepositoryBuilder();
                try {
                    Repository repository = builder.setGitDir(new File(repoPath, ".git")).build();
                    viewModel.getGitHelper().pullFromGit(repository, input.getText().toString(), mainActivity.retrieveToken(), () -> {
                        mainActivity.runOnUiThread(() -> {
                            mainActivity.showAlertDialog("Pulled from repository " + repoName + ".");
                            dismiss(); // Dismiss the current AlertDialog
                        });
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    mainActivity.showAlertDialog("Failed to open the local repository.");
                }
            } else {
                mainActivity.showAlertDialog("Invalid repository name or URL. Please ensure the repository name contains only alphanumeric characters and hyphens, and the URL is a valid GitHub URL.");
            }
        });



        pushButton.setOnClickListener(v -> {
            // Push repository action
        });



        // Create repository
        createRepoButton.setOnClickListener(v -> {
            String repoPath = requireActivity().getFilesDir().toString();
            MainActivity mainActivity = (MainActivity) requireActivity();

            mainActivity.showInputDialog("Enter repo name:", Pattern.compile("^[a-zA-Z0-9-_]+$"),
                    "Invalid repository name. Please use only alphanumeric characters and hyphens.", repoName -> {
                viewModel.getGitHelper().initGitRepository(repoPath, repoName, result -> {
                    mainActivity.runOnUiThread(() -> {
                        if (result == 0) {
                            viewModel.getGitHelper().scanForGitRepositories(mainActivity.getFilesDir(), viewModel.getRepositories().getValue());
                            mainActivity.showAlertDialog("Created repository with name " + repoName + ".");
                        } else if (result == 1) {
                            mainActivity.showAlertDialog("Repository with name " + repoName +
                                    " already exists, no new repository created.");
                        } else if (result == -1) {
                            mainActivity.showAlertDialog("Big problem!!!");
                        }
                    });
                });
            });
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

    public static String TAG = "GitHubInputDialog";
}