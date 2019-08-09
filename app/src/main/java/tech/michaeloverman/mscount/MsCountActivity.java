/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.transition.Slide;
import android.transition.TransitionInflater;

import androidx.fragment.app.Fragment;

import tech.michaeloverman.mscount.utils.ClickSounds;
import timber.log.Timber;

/**
 * Activity that handles the 'global' app issues (API Client for wear) and the MetSelect Fragment
 */

public class MsCountActivity extends SingleFragmentActivity {

//    private GoogleApiClient client;
//    private static final long CONNECTION_TIME_OUT_MS = 3000;

    @Override
    protected Fragment createFragment() {
        Timber.d("MsCountActivity createFragment()");
        return MetronomeSelectorFragment.newInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("MsCountActivity onCreate()");

//        MobileAds.initialize(getApplicationContext(), "ca-app-pub-9915736656105375~9633528243");

//        checkIfWearableConnected();

        ClickSounds.loadSounds(this);

        if(Build.VERSION.SDK_INT >= 21) {
            setupWindowAnimations();
        }
    }


//    private void checkIfWearableConnected() {
//        Timber.d("checking if wearable present");
//        retrieveDeviceNode(new Callback() {
//            @Override
//            public void success(String nodeId) {
//                Timber.d("Wear node detected");
//                PrefUtils.saveWearStatus(MsCountActivity.this, true);
//            }
//
//            @Override
//            public void failed() {
//                Timber.d("No Wear node detected");
//                PrefUtils.saveWearStatus(MsCountActivity.this, false);
//            }
//        });
//
//    }

//    private GoogleApiClient getGoogleApiClient(Context context) {
//        Timber.d("getting googleapiclient for checking wearable");
//        if (client == null)
//            client = new GoogleApiClient.Builder(context)
//                    .addApi(Wearable.API)
//                    .build();
//        return client;
//    }

//    private interface Callback {
//        void success(final String nodeId);
//        void failed();
//    }

//    private void retrieveDeviceNode(final Callback callback) {
//        Timber.d("retrieving device nodes");
//        final GoogleApiClient client = getGoogleApiClient(this);
//        new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                Timber.d("running device check thread");
//                client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
//                NodeApi.GetConnectedNodesResult result =
//                        Wearable.NodeApi.getConnectedNodes(client).await();
//                Timber.d("result: " + result.toString());
//                List<Node> nodes = result.getNodes();
//                if (nodes.size() > 0) {
//                    String nodeId = nodes.get(0).getId();
//                    callback.success(nodeId);
//                } else {
//                    callback.failed();
//                }
//                Timber.d("disconnecting client");
//                client.disconnect();
//            }
//        }).start();
//    }

    @TargetApi(21)
    private void setupWindowAnimations() {
        Slide slide = (Slide) TransitionInflater.from(this).inflateTransition(R.transition.activity_slide_out);
        getWindow().setExitTransition(slide);
        getWindow().setAllowEnterTransitionOverlap(true);

//        getWindow().setReenterTransition(slide);
//        getWindow().setAllowReturnTransitionOverlap(true);
    }

}
