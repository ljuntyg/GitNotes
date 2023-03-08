package com.example.gitnotes.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.gitnotes.data.Note;

import java.util.ArrayList;
import java.util.List;

public class ButtonContainerViewModel extends ViewModel {
    private MutableLiveData<List<Note>> notes = new MutableLiveData<>();
    private MutableLiveData<String> repoLink = new MutableLiveData<>();

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

    public void updateNote(Note note, String title, String body) {
        List<Note> allNotes = notes.getValue();
        assert allNotes.contains(note);
        note.setTitle(title);
        note.setBody(body);
        allNotes.set(allNotes.indexOf(note), note);
        notes.setValue(allNotes);
    }

    public void removeNote(Note note) {
        List<Note> allNotes = notes.getValue();
        assert allNotes.contains(note);
        allNotes.remove(note);
        notes.setValue(allNotes);
    }

    public String getRepoLink() { return repoLink.getValue(); }

    public void setRepoLink(String link) { repoLink.setValue(link); }
}