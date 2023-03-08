package com.example.gitnotes;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.List;

public class ButtonContainerFragment extends Fragment {

    private ButtonContainerViewModel mViewModel;
    private LinearLayout buttonContainer;
    private Note lastAddedNote;

    public static ButtonContainerFragment newInstance() { return new ButtonContainerFragment(); }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_button_container, container, false);

        mViewModel = new ViewModelProvider(requireActivity()).get(ButtonContainerViewModel.class);

        // Check to only add note if not duplicate
        mViewModel.getNotes().observe(getViewLifecycleOwner(), notes -> {
            if (!notes.isEmpty()) {
                Note latestNote = notes.get(notes.size() - 1);
                if (!latestNote.equals(lastAddedNote)) {
                    lastAddedNote = latestNote;
                    addButton(latestNote);
                }
            }
        });

        buttonContainer = view.findViewById(R.id.button_container);

        recreateButtons();

        return view;
    }

    public void addButton(Note note) {
        Button button = new Button(getContext());
        button.setText(note.getTitle());
        button.setOnClickListener(view -> openNote(note));
        buttonContainer.addView(button);
    }

    public void openNote(Note note) {
        NoteFragment noteFragment = NoteFragment.newInstance(note);

        FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.main_container, noteFragment)
                .addToBackStack(null)
                .commit();
    }

    private void recreateButtons() {
        List<Note> notes = mViewModel.getNotes().getValue();
        if (notes != null && !notes.isEmpty()) {
            for (Note note : notes) {
                addButton(note);
            }
        }
    }
}