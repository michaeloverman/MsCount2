/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.favorites

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import tech.michaeloverman.mscount.favorites.FavoritesDBHelper
import android.database.sqlite.SQLiteDatabase
import tech.michaeloverman.mscount.favorites.FavoritesContract

/**
 * Helper class for accessing SQLite database of pieces marked as favorites.
 *
 * Created by Michael on 12/20/2016.
 */
class FavoritesDBHelper(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        val SQL_CREATE_FAVORITE_TABLE = "CREATE TABLE " + FavoritesContract.FavoriteEntry.TABLE_NAME + " (" +
                FavoritesContract.FavoriteEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                FavoritesContract.FavoriteEntry.COLUMN_PIECE_ID + " STRING NOT NULL, " +
                " UNIQUE (" + FavoritesContract.FavoriteEntry.COLUMN_PIECE_ID +
                ") ON CONFLICT REPLACE);"
        sqLiteDatabase.execSQL(SQL_CREATE_FAVORITE_TABLE)
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, i: Int, i1: Int) {}

    companion object {
        private const val DATABASE_NAME = "favorite_pieces.db"
        private const val DATABASE_VERSION = 1
    }
}