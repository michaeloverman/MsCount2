/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import timber.log.Timber;

import static tech.michaeloverman.mscount.database.ProgramDatabaseSchema.MetProgram;

/**
 * Extension of SQLiteOpenHelper, creating and setting up database of local program storage.
 *
 * Created by Michael on 3/27/2017.
 */

class ProgramDatabaseHelper extends SQLiteOpenHelper {

    private static final int VERSION = 2;
    private static final String DATABASE_NAME = "programDatabase.db";


    public ProgramDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
//        Timber.d("ProgramDatabaseHelper constructor");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Timber.d("Database Helper onCreate()");

        final String SQL_BUILDER_STRING = "CREATE TABLE " + MetProgram.TABLE_NAME + " ("
                + MetProgram._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + MetProgram.COLUMN_COMPOSER + " TEXT NOT NULL, "
                + MetProgram.COLUMN_TITLE + " TEXT NOT NULL, "
                + MetProgram.COLUMN_PRIMARY_SUBDIVISIONS + " INTEGER NOT NULL, "
                + MetProgram.COLUMN_COUNTOFF_SUBDIVISIONS + " INTEGER NOT NULL, "
                + MetProgram.COLUMN_DEFAULT_TEMPO + " INTEGER NOT NULL, "
                + MetProgram.COLUMN_DEFAULT_RHYTHM + " INTEGER NOT NULL, "
                + MetProgram.COLUMN_TEMPO_MULTIPLIER + " REAL, "
                + MetProgram.COLUMN_MEASURE_COUNT_OFFSET + " INTEGER, "
                + MetProgram.COLUMN_DATA_ARRAY + " TEXT NOT NULL, "
                + MetProgram.COLUMN_FIREBASE_ID + " TEXT, "
                + MetProgram.COLUMN_CREATOR_ID + " TEXT, "
                + MetProgram.COLUMN_DISPLAY_RHYTHM + " INTEGER NOT NULL);";

        db.execSQL(SQL_BUILDER_STRING);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //TODO This must be fixed before actual release to save users' data if/when db is updated
        if(oldVersion == 1 && newVersion == 2) {
            final String SQL_STRING = "ALTER TABLE " + MetProgram.TABLE_NAME + " "
                    + "ADD " + MetProgram.COLUMN_DISPLAY_RHYTHM + " INTEGER NOT NULL "
                    + "DEFAULT 0;";

            db.execSQL(SQL_STRING);
        }

    }
}
