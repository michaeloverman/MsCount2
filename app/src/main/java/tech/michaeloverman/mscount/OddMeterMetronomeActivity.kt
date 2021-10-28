/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount

import tech.michaeloverman.mscount.utils.MetronomeActivity
import tech.michaeloverman.mscount.utils.Metronome
import tech.michaeloverman.mscount.OddMeterMetronomeFragment
import android.os.Bundle
import android.os.Build
import android.annotation.TargetApi
import android.transition.Fade
import android.transition.TransitionInflater
import androidx.fragment.app.Fragment
import tech.michaeloverman.mscount.R

/**
 * Activity to handle the Odd-Met Fragment
 * Created by Michael on 3/24/2017.
 */
class OddMeterMetronomeActivity : MetronomeActivity() {
    override fun createFragment(): Fragment {
        mMetronome = Metronome(this)
        return OddMeterMetronomeFragment.newInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowAnimations()
    }

    //
    //    @Override
    //    public void onBackPressed() {
    //        super.onBackPressed();
    //        if(mMetronome.isRunning()) {
    //            // stop metronome
    //            mMetronome.stop();
    //        }
    //    }
    @TargetApi(21)
    private fun setupWindowAnimations() {
        val slide = TransitionInflater.from(this).inflateTransition(R.transition.activity_fade_enter) as Fade
        window.enterTransition = slide
        window.allowEnterTransitionOverlap = true
    }
}