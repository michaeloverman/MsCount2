/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.favorites;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper class for accessing SQLite database of pieces marked as favorites.
 *
 * Created by Michael on 12/20/2016.
 */

public class FavoritesDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "favorite_pieces.db";
    private static final int DATABASE_VERSION = 1;

    public FavoritesDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
//        Log.d(TAG, ".... in constructor...");
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_FAVORITE_TABLE =
                "CREATE TABLE " + FavoritesContract.FavoriteEntry.TABLE_NAME + " (" +
                        FavoritesContract.FavoriteEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        FavoritesContract.FavoriteEntry.COLUMN_PIECE_ID + " STRING NOT NULL, " +
                        " UNIQUE (" + FavoritesContract.FavoriteEntry.COLUMN_PIECE_ID +
                        ") ON CONFLICT REPLACE);";
        sqLiteDatabase.execSQL(SQL_CREATE_FAVORITE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

}
