/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.utils;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import tech.michaeloverman.mscount.R;

/**
 * Handles click option settings - get's list of clicks available, assigns selected options
 * to appropriate SharedPref.
 *
 * Created by Michael on 4/4/2017.
 */

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if(pref != null) {
            setPreferenceSummary(pref, sharedPreferences.getString(key, ""));
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_general);

        SharedPreferences shPref = getPreferenceScreen().getSharedPreferences();
        PreferenceScreen prefScreen = getPreferenceScreen();
        int count = prefScreen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference p = prefScreen.getPreference(i);
            if(p instanceof ListPreference) {
                // get list of available clicks, insert in settings
                setEntriesAndValues(p);
            }
            String value = shPref.getString(p.getKey(), "");
            setPreferenceSummary(p, value);
        }

    }

    /**
     * Sets the summary description of the setting variable
     * @param preference the actual preference item to summarize
     * @param value the summary itself
     */
    private void setPreferenceSummary(Preference preference, Object value) {
        String stringValue = value.toString();

        if (preference instanceof ListPreference) {
            ListPreference listPref = (ListPreference) preference;
            int prefIndex = listPref.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPref.getEntries()[prefIndex]);
            }
        } else {
            preference.setSummary(stringValue);
        }
    }

    /**
     * Pulls list of available click sounds from SettingsActivity, inserts into settings item
     * @param p the preference to which entries and values is to be assigned
     */
    private void setEntriesAndValues(Preference p) {
        String[] entries = ((SettingsActivity)getActivity()).mEntries;
        String[] values = ((SettingsActivity)getActivity()).mValues;

        ListPreference lp = (ListPreference) p;
        lp.setEntries(entries);
        lp.setEntryValues(values);
    }

    @Override
    public void onStart() {
        super.onStart();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
