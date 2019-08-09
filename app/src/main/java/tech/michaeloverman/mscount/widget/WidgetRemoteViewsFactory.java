/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import tech.michaeloverman.mscount.R;
import tech.michaeloverman.mscount.database.ProgramDatabaseSchema;
import tech.michaeloverman.mscount.programmed.ProgrammedMetronomeActivity;
import timber.log.Timber;

/**
 * Created by Michael on 4/14/2017.
 */

class WidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context mContext;
    private Cursor data = null;

    private static final String[] PROGRAM_COLUMNS = {
            ProgramDatabaseSchema.MetProgram._ID,
            ProgramDatabaseSchema.MetProgram.COLUMN_TITLE
    };
    private static final int INDEX_PROGRAM_ID = 0;
    private static final int INDEX_PROGRAM_TITLE = 1;


    public WidgetRemoteViewsFactory(Context context, Intent intent) {
        Timber.d("WidgetRemoteViewsFactory");
        mContext = context;

    }

    @Override
    public void onCreate() {
        Timber.d("onCreate()");
//        widgetManager = AppWidgetManager.getInstance(mContext);
    }

    @Override
    public void onDataSetChanged() {
        Timber.d("onDataSetChanged()");
        if(data != null) {
            data.close();
        }

        // temporarily clear identity
        final long identityToken = Binder.clearCallingIdentity();

        Uri localDatabaseLocation = ProgramDatabaseSchema.MetProgram.CONTENT_URI;
        data = mContext.getContentResolver().query(localDatabaseLocation,
                PROGRAM_COLUMNS,
                null,
                null,
                ProgramDatabaseSchema.MetProgram.COLUMN_TITLE + " ASC");

        Binder.restoreCallingIdentity(identityToken);
    }

    @Override
    public void onDestroy() {
        Timber.d("onDestroy()");
        if(data != null) {
            data.close();
            data = null;
        }
    }

    @Override
    public int getCount() {
        Timber.d("getCount()");
        return data == null ? 0 : data.getCount();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        Timber.d("getViewAt: %s", position);
        if(position == AdapterView.INVALID_POSITION ||
                data == null || !data.moveToPosition(position)) {
            return null;
        }
        RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item);

        String title = data.getString(INDEX_PROGRAM_TITLE);
        Timber.d("title: %s", title);
        views.setTextViewText(R.id.widget_item_title, title);

        final Intent fillInIntent = new Intent();
        Timber.d("ADDING id to fillInIntent: %s", data.getInt(INDEX_PROGRAM_ID));
        fillInIntent.putExtra(ProgrammedMetronomeActivity.PROGRAM_ID_EXTRA,
                data.getInt(INDEX_PROGRAM_ID));
        views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
        Timber.d("FillInIntent added to view, presumably!");

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        Timber.d("getloadingView()");
        return new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item);
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        Timber.d("getItemId: %s", position);
        if(data.moveToPosition(position)) {
            return data.getLong(INDEX_PROGRAM_ID);
        }
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
