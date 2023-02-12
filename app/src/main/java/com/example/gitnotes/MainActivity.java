package com.example.gitnotes;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private String repositoryLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar(findViewById(R.id.toolbar));

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
                InputDialogFragment dialog = InputDialogFragment.newInstance(sharedText);
                dialog.show(getSupportFragmentManager(), InputDialogFragment.TAG);
            }
        }
    }
}

