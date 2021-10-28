/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.utils

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import tech.michaeloverman.mscount.R

/**
 * Created by Michael on 4/4/2017.
 */
class SettingsActivity : AppCompatActivity() {
    lateinit var mEntries: Array<String>
    lateinit var mValues: Array<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // get setting list file names from originating intent
        val intent = intent
        mEntries = intent.getStringArrayExtra(MetronomeActivity.EXTRA_ENTRIES) as Array<String>
        mValues = intent.getStringArrayExtra(MetronomeActivity.EXTRA_VALUES) as Array<String>
        setContentView(R.layout.settings_activity)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}