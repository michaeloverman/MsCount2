/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

import timber.log.Timber;

/**
 * Created by Michael on 4/14/2017.
 */

public class WidgetRemoteViewsService extends RemoteViewsService {

    public WidgetRemoteViewsService() {
        super();
        Timber.d("WidgetRemoteViewsService()!!!");
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        Timber.d("onGetViewFactory()");
        return new WidgetRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}
