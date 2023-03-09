package com.example.gitnotes.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.gitnotes.data.Note;
import com.example.gitnotes.data.NoteDbHelper;
import com.example.gitnotes.fragments.ButtonContainerFragment;
import com.example.gitnotes.fragments.GitHubInputDialogFragment;
import com.example.gitnotes.fragments.NewNoteDialogFragment;
import com.example.gitnotes.R;
import com.example.gitnotes.viewmodels.ButtonContainerViewModel;

import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private ButtonContainerViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        viewModel = new ViewModelProvider(this).get(ButtonContainerViewModel.class);
        viewModel.setDbHelper(new NoteDbHelper(this));

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        ButtonContainerFragment buttonContainerFragment = new ButtonContainerFragment();
        fragmentTransaction.add(R.id.main_container, buttonContainerFragment).commit();

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
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

