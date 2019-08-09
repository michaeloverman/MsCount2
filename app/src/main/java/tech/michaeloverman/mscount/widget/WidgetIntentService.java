/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.widget;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;

import tech.michaeloverman.mscount.R;
import tech.michaeloverman.mscount.database.ProgramDatabaseSchema;
import tech.michaeloverman.mscount.favorites.FavoritesContract;
import tech.michaeloverman.mscount.programmed.ProgrammedMetronomeActivity;
import timber.log.Timber;

/**
 * Created by overm on 4/14/2017.
 */

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class WidgetIntentService extends IntentService {

    private static final String[] PROGRAM_COLUMNS = {
            ProgramDatabaseSchema.MetProgram._ID,
            ProgramDatabaseSchema.MetProgram.COLUMN_TITLE
    };
    // --Commented out by Inspection (5/13/2017 11:37 AM):static final int INDEX_PROGRAM_ID = 0;
    private static final int INDEX_COLUMN_TITLE = 1;

    public WidgetIntentService() {
        super("WidgetIntentService");
        Timber.d("WIDGET IntentService created");
    }



    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Timber.d("onHandleIntent()");
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        int[] widgetIds = widgetManager.getAppWidgetIds(new ComponentName(this,
                MsCountWidgetProvider.class));

        Uri programLocationUri = FavoritesContract.FavoriteEntry.CONTENT_URI;
        Cursor data = getContentResolver().query(programLocationUri,
                PROGRAM_COLUMNS,
                null,
                null,
                null);
        assert data != null;
        data.moveToFirst();

        for(int widgetId : widgetIds) {
            String title = data.getString(INDEX_COLUMN_TITLE);

            RemoteViews views = new RemoteViews(
                    getPackageName(),
                    R.layout.widget_list_item);
            views.setTextViewText(R.id.widget_item_title, title);

            Intent launchIntent = new Intent(this, ProgrammedMetronomeActivity.class);
            PendingIntent pendingIntent = PendingIntent
                    .getActivity(this, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            widgetManager.updateAppWidget(widgetId, views);
        }
        data.close();
    }

}

