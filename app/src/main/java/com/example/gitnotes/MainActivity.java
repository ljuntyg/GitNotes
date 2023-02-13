package com.example.gitnotes;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar(findViewById(R.id.toolbar));

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        ButtonContainerFragment buttonContainerFragment = new ButtonContainerFragment();
        fragmentTransaction.add(R.id.main_container, buttonContainerFragment).commit();

        handleIntent(getIntent());

        storeFile("testFile", "this is a test file!");
        System.out.println(Arrays.toString(getFiles()));
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
                InputDialogFragment dialog = InputDialogFragment.newInstance(sharedText);
                dialog.show(getSupportFragmentManager(), InputDialogFragment.TAG);
            }
        }
    }

    public void openGitHubInputDialog(View view) {
        GitHubInputDialogFragment dialog = GitHubInputDialogFragment.newInstance();
        dialog.show(getSupportFragmentManager(), GitHubInputDialogFragment.TAG);
    }

    public void openNewNoteDialog(View view) {
        NewNoteDialogFragment dialog = NewNoteDialogFragment.newInstance();
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

