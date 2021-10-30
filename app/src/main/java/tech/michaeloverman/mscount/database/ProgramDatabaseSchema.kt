/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.database

import android.net.Uri
import android.provider.BaseColumns
import timber.log.Timber

/**
 * The table definitions for SQLite database of local program data storage.
 * Created by Michael on 3/27/2017.
 */
object ProgramDatabaseSchema {
    const val AUTHORITY = "tech.michaeloverman.android.mscount"
    private val BASE_URI = Uri.parse("content://" + AUTHORITY)
    const val PATH_PROGRAMS = "programs"

    object MetProgram : BaseColumns {
        val CONTENT_URI = BASE_URI.buildUpon()
                .appendPath(PATH_PROGRAMS)
                .build()
        const val TABLE_NAME = "programs"

        const val _ID = "_id";
        const val COLUMN_COMPOSER = "composer"
        const val COLUMN_TITLE = "title"
        const val COLUMN_PRIMARY_SUBDIVISIONS = "primary_subs"
        const val COLUMN_COUNTOFF_SUBDIVISIONS = "countoff_subs"
        const val COLUMN_DEFAULT_TEMPO = "default_tempo"
        const val COLUMN_DEFAULT_RHYTHM = "default_rhythm"
        const val COLUMN_TEMPO_MULTIPLIER = "tempo_multiplier"
        const val COLUMN_MEASURE_COUNT_OFFSET = "measure_count_offset"
        const val COLUMN_DATA_ARRAY = "actual_data"
        const val COLUMN_FIREBASE_ID = "firebase_id"
        const val COLUMN_CREATOR_ID = "creator_id"
        const val COLUMN_DISPLAY_RHYTHM = "display_ryhthm"
        const val POSITION_ID = 0
        const val POSITION_COMPOSER = 1
        const val POSITION_TITLE = 2
        const val POSITION_PRIMANY_SUBDIVISIONS = 3
        const val POSITION_COUNOFF_SUBDIVISIONS = 4
        const val POSITION_DEFAULT_TEMPO = 5
        const val POSITION_DEFAULT_RHYTHM = 6
        const val POSITION_TEMPO_MULTIPLIER = 7
        const val POSITION_MEASURE_COUNTE_OFFSET = 8
        const val POSITION_DATA_ARRAY = 9
        const val POSITION_FIREBASE_ID = 10
        const val POSITION_CREATOR_ID = 11
        const val POSITION_DISPLAY_RHYTHM = 12
        fun buildUriWithComposer(composer: String?): Uri {
            Timber.d("buildUriWithComposer")
            return CONTENT_URI.buildUpon()
                    .appendPath(composer)
                    .build()
        }

        fun buildUriWithId(id: Int): Uri {
            return CONTENT_URI.buildUpon()
                    .appendPath(Integer.toString(id))
                    .build()
        }
    }
}