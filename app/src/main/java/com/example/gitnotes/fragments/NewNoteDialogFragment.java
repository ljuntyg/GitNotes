package com.example.gitnotes.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.gitnotes.viewmodels.ButtonContainerViewModel;
import com.example.gitnotes.data.Note;
import com.example.gitnotes.R;

public class NewNoteDialogFragment extends DialogFragment {

    private ButtonContainerViewModel viewModel;

    public static NewNoteDialogFragment newInstance(Bundle sharedText) {
        NewNoteDialogFragment fragment = new NewNoteDialogFragment();
        fragment.setArguments(sharedText);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        viewModel = new ViewModelProvider(requireActivity()).get(ButtonContainerViewModel.class);

        final EditText titleInput = new EditText(getContext());
        final EditText bodyInput = new EditText(getContext());
        titleInput.setHint(getString(R.string.title));
        bodyInput.setHint(getString(R.string.body));

        titleInput.setMaxLines(4);
        bodyInput.setMaxLines(20);

        assert getArguments() != null;
        if (!getArguments().isEmpty()) {
            String body = getArguments().getString("shared_text");
            String title = body.split("\n")[0];

            bodyInput.setText(body);
            titleInput.setText(title);
        }

        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(titleInput);
        linearLayout.addView(bodyInput);

        return new AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.note_details))
                .setView(linearLayout)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    String title = titleInput.getText().toString();
                    String body = bodyInput.getText().toString();
                    viewModel.addNote(new Note(title, body));
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                    dismiss();
                })
                .create();
    }

    public static String TAG = "NewNoteDialog";
}
