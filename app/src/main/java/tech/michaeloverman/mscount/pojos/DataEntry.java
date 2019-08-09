/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.pojos;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * POJO for a single piece of data: an integer value and a boolean. Boolean whether it is a barline
 * or not, value is the measure number for barlines, and the number of subdivisions otherwise.
 * Created by Michael on 3/13/2017.
 */

public class DataEntry implements Serializable {

    private int mData;
    private boolean isBarline;

    public DataEntry(int data, boolean bar) {
        mData = data;
        isBarline = bar;
    }

    public DataEntry() { }

    public boolean isBarline() {
        return isBarline;
    }

    public void setBarline(boolean is) {
        isBarline = is;
    }

    public int getData() {
        return mData;
    }

    public void setData(int data) {
        mData = data;
    }

    @NotNull
    @Override
    public String toString() {
        return mData + ";" + isBarline;
    }
}
