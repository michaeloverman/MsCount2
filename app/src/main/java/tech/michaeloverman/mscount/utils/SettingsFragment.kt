/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import tech.michaeloverman.mscount.R

/**
 * Handles click option settings - get's list of clicks available, assigns selected options
 * to appropriate SharedPref.
 *
 * Created by Michael on 4/4/2017.
 */
class SettingsFragment private constructor() : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {

    lateinit var mEntries: Array<String>
    lateinit var mValues: Array<String>
    private lateinit var localContext: AppCompatActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        // get setting list file names from originating arguments
        // these must be defined before calling super, as super sets them up
        arguments?.let {
            mEntries = it.getStringArrayList(EXTRA_ENTRIES)?.toTypedArray() ?: arrayOf("")
            mValues = it.getStringArrayList(EXTRA_VALUES)?.toTypedArray() ?: arrayOf("")
        }

        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        localContext = context as AppCompatActivity
        localContext.actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            localContext.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val pref: Preference? = findPreference(key)
        if (pref != null) {
            setPreferenceSummary(pref, sharedPreferences.getString(key, ""))
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_general, rootKey)
        val shPref = preferenceScreen.sharedPreferences
        val prefScreen = preferenceScreen
        val count = prefScreen.preferenceCount
        for (i in 0 until count) {
            val p = prefScreen.getPreference(i)
            (p as? ListPreference)?.let { setEntriesAndValues(it) }
            val value = shPref.getString(p.key, "")
            setPreferenceSummary(p, value)
        }
    }

    /**
     * Sets the summary description of the setting variable
     * @param preference the actual preference item to summarize
     * @param value the summary itself
     */
    private fun setPreferenceSummary(preference: Preference, value: Any?) {
        val stringValue = value.toString()
        if (preference is ListPreference) {
            val prefIndex = preference.findIndexOfValue(stringValue)
            if (prefIndex >= 0) {
                preference.setSummary(preference.entries[prefIndex])
            }
        } else {
            preference.summary = stringValue
        }
    }

    /**
     * Pulls list of available click sounds from SettingsActivity, inserts into settings item
     * @param p the preference to which entries and values is to be assigned
     */
    private fun setEntriesAndValues(p: Preference) {
//        val entries = (activity as SettingsActivity?)!!.mEntries
//        val values = (activity as SettingsActivity?)!!.mValues
        val lp = p as ListPreference
        lp.entries = mEntries
        lp.entryValues = mValues
    }

    override fun onStart() {
        super.onStart()
        preferenceScreen.sharedPreferences
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        preferenceScreen.sharedPreferences
                .unregisterOnSharedPreferenceChangeListener(this)
    }

    companion object {
        private const val EXTRA_ENTRIES = "entries_for_settings"
        private const val EXTRA_VALUES = "values_for_settings"
        fun getInstance(entries: ArrayList<String>, values: ArrayList<String>) : SettingsFragment =
            SettingsFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(EXTRA_ENTRIES, entries)
                    putStringArrayList(EXTRA_VALUES, values)
                }
            }
    }
}