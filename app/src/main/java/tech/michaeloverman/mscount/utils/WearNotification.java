///* Copyright (C) 2017 Michael Overman - All Rights Reserved */
//package tech.michaeloverman.mscount.utils;
//
//import android.app.Notification;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.BitmapFactory;
//
//import androidx.core.app.NotificationCompat;
//import androidx.core.app.NotificationManagerCompat;
//import androidx.core.content.ContextCompat;
//
//import tech.michaeloverman.mscount.R;
//import timber.log.Timber;
//
///**
// * Handles everything regarding the start and stop notifications on any available wear device.
// *
// * Created by Michael on 4/29/2017.
// */
//
//public class WearNotification {
//
//    // --Commented out by Inspection (5/13/2017 11:36 AM):private final String EXTRA_WEAR_INTENT_ID = "tech.michaeloverman.android.mscount.wearable.extra_message";
//
//    private final Context mContext;
//    private boolean mPlaying;
//    private final String mTitle;
//    private final String mMessage;
//
//    public WearNotification(Context context, String title, String message) {
//        mContext = context;
//        mTitle = title;
//        mMessage = message;
//        mPlaying = false;
//    }
//
//    public void sendStartStop() {
//        Timber.d("sendStartStop()");
//        int notifId = 3435;
//        Intent wearIntent = new Intent(Metronome.ACTION_METRONOME_START_STOP);
//
//        PendingIntent wearPendingIntent = PendingIntent.getBroadcast(
//                mContext, notifId, wearIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//        Timber.d("Pending Intent created");
//
//        NotificationCompat.Action action = new NotificationCompat.Action.Builder(
//                getIcon(),
//                "Start/Stop",
//                wearPendingIntent)
//                .build();
//        Timber.d("action created");
//
//        NotificationCompat.WearableExtender wearExtend = new NotificationCompat.WearableExtender()
//                .setContentAction(0)
////                .setHintHideIcon(true)
////                .setCustomSizePreset(NotificationCompat.WearableExtender.SIZE_FULL_SCREEN)
////                .setHintScreenTimeout(NotificationCompat.WearableExtender.SCREEN_TIMEOUT_LONG)
//                .setBackground(BitmapFactory.decodeResource(
//                        mContext.getResources(), R.mipmap.ic_launcher))
//                .setContentIntentAvailableOffline(false)
////                .setHintAmbientBigPicture(true)
//                .addAction(action);
//
//        int color = ContextCompat.getColor(mContext, R.color.colorPrimary);
////        NotificationCompat.Style style = new android.support.v7.app.NotificationCompat.MediaStyle();
//        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(mContext) // TODO add channelId string after mContext
//                .setSmallIcon(getIcon())
//                .setContentTitle(mTitle)
//                .setContentText(mMessage)
//                .setColor(color)
//                .setVibrate(new long[] { 50, 50, 500 })
//                .setOnlyAlertOnce(true)
////                .setStyle(style)
//                .addAction(action)
//                .setContentIntent(wearPendingIntent)
//                .extend(wearExtend);
////                .addAction(android.R.drawable.ic_media_play, playPauseIntent);
//        Timber.d("notifBuilder created");
//
//        Notification notif = notifBuilder.build();
//
//        NotificationManagerCompat manager = NotificationManagerCompat.from(mContext);
//        manager.notify(notifId, notif);
//        mPlaying = !mPlaying;
//        Timber.d("manager has notified...");
//    }
//
//    private int getIcon() {
//        if(mPlaying) {
//            return android.R.drawable.ic_media_pause;
//        } else {
//            return android.R.drawable.ic_media_play;
//        }
//    }
//
//    public void cancel() {
//        NotificationManagerCompat.from(mContext).cancelAll();
//    }
//}
