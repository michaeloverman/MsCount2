/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.favorites

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri

/**
 * Class to access SQLite database storing movies marked as favorites.
 *
 * Created by Michael on 12/20/2016.
 */
class FavoritesProvider : ContentProvider() {
    private var mDBHelper: FavoritesDBHelper? = null
    override fun onCreate(): Boolean {
        mDBHelper = FavoritesDBHelper(context)
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?,
                       selection: String?, selectionArgs: Array<String>?,
                       sortOrder: String?): Cursor? {
        val cursor: Cursor = if (sUriMatcher.match(uri) == CODE_FAVORITES) {
            mDBHelper!!.readableDatabase.query(
                    FavoritesContract.FavoriteEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder)
        } else {
            throw UnsupportedOperationException("Unknown uri: $uri")
        }
        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String? {
        throw RuntimeException("getType not implemented")
    }

    override fun insert(uri: Uri, value: ContentValues?): Uri? {
        val db = mDBHelper!!.writableDatabase
        val _id: Long
        if (sUriMatcher.match(uri) == CODE_FAVORITES) {
            db.beginTransaction()
            var rowsInserted = 0
            try {
//                    int movieId = value.getAsInteger(FavoritesContract.FavoriteEntry.COLUMN_MOVIE_ID);
                _id = db.insert(FavoritesContract.FavoriteEntry.TABLE_NAME, null, value)
                if (_id != -1L) {
                    rowsInserted++
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            if (rowsInserted > 0) {
                context!!.contentResolver.notifyChange(uri, null)
            }
            return uri
        }

//        sendUpdateIntent();
        return uri
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        var selection = selection
        val numRowsDeleted: Int
        if (selection == null) selection = "1"
        numRowsDeleted = if (sUriMatcher.match(uri) == CODE_FAVORITES) {
            mDBHelper!!.writableDatabase.delete(
                    FavoritesContract.FavoriteEntry.TABLE_NAME,
                    selection,
                    selectionArgs)
        } else {
            throw UnsupportedOperationException("Unknown uri: $uri")
        }
        if (numRowsDeleted != 0) {
            context!!.contentResolver.notifyChange(uri, null)
        }

//        sendUpdateIntent();
        return numRowsDeleted
    }

    override fun update(uri: Uri, contentValues: ContentValues?, s: String?, strings: Array<String>?): Int {
        return 0
    }

    companion object {
        private const val CODE_FAVORITES = 222
        private val sUriMatcher = buildUriMatcher()
        const val ACTION_DATA_UPDATED = "tech.michaeloverman.android.mscount.ACTION_DATA_UPDATED"
        private fun buildUriMatcher(): UriMatcher {
            val matcher = UriMatcher(UriMatcher.NO_MATCH)
            val authority = FavoritesContract.CONTENT_AUTHORITY
            matcher.addURI(authority, FavoritesContract.PATH_FAVORITES, CODE_FAVORITES)
            return matcher
        }
    }
}