package com.example.gitnotes;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

public class InputDialogFragment extends DialogFragment {

    private ButtonContainerViewModel viewModel;

    public static InputDialogFragment newInstance(String sharedText) {
        InputDialogFragment fragment = new InputDialogFragment();
        Bundle args = new Bundle();
        args.putString("shared_text", sharedText);
        fragment.setArguments(args);
        return fragment;
    }
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        viewModel = new ViewModelProvider(requireActivity()).get(ButtonContainerViewModel.class);
        final EditText input = new EditText(getContext());
        input.setText(getArguments().getString("shared_text"));

        return new AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.note_details))
                .setView(input)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    String body = input.getText().toString();
                    String title = body.split("\n")[0];
                    viewModel.addNote(new Note(title, body));
                    System.out.println();
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                    dismiss();
                })
                .create();
    }
    public static String TAG = "InputDialog";
}
