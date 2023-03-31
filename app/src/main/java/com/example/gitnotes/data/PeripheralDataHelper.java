package com.example.gitnotes.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

public class PeripheralDataHelper {
    private final SharedPreferences sharedPreferences;

    public PeripheralDataHelper(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    // Not secure?
    public void storeUsername(String username) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.apply();
    }

    // Not secure?
    public String retrieveUsername() {
        return sharedPreferences.getString("username", null);
    }


    // Not secure?
    public void storeToken(String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("personal_access_token", token);
        editor.apply();
    }

    // Not secure?
    public String retrieveToken() {
        return sharedPreferences.getString("personal_access_token", null);
    }

    public void deleteAllFilesInInternalStorage(Context context) {
        File internalStorageDir = context.getFilesDir();
        deleteRecursive(internalStorageDir);
    }

    public boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                boolean success = deleteRecursive(child);
                if (!success) {
                    return false;
                }
            }
        }
        return fileOrDirectory.delete();
    }
}
