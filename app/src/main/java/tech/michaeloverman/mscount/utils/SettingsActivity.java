/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.utils;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import tech.michaeloverman.mscount.R;


/**
 * Created by Michael on 4/4/2017.
 */

public class SettingsActivity extends AppCompatActivity {

    String[] mEntries;
    String[] mValues;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get setting list file names from originating intent
        Intent intent = getIntent();
        mEntries = intent.getStringArrayExtra(MetronomeActivity.EXTRA_ENTRIES);
        mValues = intent.getStringArrayExtra(MetronomeActivity.EXTRA_VALUES);

        setContentView(R.layout.settings_activity);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if(id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
