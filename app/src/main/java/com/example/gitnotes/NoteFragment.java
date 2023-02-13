package com.example.gitnotes;

import android.graphics.Typeface;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

public class NoteFragment extends Fragment {
    private String title;
    private String body;

    public static NoteFragment newInstance(Note note) {
        NoteFragment fragment = new NoteFragment();

        Bundle args = new Bundle();
        args.putString("title", note.getTitle());
        args.putString("body", note.getBody());

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_note, container, false);

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
        titleEditText.setEnabled(false);
        titleEditText.setFocusable(false);

        // Set body text view properties
        bodyEditText.setText(body);
        bodyEditText.setTextSize(18);
        bodyEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        bodyEditText.setGravity(Gravity.START);
        bodyEditText.setEnabled(false);
        bodyEditText.setFocusable(false);

        // Add views to the linear layout
        linearLayout.addView(titleEditText);
        linearLayout.addView(bodyEditText);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
        toolbar.setVisibility(View.VISIBLE);
    }
}