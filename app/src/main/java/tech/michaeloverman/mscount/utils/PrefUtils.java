/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import tech.michaeloverman.mscount.R;
import timber.log.Timber;

/**
 * Created by Michael on 3/27/2017.
 */

public final class PrefUtils {

    private static final String PREF_DOWNBEAT_CLICK_DEFAULT = "4";
    private static final String PREF_INNERBEAT_CLICK_DEFAULT = "2";
    private static final String PREF_SUBDIVISION_CLICK_DEFAULT = "6";
    private static final String PREF_USE_FIREBASE = "use_firebase";
    private static final String PREF_CURRENT_TEMPO = "programmable_tempo_key";
    private static final String PREF_PIECE_KEY = "programmable_piece_id";
//    private static final String PREF_WEAR_STATUS_KEY = "pref_wear_status";

    // Suppress default constructor for noninstantiability
    private PrefUtils() {
    }

    public static int getDownBeatClickId(Context context) {
        SharedPreferences shp = PreferenceManager.getDefaultSharedPreferences(context);
        String id = shp.getString(context.getResources().getString(R.string.down_beat_click_key),
                PREF_DOWNBEAT_CLICK_DEFAULT);
        return Integer.parseInt(id);
    }

    public static int getInnerBeatClickId(Context context) {
        SharedPreferences shp = PreferenceManager.getDefaultSharedPreferences(context);
        String id = shp.getString(context.getResources().getString(R.string.inner_beat_click_key),
                PREF_INNERBEAT_CLICK_DEFAULT);
        return Integer.parseInt(id);
    }

    public static int getSubdivisionBeatClickId(Context context) {
        SharedPreferences shp = PreferenceManager.getDefaultSharedPreferences(context);
        String id = shp.getString(context.getResources().getString(R.string.subdivision_beat_click_key),
                PREF_SUBDIVISION_CLICK_DEFAULT);
        return Integer.parseInt(id);
    }
    public static void saveCurrentProgramToPrefs(Context context,
                                                boolean useFirebase, String key, int tempo) {
        SharedPreferences.Editor prefs = PreferenceManager
                .getDefaultSharedPreferences(context).edit();
        prefs.putBoolean(PREF_USE_FIREBASE, useFirebase);
        prefs.putString(PREF_PIECE_KEY, key);
        prefs.putInt(PREF_CURRENT_TEMPO, tempo);
        prefs.apply();
    }

    public static void saveWidgetSelectedPieceToPrefs(Context context, int key) {
        saveCurrentProgramToPrefs(context, false, Integer.toString(key), getSavedTempo(context));
    }

    public static String getSavedPieceKey(Context context) {
        SharedPreferences shp = PreferenceManager.getDefaultSharedPreferences(context);
        return shp.getString(PREF_PIECE_KEY, null);
    }

    public static int getSavedTempo(Context context) {
        SharedPreferences shp = PreferenceManager.getDefaultSharedPreferences(context);
        return shp.getInt(PREF_CURRENT_TEMPO, 120);
    }

    public static void saveFirebaseStatus(Context context, boolean firebase) {
        Timber.d("saving useFirebase: %s", firebase);
        SharedPreferences.Editor prefs = PreferenceManager
                .getDefaultSharedPreferences(context).edit();
        prefs.putBoolean(PREF_USE_FIREBASE, firebase);
        prefs.apply();
    }

    public static boolean usingFirebase(Context context) {
        SharedPreferences shp = PreferenceManager.getDefaultSharedPreferences(context);
        return shp.getBoolean(PREF_USE_FIREBASE, true);
    }

//    public static void saveWearStatus(Context context, boolean status) {
//        SharedPreferences.Editor prefs = PreferenceManager
//                .getDefaultSharedPreferences(context).edit();
//        prefs.putBoolean(PREF_WEAR_STATUS_KEY, status);
//        prefs.apply();
//    }
//
//    public static boolean wearPresent(Context context) {
//        SharedPreferences shp = PreferenceManager.getDefaultSharedPreferences(context);
//        return shp.getBoolean(PREF_WEAR_STATUS_KEY, false);
//    }
}
