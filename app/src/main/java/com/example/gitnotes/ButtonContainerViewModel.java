package com.example.gitnotes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class ButtonContainerViewModel extends ViewModel {
    private MutableLiveData<List<Note>> notes = new MutableLiveData<List<Note>>();

    public ButtonContainerViewModel() {
        notes.setValue(new ArrayList<>());
    }

    public LiveData<List<Note>> getNotes() {
        return notes;
    }

    public void addNote(Note note) {
        List<Note> currentNotes = notes.getValue();
        currentNotes.add(note);
        notes.setValue(currentNotes);
    }
}