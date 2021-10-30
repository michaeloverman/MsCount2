/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.utils

import android.content.Context
import androidx.preference.PreferenceManager
import tech.michaeloverman.mscount.R

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
    const val PREF_NORMAL_HELP = "normal_help_shown"
    const val PREF_ODD_HELP = "odd_help_shown"
    const val PREF_PROGRAM_HELP = "program_help_shown"
    const val PREF_DATA_HELP = "data_help_shown"
    const val PREF_META_HELP = "meta_help_shown"
    const val PREF_META_OPT_HELP = "meta_opt_help_shown"

    fun initialHelpShown(context: Context?, screen: String) : Boolean =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(screen, false)

    fun helpScreenShown(context: Context?, screen: String) =
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(screen, true)
            .apply()

    fun getDownBeatClickId(context: Context): Int =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(context.getString(R.string.down_beat_click_key),
                PREF_DOWNBEAT_CLICK_DEFAULT)!!
            .toInt()

    fun getInnerBeatClickId(context: Context): Int =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(context.getString(R.string.inner_beat_click_key),
                PREF_INNERBEAT_CLICK_DEFAULT)!!
            .toInt()

    fun getSubdivisionBeatClickId(context: Context): Int =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(context.resources.getString(R.string.subdivision_beat_click_key), PREF_SUBDIVISION_CLICK_DEFAULT)!!
            .toInt()

    fun saveCurrentProgramToPrefs(context: Context?,
                                  useFirebase: Boolean,
                                  key: String?, tempo: Int) =
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(PREF_USE_FIREBASE, useFirebase)
            .putString(PREF_PIECE_KEY, key)
            .putInt(PREF_CURRENT_TEMPO, tempo)
            .apply()

    fun getSavedPieceKey(context: Context?): String? =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(PREF_PIECE_KEY, null)

    fun getSavedTempo(context: Context?): Int =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(PREF_CURRENT_TEMPO, 120)

    fun saveFirebaseStatus(context: Context?, firebase: Boolean) =
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(PREF_USE_FIREBASE, firebase)
            .apply()

    fun usingFirebase(context: Context?): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(PREF_USE_FIREBASE, true)
}