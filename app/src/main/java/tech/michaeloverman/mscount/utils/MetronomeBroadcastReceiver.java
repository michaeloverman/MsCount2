///* Copyright (C) 2017 Michael Overman - All Rights Reserved */
//package tech.michaeloverman.mscount.utils;
//
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//
//import timber.log.Timber;
//
///**
// * Interface for wear related broadcasts
// * Created by Michael on 4/29/2017.
// */
//
//public class MetronomeBroadcastReceiver extends BroadcastReceiver {
//
//    private final MetronomeStartStopListener mListener;
//
//    public MetronomeBroadcastReceiver(MetronomeStartStopListener ml) {
//        Timber.d("BroadcastReceiver created");
//        mListener = ml;
//    }
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        Timber.d("onReceive()  woot woot");
//        if(intent.getAction() != null && intent.getAction().equals(Metronome.ACTION_METRONOME_START_STOP)) {
//            Timber.d(Metronome.ACTION_METRONOME_START_STOP);
//            mListener.metronomeStartStop();
//        } else if (intent.getAction().equals("Action Reply")) {
//            Timber.d("Action Reply received");
//        }
//    }
//}
