/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.database;

import android.net.Uri;
import android.provider.BaseColumns;

import timber.log.Timber;

/**
 * The table definitions for SQLite database of local program data storage.
 * Created by Michael on 3/27/2017.
 */

public class ProgramDatabaseSchema {
    static final String AUTHORITY = "tech.michaeloverman.android.mscount";
    private static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);
    public static final String PATH_PROGRAMS = "programs";

    public static final class MetProgram implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_URI.buildUpon()
                .appendPath(PATH_PROGRAMS)
                .build();

        public static final String TABLE_NAME = "programs";

//        public static final String _ID = "";
        public static final String COLUMN_COMPOSER = "composer";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_PRIMARY_SUBDIVISIONS = "primary_subs";
        public static final String COLUMN_COUNTOFF_SUBDIVISIONS = "countoff_subs";
        public static final String COLUMN_DEFAULT_TEMPO = "default_tempo";
        public static final String COLUMN_DEFAULT_RHYTHM = "default_rhythm";
        public static final String COLUMN_TEMPO_MULTIPLIER = "tempo_multiplier";
        public static final String COLUMN_MEASURE_COUNT_OFFSET = "measure_count_offset";
        public static final String COLUMN_DATA_ARRAY = "actual_data";
        public static final String COLUMN_FIREBASE_ID = "firebase_id";
        public static final String COLUMN_CREATOR_ID = "creator_id";
        public static final String COLUMN_DISPLAY_RHYTHM = "display_ryhthm";

        public static final int POSITION_ID = 0;
        public static final int POSITION_COMPOSER = 1;
        public static final int POSITION_TITLE = 2;
        public static final int POSITION_PRIMANY_SUBDIVISIONS = 3;
        public static final int POSITION_COUNOFF_SUBDIVISIONS = 4;
        public static final int POSITION_DEFAULT_TEMPO = 5;
        public static final int POSITION_DEFAULT_RHYTHM = 6;
        public static final int POSITION_TEMPO_MULTIPLIER = 7;
        public static final int POSITION_MEASURE_COUNTE_OFFSET = 8;
        public static final int POSITION_DATA_ARRAY = 9;
        public static final int POSITION_FIREBASE_ID = 10;
        public static final int POSITION_CREATOR_ID = 11;
        public static final int POSITION_DISPLAY_RHYTHM = 12;

        @SuppressWarnings("unused")
        public static Uri buildUriWithComposer(String composer) {
            Timber.d("buildUriWithComposer");
            return CONTENT_URI.buildUpon()
                    .appendPath(composer)
                    .build();
        }

        @SuppressWarnings("unused")
        public static Uri buildUriWithId(int id) {
            return CONTENT_URI.buildUpon()
                    .appendPath(Integer.toString(id))
                    .build();
        }

    }
}
