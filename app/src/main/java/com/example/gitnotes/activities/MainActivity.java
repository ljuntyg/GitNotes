package com.example.gitnotes.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import android.Manifest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.gitnotes.data.Note;
import com.example.gitnotes.data.NoteDbHelper;
import com.example.gitnotes.fragments.ButtonContainerFragment;
import com.example.gitnotes.fragments.GitHubInputDialogFragment;
import com.example.gitnotes.fragments.NewNoteDialogFragment;
import com.example.gitnotes.R;
import com.example.gitnotes.viewmodels.ButtonContainerViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private ButtonContainerViewModel viewModel;
    private static final int REQUEST_PERMISSIONS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // deleteAllFilesInInternalStorage(); // !!
        // this.getSharedPreferences("app_preferences", MODE_PRIVATE).edit().clear().apply(); // !!

        viewModel = new ViewModelProvider(this).get(ButtonContainerViewModel.class);
        viewModel.setDbHelper(new NoteDbHelper(this));

        // Scan for repositories in local storage
        Map<File, String> foundRepositories = new HashMap<>();
        viewModel.getGitHelper().scanForGitRepositories(getFilesDir(), foundRepositories);
        viewModel.setRepositories(foundRepositories);

        Log.d("MYLOG", viewModel.getRepositories().getValue().toString());

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        ButtonContainerFragment buttonContainerFragment = new ButtonContainerFragment();
        fragmentTransaction.add(R.id.main_container, buttonContainerFragment).commit();

        Log.d("MYLOG", "token: " + retrieveToken());
        Log.d("MYLOG", "files: " + Arrays.toString(fileList()));

        handleIntent(getIntent());
    }

    public interface AlertDialogCallback {
        void onDialogConfirmed();
    }

    public void showAlertDialog(String message, AlertDialogCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    dialog.dismiss();
                    callback.onDialogConfirmed();
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void showAlertDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, id) -> dialog.dismiss());
        AlertDialog alert = builder.create();
        alert.show();
    }

    public interface InputDialogCallback {
        void onInputConfirmed(String input);
    }

    public void showInputDialog(String prompt, InputDialogCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setHint(R.string.name);
        input.setMaxLines(4);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(input);

        builder.setMessage(prompt)
                .setView(linearLayout)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    String ret = input.getText().toString();
                    if (callback != null) {
                        callback.onInputConfirmed(ret);
                    }
                }).setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .create().show();
    }

    public void showInputDialog(String prompt, Pattern allowedInput, String errorMsg, InputDialogCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setHint(R.string.name);
        input.setMaxLines(4);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(input);

        builder.setMessage(prompt)
                .setView(linearLayout)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    String ret = input.getText().toString();
                    if (allowedInput.matcher(ret).matches()) {
                        if (callback != null) {
                            callback.onInputConfirmed(ret);
                        }
                    } else {
                        showAlertDialog(errorMsg);
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .create().show();
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

    public void openNewNoteDialog(View view) {
        NewNoteDialogFragment dialog = NewNoteDialogFragment.newInstance(new Bundle());
        dialog.show(getSupportFragmentManager(), NewNoteDialogFragment.TAG);
    }

    public void openGitHubInputDialog(View view) {
        if (retrieveToken() == null) {
            enterTokenAndUsernameDialog(isTokenAndUsernameValid -> {
                if (isTokenAndUsernameValid) {
                    GitHubInputDialogFragment dialog = GitHubInputDialogFragment.newInstance();
                    dialog.show(getSupportFragmentManager(), GitHubInputDialogFragment.TAG);
                }
            });
        } else {
            GitHubInputDialogFragment dialog = GitHubInputDialogFragment.newInstance();
            dialog.show(getSupportFragmentManager(), GitHubInputDialogFragment.TAG);
        }
    }

    // Not secure?
    private void storeUsername(String username) {
        SharedPreferences sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.apply();
    }

    // Not secure?
    public String retrieveUsername() {
        SharedPreferences sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE);
        return sharedPreferences.getString("username", null);
    }


    // Not secure?
    private void storeToken(String token) {
        SharedPreferences sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("personal_access_token", token);
        editor.apply();
    }

    // Not secure?
    public String retrieveToken() {
        SharedPreferences sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE);
        return sharedPreferences.getString("personal_access_token", null);
    }

    public interface TokenDialogCallback {
        void onTokenProvided(boolean isTokenAndUsernameValid);
    }

    public void enterTokenAndUsernameDialog(TokenDialogCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.token_and_username_dialog, null);

        final EditText tokenInput = view.findViewById(R.id.token_input);
        final EditText usernameInput = view.findViewById(R.id.username_input);

        builder.setMessage(getString(R.string.pat_and_username))
                .setView(view)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {

                    String token = tokenInput.getText().toString().trim().replaceAll("\\s","");
                    String username = usernameInput.getText().toString().trim().replaceAll("\\s","");

                    // Check if username is ok and if token matches regular or fine grained token, https://gist.github.com/magnetikonline/073afe7909ffdd6f10ef06a00bc3bc88
                    if (username.equals("") || !Pattern.matches("^[a-zA-Z0-9-_]+$", username)) {
                        dialog.dismiss();
                        showAlertDialog("Username not valid.");
                        callback.onTokenProvided(true);
                    } else if (Pattern.matches("^ghp_[a-zA-Z0-9]{36}$", token) ||
                            Pattern.matches("^github_pat_[a-zA-Z0-9]{22}_[a-zA-Z0-9]{59}$", token)) {
                        storeToken(token);
                        storeUsername(username);
                        dialog.dismiss();
                        showAlertDialog("Token and username successfully registered.", () -> {
                            callback.onTokenProvided(true);
                        });
                    } else {
                        dialog.dismiss();
                        showAlertDialog("Token not in correct format or token format not supported.");
                        callback.onTokenProvided(false);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                    dialog.dismiss();
                    callback.onTokenProvided(false);
                })
                .create();
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void deleteAllFilesInInternalStorage() {
        File internalStorageDir = this.getFilesDir();
        deleteRecursive(internalStorageDir);
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }
}

