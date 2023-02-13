package com.example.gitnotes;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class GitHubInputDialogFragment extends DialogFragment {

    private ButtonContainerViewModel viewModel;

    public static GitHubInputDialogFragment newInstance() { return new GitHubInputDialogFragment(); }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        viewModel = new ViewModelProvider(requireActivity()).get(ButtonContainerViewModel.class);
        final EditText input = new EditText(getContext());
        input.setHint(R.string.link);

        return new AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.github_repo))
                .setView(input)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    viewModel.setRepoLink(input.getText().toString());
                    System.out.println(viewModel.getRepoLink());
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                    dismiss();
                })
                .create();
    }

    public static String TAG = "GitHubInputDialog";
}
