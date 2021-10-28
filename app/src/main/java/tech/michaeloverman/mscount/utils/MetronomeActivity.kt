/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.utils

import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import tech.michaeloverman.mscount.R
import tech.michaeloverman.mscount.SingleFragmentActivity

/**
 * Abstract parent class for various metronomes. Handles click sound settings.
 * Created by Michael on 4/5/2017.
 */
abstract class MetronomeActivity : SingleFragmentActivity() {
    protected var mMetronome: Metronome? = null
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.global_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.settings) {
            gotoSettings()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    /**
     * Attaches click file names to Settings intent for setting menu
     */
    private fun gotoSettings() {
        val list = ClickSounds.clicks
        val size = list.size
        val entries = arrayOfNulls<String>(size)
        val values = arrayOfNulls<String>(size)
        for (i in 0 until size) {
            entries[i] = list[i].name
            values[i] = list[i].soundId.toString()
        }
        val intent = Intent(this, SettingsActivity::class.java)
        intent.putExtra(EXTRA_ENTRIES, entries)
        intent.putExtra(EXTRA_VALUES, values)
        startActivity(intent)
    }

    companion object {
        const val EXTRA_ENTRIES = "entries_for_settings"
        const val EXTRA_VALUES = "values_for_settings"
    }
}