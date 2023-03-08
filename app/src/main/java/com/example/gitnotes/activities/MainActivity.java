package com.example.gitnotes.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.gitnotes.data.NoteDbHelper;
import com.example.gitnotes.data.Note;
import com.example.gitnotes.fragments.ButtonContainerFragment;
import com.example.gitnotes.fragments.GitHubInputDialogFragment;
import com.example.gitnotes.fragments.NewNoteDialogFragment;
import com.example.gitnotes.R;
import com.example.gitnotes.viewmodels.ButtonContainerViewModel;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private ButtonContainerViewModel viewModel;

    private NoteDbHelper dbHelper;
    private SQLiteDatabase dbWritable;
    private SQLiteDatabase dbReadable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        viewModel = new ViewModelProvider(this).get(ButtonContainerViewModel.class);

        dbHelper = new NoteDbHelper(this);
        dbWritable = dbHelper.getWritableDatabase();
        dbReadable = dbHelper.getReadableDatabase();

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        ButtonContainerFragment buttonContainerFragment = new ButtonContainerFragment();
        fragmentTransaction.add(R.id.main_container, buttonContainerFragment).commit();

        /*addData(new Note("testing", "this"));
        viewData();*/

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null) {
                Bundle args = new Bundle();
                args.putString("shared_text", sharedText);
                NewNoteDialogFragment dialog = NewNoteDialogFragment.newInstance(args);
                dialog.show(getSupportFragmentManager(), NewNoteDialogFragment.TAG);
            }
        }
    }

    public void openGitHubInputDialog(View view) {
        GitHubInputDialogFragment dialog = GitHubInputDialogFragment.newInstance();
        dialog.show(getSupportFragmentManager(), GitHubInputDialogFragment.TAG);
    }

    public void openNewNoteDialog(View view) {
        NewNoteDialogFragment dialog = NewNoteDialogFragment.newInstance(new Bundle());
        dialog.show(getSupportFragmentManager(), NewNoteDialogFragment.TAG);
    }

    public boolean addData(Note note) {
        ContentValues values = new ContentValues();
        values.put(NoteDbHelper.COLUMN_NAME_TITLE, note.getTitle());
        values.put(NoteDbHelper.COLUMN_NAME_BODY, note.getBody());

        long res = dbWritable.insert(NoteDbHelper.TABLE_NAME, null, values);
        return res != -1;
    }

    public void viewData() {
        Cursor data = dbWritable.rawQuery("SELECT * FROM " + NoteDbHelper.TABLE_NAME, null);

        if (data.getCount() == 0) {
            throw new SQLException("no data");
        }

        StringBuilder sb = new StringBuilder();
        while (data.moveToNext()) {
            sb.append("ID: " + data.getString(0) + "\n");
            sb.append("TITLE: " + data.getString(1) + "\n");
            sb.append("BODY: " + data.getString(2) + "\n");

            displayMessage("All stored data: ", sb.toString());
        }
    }

    public boolean updateData(String id, String title, String body) {
        try {
            Integer.parseInt(id);
            ContentValues values = new ContentValues();
            values.put(NoteDbHelper._ID, id);
            values.put(NoteDbHelper.COLUMN_NAME_TITLE, title);
            values.put(NoteDbHelper.COLUMN_NAME_BODY, body);
            dbWritable.update(NoteDbHelper.TABLE_NAME, values, "ID = ?", new String[] {id});
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public int deleteData(String id) {
        try {
            Integer.parseInt(id);
            return dbWritable.delete(NoteDbHelper.TABLE_NAME, "ID = ?", new String[] {id});
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public void displayMessage(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
    }

    public void storeFile(String fileName, String fileContents) {
        try {
            FileOutputStream fos = this.openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(fileContents.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String[] getFiles() {
        return this.fileList();
    }
}

