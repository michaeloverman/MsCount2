/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.utils

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
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
class SettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val pref = findPreference(key)
        if (pref != null) {
            setPreferenceSummary(pref, sharedPreferences.getString(key, ""))
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle, rootKey: String) {
        addPreferencesFromResource(R.xml.pref_general)
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
            val listPref = preference
            val prefIndex = listPref.findIndexOfValue(stringValue)
            if (prefIndex >= 0) {
                preference.setSummary(listPref.entries[prefIndex])
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
        val entries = (activity as SettingsActivity?)!!.mEntries
        val values = (activity as SettingsActivity?)!!.mValues
        val lp = p as ListPreference
        lp.entries = entries
        lp.entryValues = values
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
}