/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.database

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.widget.Toast
import tech.michaeloverman.mscount.favorites.FavoritesProvider
import timber.log.Timber

/**
 * Handles database actions: query, insert, delete
 * Created by Michael on 3/27/2017.
 */
class ProgramProvider : ContentProvider() {
    private var mHelper: ProgramDatabaseHelper? = null
    override fun onCreate(): Boolean {
        mHelper = ProgramDatabaseHelper(context)
        Timber.d("onCreate()")
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        var selectionArgs = selectionArgs
        val cursor: Cursor
        Timber.d("query()")
        when (sUriMatcher.match(uri)) {
            CODE_COMPOSER -> {
                Timber.d("UriMatcher: CODE_COMPOSER")
                val composer = uri.lastPathSegment
                selectionArgs = arrayOf(composer.toString())
                cursor = mHelper!!.readableDatabase.query(
                        ProgramDatabaseSchema.MetProgram.TABLE_NAME,
                        null,
                        ProgramDatabaseSchema.MetProgram.COLUMN_COMPOSER + " = ? ",
                        selectionArgs,
                        null,
                        null,
                        sortOrder)
            }
            CODE_COMPOSER_WITH_PIECE -> {
                Timber.d("UriMatcher: CODE_COMPOSER_WITH_PIECE")
                val title = uri.lastPathSegment
                selectionArgs = arrayOf(title.toString())
                cursor = mHelper!!.readableDatabase.query(
                        ProgramDatabaseSchema.MetProgram.TABLE_NAME,
                        null,
                        ProgramDatabaseSchema.MetProgram.COLUMN_TITLE + " = ? ",
                        selectionArgs,
                        null,
                        null,
                        sortOrder)
            }
            CODE_ALL_OF_IT -> {
                Timber.d("UriMatcher: CODE_ALL_OF_IT")
                cursor = mHelper!!.readableDatabase.query(
                        ProgramDatabaseSchema.MetProgram.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder)
            }
            else -> throw UnsupportedOperationException("Unknown uri: $uri")
        }
        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        Timber.d("insert() ~~~!!!")
        val db = mHelper!!.writableDatabase
        val returnUri: Uri?
        Timber.d("switch(sUriMatcher.match(uri) %s", sUriMatcher.match(uri))
        if (sUriMatcher.match(uri) == CODE_ALL_OF_IT) {
            Timber.d("case CODE_ALL_OF_IT")
            db.beginTransaction()
            try {
                val lineNumber = getLineNumberInDatabase(db,
                        values?.get(
                                ProgramDatabaseSchema.MetProgram.COLUMN_COMPOSER)?.toString(),
                        values?.get(
                                ProgramDatabaseSchema.MetProgram.COLUMN_TITLE)?.toString())
                val _id: Long
                _id = if (lineNumber == -1) {
                    db.insert(ProgramDatabaseSchema.MetProgram.TABLE_NAME,
                            null, values)
                } else {
                    db.update(ProgramDatabaseSchema.MetProgram.TABLE_NAME, values,
                            "_id=?", arrayOf(Integer.toString(lineNumber))).toLong()
                }
                Timber.d("long returned = %s", _id)
                if (_id == -1L) {
                    Toast.makeText(context, "Problem saving to database!!", Toast.LENGTH_SHORT).show()
                    returnUri = null
                } else {
                    db.setTransactionSuccessful()
                    returnUri = ProgramDatabaseSchema.MetProgram.CONTENT_URI
                    context!!.contentResolver.notifyChange(uri, null)
                }
            } finally {
                db.endTransaction()
            }
        } else {
            Timber.d("switch DEFAULT")
            Toast.makeText(context, "Database request improperly formatted.", Toast.LENGTH_SHORT).show()
            returnUri = null
        }
        sendUpdateIntent()
        return returnUri
    }

    private fun sendUpdateIntent() {
        Timber.d("sending data_update intent!!!!")
        val dataUpdateIntent = Intent(FavoritesProvider.ACTION_DATA_UPDATED)
        context!!.sendBroadcast(dataUpdateIntent)
    }

    private fun getLineNumberInDatabase(db: SQLiteDatabase, composer: String?, title: String?): Int {
        val c = db.query(ProgramDatabaseSchema.MetProgram.TABLE_NAME, arrayOf("_id"), ProgramDatabaseSchema.MetProgram.COLUMN_COMPOSER + "=? AND "
                + ProgramDatabaseSchema.MetProgram.COLUMN_TITLE + "=?", arrayOf(composer, title),
                null, null, null, null)
        if (c.moveToFirst()) {
            val position = c.getInt(ProgramDatabaseSchema.MetProgram.POSITION_ID)
            c.close()
            return position
        }
        c.close()
        return -1
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        Timber.d("delete()...")
        val db = mHelper!!.writableDatabase
        val rowsDeleted: Int
        if (sUriMatcher.match(uri) == CODE_ALL_OF_IT) {
            rowsDeleted = db.delete(ProgramDatabaseSchema.MetProgram.TABLE_NAME,
                    selection,
                    selectionArgs)
            Timber.d("%s rows deleted", rowsDeleted)
        } else {
            throw UnsupportedOperationException("Unknown URI: $uri")
        }
        if (rowsDeleted != 0) {
            val context = context
            context?.contentResolver?.notifyChange(uri, null)
        }
        sendUpdateIntent()
        return rowsDeleted
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        Timber.d("update...()")
        return 0
    }

    companion object {
        private const val CODE_ALL_OF_IT = 100
        private const val CODE_COMPOSER = 101
        private const val CODE_COMPOSER_WITH_PIECE = 102
        private val sUriMatcher = buildUriMatcher()
        private fun buildUriMatcher(): UriMatcher {
            Timber.d("UriMatcher creation...")
            val matcher = UriMatcher(UriMatcher.NO_MATCH)
            val authority = ProgramDatabaseSchema.AUTHORITY
            matcher.addURI(authority, ProgramDatabaseSchema.PATH_PROGRAMS, CODE_ALL_OF_IT)
            matcher.addURI(authority, ProgramDatabaseSchema.PATH_PROGRAMS + "/*", CODE_COMPOSER)
            matcher.addURI(authority, ProgramDatabaseSchema.PATH_PROGRAMS + "/*/*", CODE_COMPOSER_WITH_PIECE)
            return matcher
        }
    }
}