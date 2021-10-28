/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.utils

import android.content.Context
import androidx.preference.PreferenceManager
import tech.michaeloverman.mscount.R
import timber.log.Timber

/**
 * Created by Michael on 3/27/2017.
 */
object PrefUtils {
    private const val PREF_DOWNBEAT_CLICK_DEFAULT = "4"
    private const val PREF_INNERBEAT_CLICK_DEFAULT = "2"
    private const val PREF_SUBDIVISION_CLICK_DEFAULT = "6"
    private const val PREF_USE_FIREBASE = "use_firebase"
    private const val PREF_CURRENT_TEMPO = "programmable_tempo_key"
    private const val PREF_PIECE_KEY = "programmable_piece_id"
    fun getDownBeatClickId(context: Context): Int {
        val shp = PreferenceManager.getDefaultSharedPreferences(context)
        val id = shp.getString(context.resources.getString(R.string.down_beat_click_key),
                PREF_DOWNBEAT_CLICK_DEFAULT)
        return id!!.toInt()
    }

    fun getInnerBeatClickId(context: Context): Int {
        val shp = PreferenceManager.getDefaultSharedPreferences(context)
        val id = shp.getString(context.resources.getString(R.string.inner_beat_click_key),
                PREF_INNERBEAT_CLICK_DEFAULT)
        return id!!.toInt()
    }

    fun getSubdivisionBeatClickId(context: Context): Int {
        val shp = PreferenceManager.getDefaultSharedPreferences(context)
        val id = shp.getString(context.resources.getString(R.string.subdivision_beat_click_key),
                PREF_SUBDIVISION_CLICK_DEFAULT)
        return id!!.toInt()
    }

    fun saveCurrentProgramToPrefs(context: Context?,
                                  useFirebase: Boolean, key: String?, tempo: Int) {
        val prefs = PreferenceManager
                .getDefaultSharedPreferences(context).edit()
        prefs.putBoolean(PREF_USE_FIREBASE, useFirebase)
        prefs.putString(PREF_PIECE_KEY, key)
        prefs.putInt(PREF_CURRENT_TEMPO, tempo)
        prefs.apply()
    }

    fun saveWidgetSelectedPieceToPrefs(context: Context?, key: Int) {
        saveCurrentProgramToPrefs(context, false, Integer.toString(key), getSavedTempo(context))
    }

    fun getSavedPieceKey(context: Context?): String? {
        val shp = PreferenceManager.getDefaultSharedPreferences(context)
        return shp.getString(PREF_PIECE_KEY, null)
    }

    fun getSavedTempo(context: Context?): Int {
        val shp = PreferenceManager.getDefaultSharedPreferences(context)
        return shp.getInt(PREF_CURRENT_TEMPO, 120)
    }

    fun saveFirebaseStatus(context: Context?, firebase: Boolean) {
        Timber.d("saving useFirebase: %s", firebase)
        val prefs = PreferenceManager
                .getDefaultSharedPreferences(context).edit()
        prefs.putBoolean(PREF_USE_FIREBASE, firebase)
        prefs.apply()
    }

    fun usingFirebase(context: Context?): Boolean {
        val shp = PreferenceManager.getDefaultSharedPreferences(context)
        return shp.getBoolean(PREF_USE_FIREBASE, true)
    } //    public static void saveWearStatus(Context context, boolean status) {
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