/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.utils

import android.app.AlertDialog
import android.content.DialogInterface
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import tech.michaeloverman.mscount.R
import tech.michaeloverman.mscount.dataentry.MetaDataEntryFragment

/**
 * Abstract parent class for various metronomes. Handles click sound settings.
 * Created by Michael on 4/5/2017.
 */
abstract class MetronomeActivity : AppCompatActivity() {
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

    override fun onBackPressed() {
        val f = this.supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (f is MetaDataEntryFragment) {
            losingDataAlertDialog()
        } else {
            super.onBackPressed()
        }
    }

    private fun losingDataAlertDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.erase_data)
            .setMessage(R.string.leave_without_save)
            .setPositiveButton(R.string.leave) { _: DialogInterface?, _: Int -> super.onBackPressed() }
            .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .show()
    }

    /**
     * Attaches click file names to Settings intent for setting menu
     */
    private fun gotoSettings() {
        val list = ClickSounds.clicks
        val entries = arrayListOf<String>()
        val values = arrayListOf<String>()
        list.forEach {
            entries.add(it.name)
            values.add(it.soundId.toString())
        }
        val frag = SettingsFragment.getInstance(entries, values)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, frag)
            .addToBackStack("settings")
            .commit()
//        val intent = Intent(this, SettingsActivity::class.java)
//        intent.putExtra(EXTRA_ENTRIES, entries)
//        intent.putExtra(EXTRA_VALUES, values)
//        startActivity(intent)
    }

    companion object {
    }
}