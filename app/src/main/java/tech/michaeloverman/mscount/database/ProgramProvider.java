/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import timber.log.Timber;

import static tech.michaeloverman.mscount.favorites.FavoritesProvider.ACTION_DATA_UPDATED;


/**
 * Handles database actions: query, insert, delete
 * Created by Michael on 3/27/2017.
 */

public class ProgramProvider extends ContentProvider {

    private static final int CODE_ALL_OF_IT = 100;
    private static final int CODE_COMPOSER = 101;
    private static final int CODE_COMPOSER_WITH_PIECE = 102;

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private ProgramDatabaseHelper mHelper;

    private static UriMatcher buildUriMatcher() {
        Timber.d("UriMatcher creation...");
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = ProgramDatabaseSchema.AUTHORITY;

        matcher.addURI(authority, ProgramDatabaseSchema.PATH_PROGRAMS, CODE_ALL_OF_IT);
        matcher.addURI(authority, ProgramDatabaseSchema.PATH_PROGRAMS + "/*", CODE_COMPOSER);
        matcher.addURI(authority, ProgramDatabaseSchema.PATH_PROGRAMS + "/*/*", CODE_COMPOSER_WITH_PIECE);

        return matcher;
    }


    @Override
    public boolean onCreate() {
        mHelper = new ProgramDatabaseHelper(getContext());
        Timber.d("onCreate()");
        return true;
    }

    @SuppressWarnings("ConstantConditions")
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Cursor cursor;

        Timber.d("query()");

        switch (sUriMatcher.match(uri)) {
            case CODE_COMPOSER:
                Timber.d("UriMatcher: CODE_COMPOSER");
                String composer = uri.getLastPathSegment();
                selectionArgs = new String[]{composer};

                cursor = mHelper.getReadableDatabase().query(
                        ProgramDatabaseSchema.MetProgram.TABLE_NAME,
                        null,
                        ProgramDatabaseSchema.MetProgram.COLUMN_COMPOSER + " = ? ",
                        selectionArgs,
                        null,
                        null,
                        sortOrder);

                break;

            case CODE_COMPOSER_WITH_PIECE:
                Timber.d("UriMatcher: CODE_COMPOSER_WITH_PIECE");
                String title = uri.getLastPathSegment();
                selectionArgs = new String[]{title};
                cursor = mHelper.getReadableDatabase().query(
                        ProgramDatabaseSchema.MetProgram.TABLE_NAME,
                        null,
                        ProgramDatabaseSchema.MetProgram.COLUMN_TITLE + " = ? ",
                        selectionArgs,
                        null,
                        null,
                        sortOrder);

                break;
            case CODE_ALL_OF_IT:
                Timber.d("UriMatcher: CODE_ALL_OF_IT");
                cursor = mHelper.getReadableDatabase().query(
                        ProgramDatabaseSchema.MetProgram.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);

                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @SuppressWarnings("ConstantConditions")
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        Timber.d("insert() ~~~!!!");
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Uri returnUri;

        Timber.d("switch(sUriMatcher.match(uri) %s", sUriMatcher.match(uri));
        if (sUriMatcher.match(uri) == CODE_ALL_OF_IT) {
            Timber.d("case CODE_ALL_OF_IT");
            db.beginTransaction();
            try {
                int lineNumber = getLineNumberInDatabase(db,
                        values != null ? values.get(
                                ProgramDatabaseSchema.MetProgram.COLUMN_COMPOSER).toString()
                                : null,
                        values != null ? values.get(
                                ProgramDatabaseSchema.MetProgram.COLUMN_TITLE).toString()
                                : null);
                long _id;
                if (lineNumber == -1) {
                    _id = db.insert(ProgramDatabaseSchema.MetProgram.TABLE_NAME,
                            null, values);
                } else {
            
                    _id = db.update(ProgramDatabaseSchema.MetProgram.TABLE_NAME, values,
                            "_id=?", new String[]{Integer.toString(lineNumber)});
                }
                Timber.d("long returned = %s", _id);
                if (_id == -1) {
                    Toast.makeText(getContext(), "Problem saving to database!!", Toast.LENGTH_SHORT).show();
                    returnUri = null;
                } else {
                    db.setTransactionSuccessful();
                    returnUri = ProgramDatabaseSchema.MetProgram.CONTENT_URI;
                    getContext().getContentResolver().notifyChange(uri, null);
                }
        
            } finally {
                db.endTransaction();
            }
    
        } else {
            Timber.d("switch DEFAULT");
            Toast.makeText(getContext(), "Database request improperly formatted.", Toast.LENGTH_SHORT).show();
            returnUri = null;
        }
        sendUpdateIntent();
        return returnUri;
    }

    @SuppressWarnings("ConstantConditions")
    private void sendUpdateIntent() {
        Timber.d("sending data_update intent!!!!");
        Intent dataUpdateIntent = new Intent(ACTION_DATA_UPDATED);
        getContext().sendBroadcast(dataUpdateIntent);
    }

    private int getLineNumberInDatabase(SQLiteDatabase db, String composer, String title) {
        Cursor c = db.query(ProgramDatabaseSchema.MetProgram.TABLE_NAME,
                new String[]{"_id"}, ProgramDatabaseSchema.MetProgram.COLUMN_COMPOSER + "=? AND "
                        + ProgramDatabaseSchema.MetProgram.COLUMN_TITLE + "=?", new String[]{composer, title},
                null, null, null, null);
        if (c.moveToFirst()) {
            int position = c.getInt(ProgramDatabaseSchema.MetProgram.POSITION_ID);
            c.close();
            return position;
        }
        c.close();
        return -1;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        Timber.d("delete()...");
        final SQLiteDatabase db = mHelper.getWritableDatabase();
        int rowsDeleted;

        if (sUriMatcher.match(uri) == CODE_ALL_OF_IT) {
            rowsDeleted = db.delete(ProgramDatabaseSchema.MetProgram.TABLE_NAME,
                    selection,
                    selectionArgs);
            Timber.d("%s rows deleted", rowsDeleted);
        } else {
            throw new UnsupportedOperationException("Unknown URI: " + uri);
        }

        if (rowsDeleted != 0) {
            Context context = getContext();
            if (context != null) {
                context.getContentResolver().notifyChange(uri, null);
            }
        }
        sendUpdateIntent();
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        Timber.d("update...()");
        return 0;
    }
}
