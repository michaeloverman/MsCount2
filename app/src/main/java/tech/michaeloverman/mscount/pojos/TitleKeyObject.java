/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.pojos;


import androidx.annotation.NonNull;

import timber.log.Timber;


/**
 * Very simple POJO for pairing a program title and its Firebase key. Used primarily by
 * the Favorites database.
 *
 * Created by Michael on 2/25/2017.
 */

public class TitleKeyObject implements Comparable {
    
    private String mTitle;
    private String mKey;

    public TitleKeyObject(String title, String key) {
        Timber.d("TitleKeyObject constructor(): " + title + ", " + key);
        mTitle = title;
        mKey = key;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getKey() {
        return mKey;
    }

    public void setKey(String key) {
        mKey = key;
    }

    @Override
    public int compareTo(@NonNull Object o) {
        TitleKeyObject other = (TitleKeyObject) o;
        return mTitle.compareTo(other.mTitle);
    }
}
