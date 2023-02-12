package com.example.gitnotes;

import androidx.lifecycle.Observer;
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
    private Observer<List<Note>> observer;

    public static ButtonContainerFragment newInstance() { return new ButtonContainerFragment(); }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_button_container, container, false);

        mViewModel = new ViewModelProvider(requireActivity()).get(ButtonContainerViewModel.class);
        mViewModel.getNotes().observe(getViewLifecycleOwner(), notes -> {
            if (!notes.isEmpty()) {
                addButton(notes.get(notes.size() - 1).getTitle());
            }
        });

        buttonContainer = view.findViewById(R.id.button_container);

        return view;
    }

    public void addButton(String text) {
        Button button = new Button(getContext());
        button.setText(text);
        buttonContainer.addView(button);
    }

}