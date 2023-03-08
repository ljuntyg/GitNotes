package com.example.gitnotes;

import android.graphics.Typeface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Objects;

public class NoteFragment extends Fragment {
    private String title;
    private String body;
    private ButtonContainerViewModel mViewModel;
    private Note mNote;

    public static NoteFragment newInstance(Note note) {
        NoteFragment fragment = new NoteFragment();

        Bundle args = new Bundle();
        args.putString("title", note.getTitle());
        args.putString("body", note.getBody());
        args.putSerializable("note", note);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_note, container, false);

        // Changes toolbar from main activity toolbar to note fragment toolbar
        requireActivity().findViewById(R.id.toolbar).setVisibility(View.GONE);
        Toolbar toolbar = view.findViewById(R.id.toolbar_note);
        toolbar.setTitle(R.string.app_name);

        // Removes note and closes fragment, prompts yes/no first
        view.findViewById(R.id.remove_note_button).setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setMessage(getString(R.string.remove_note_question))
                    .setPositiveButton(getString(R.string.yes_confirm), (dialog, which) -> {
                        mViewModel.removeNote(mNote);
                        requireActivity().getSupportFragmentManager().popBackStack();
                    })
                    .setNegativeButton(getString(R.string.no_cancel), (dialog, which) -> {})
                    .create()
                    .show();
        });

        assert getArguments() != null;
        mViewModel = new ViewModelProvider(requireActivity()).get(ButtonContainerViewModel.class);
        mNote = (Note) getArguments().getSerializable("note");

        title = getArguments().getString("title");
        body = getArguments().getString("body");

        LinearLayout linearLayout = view.findViewById(R.id.note_fragment_linearlayout);
        EditText titleEditText = new EditText(getContext());
        EditText bodyEditText = new EditText(getContext());

        // Set title text view properties
        titleEditText.setText(title);
        titleEditText.setTextSize(24);
        titleEditText.setTypeface(null, Typeface.BOLD);
        titleEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        titleEditText.setGravity(Gravity.START);

        // Set body text view properties
        bodyEditText.setText(body);
        bodyEditText.setTextSize(18);
        bodyEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        bodyEditText.setGravity(Gravity.START);

        // Add views to the linear layout
        linearLayout.addView(titleEditText);
        linearLayout.addView(bodyEditText);

        titleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                mViewModel.updateNote(mNote, titleEditText.getText().toString(), mNote.getBody());
            }
        });

        bodyEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                mViewModel.updateNote(mNote, mNote.getTitle(), bodyEditText.getText().toString());
            }
        });

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireActivity().findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
    }
}