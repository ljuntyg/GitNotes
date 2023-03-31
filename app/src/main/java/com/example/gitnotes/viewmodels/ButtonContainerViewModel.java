package com.example.gitnotes.viewmodels;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.gitnotes.data.GitHelper;
import com.example.gitnotes.data.Note;
import com.example.gitnotes.data.NoteDbHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import kotlin.Triple;

public class ButtonContainerViewModel extends ViewModel {
    private final MutableLiveData<List<Note>> notes = new MutableLiveData<>();
    private final MutableLiveData<Map<File, String>> repositories = new MutableLiveData<>();
    private final MutableLiveData<Integer> selectedSpinnerPosition = new MutableLiveData<>(0);
    private final GitHelper gitHelper;

    private NoteDbHelper dbHelper;

    public ButtonContainerViewModel() {
        gitHelper = new GitHelper();
        notes.setValue(new ArrayList<>());
        repositories.setValue(new HashMap<>());
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

    public LiveData<Integer> getSelectedSpinnerPosition() {
        return selectedSpinnerPosition;
    }

    public void setSelectedSpinnerPosition(int position) {
        selectedSpinnerPosition.setValue(position);
    }

    public void addRepository(File repository, String repoLink) {
        Map<File, String> currentRepositories = repositories.getValue();
        assert currentRepositories != null;
        currentRepositories.put(repository, repoLink);
        repositories.setValue(currentRepositories);
    }

    public void removeRepository(File repository) {
        Map<File, String> currentRepositories = repositories.getValue();
        assert currentRepositories != null;
        currentRepositories.remove(repository);
        repositories.setValue(currentRepositories);
    }

    public LiveData<Map<File, String>> getRepositories() {
        return repositories;
    }

    public void setRepositories(Map<File, String> newRepositories) {
        repositories.setValue(newRepositories);
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

    public GitHelper getGitHelper() {
        return gitHelper;
    }

}