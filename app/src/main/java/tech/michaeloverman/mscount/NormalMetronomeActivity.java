/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.TransitionInflater;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import tech.michaeloverman.mscount.utils.MetronomeActivity;


/**
 * Activity to handle the Normal Met Fragment
 *
 * Created by Michael on 3/24/2017.
 */

public class NormalMetronomeActivity extends MetronomeActivity {

    @Override
    protected Fragment createFragment() {
//        mMetronome = new Metronome(this);
        return NormalMetronomeFragment.newInstance();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= 21) {
            setupWindowAnimations();
        }
    }
//
//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//        if(mMetronome != null && mMetronome.isRunning()) {
//            mMetronome.stop();
//        }
//    }

    @TargetApi(21)
    private void setupWindowAnimations() {
        Fade slide = (Fade) TransitionInflater.from(this).inflateTransition(R.transition.activity_fade_enter);
        getWindow().setEnterTransition(slide);
        getWindow().setAllowEnterTransitionOverlap(true);
    }
}
