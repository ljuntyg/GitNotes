package com.example.gitnotes.viewmodels;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.gitnotes.data.Note;
import com.example.gitnotes.data.NoteDbHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ButtonContainerViewModel extends ViewModel {
    private MutableLiveData<List<Note>> notes = new MutableLiveData<>();
    private MutableLiveData<String> repoLink = new MutableLiveData<>();

    private NoteDbHelper dbHelper;

    public ButtonContainerViewModel() {
        notes.setValue(new ArrayList<>());
    }

    public void setDbHelper(NoteDbHelper dbHelper) {
        this.dbHelper = dbHelper;
        buildNotes();
    }

    @Override
    protected void onCleared() {
        Log.d("MYLOG", dbHelper.viewData().toString());
        dbHelper.close();
    }


    public LiveData<List<Note>> getNotes() {
        return notes;
    }

    // Adds note to notes and DB
    public void addNote(Note note) {
        List<Note> allNotes = notes.getValue();

        if (!dbHelper.addData(note)) {
            throw new SQLException("add failed");
        }

        allNotes.add(note);
        notes.setValue(allNotes);
    }

    // Updates notes and DB
    public void updateNote(Note note, String title, String body) {
        List<Note> allNotes = notes.getValue();
        assert allNotes.contains(note);
        int id = allNotes.indexOf(note) + 1;

        if (!dbHelper.updateData(Integer.toString(id), title, body)) {
            throw new SQLException("update failed");
        }

        note.setTitle(title);
        note.setBody(body);
        allNotes.set(allNotes.indexOf(note), note);
        notes.setValue(allNotes);
    }

    // Removes note from notes and DB
    public void removeNote(Note note) {
        List<Note> allNotes = notes.getValue();
        assert allNotes.contains(note);
        int id = allNotes.indexOf(note) + 1;

        if (dbHelper.deleteData(Integer.toString(id)) == -1) {
            throw new SQLException("deletion failed");
        }

        allNotes.set(allNotes.indexOf(note), null); // null, so index for other notes not shifted
        notes.setValue(allNotes);
    }

    // Pulls notes from DB to LiveData
    private void buildNotes() {
        notes.setValue(dbHelper.viewData());
    }

    public String getRepoLink() { return repoLink.getValue(); }

    public void setRepoLink(String link) { repoLink.setValue(link); }
}