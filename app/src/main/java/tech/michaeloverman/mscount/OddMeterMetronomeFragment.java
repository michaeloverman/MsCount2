/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import tech.michaeloverman.mscount.utils.Metronome;
import tech.michaeloverman.mscount.utils.MetronomeBroadcastReceiver;
import tech.michaeloverman.mscount.utils.MetronomeStartStopListener;
import timber.log.Timber;

/**
 * Odd-Meter metronome fragment. Handles UI, input, etc
 * Created by Michael on 3/14/2017.
 */

public class OddMeterMetronomeFragment extends Fragment implements MetronomeStartStopListener {

    private static final float MAX_TEMPO_BPM = Metronome.MAX_TEMPO;
    private static final float MIN_TEMPO_BPM = Metronome.MIN_TEMPO;
    private static final float SUBDIVISION_DISPLAY_SIZE = 40;
    private static final int MARGIN = 8;
    private static final String PREF_KEY_BPM = "pref_key_oddmeter_bpm";
    private static final String PREF_LIST_LENGTH = "pref_key_oddmeter_subdivision_list";
    private static final String PREF_KEY_LIST = "oddmeter_subdivision_";
    private static final String PREF_KEY_MULTIPLIER = "oddmeter_multiplier";
    private static final String PREF_KEY_INCLUDE_SUBDIVISIONS = "include_subdivisions_checkbox";

    private Metronome mMetronome;
    private boolean mMetronomeRunning;
    private float mBPM;
    private int mMultiplier;

//    private WearNotification mWearNotification;
    private BroadcastReceiver mBroadcastReceiver;
//    private boolean mHasWearDevice;

    @BindView(R.id.oddmeter_start_stop_fab) FloatingActionButton mStartStopFab;
    @BindView(R.id.oddmeter_tempo_view) TextView mTempoSetting;
    @BindView(R.id.include_subdivisions_checkBox) CheckBox mSubdivisionsCheckbox;
    @BindView(R.id.extra_subdivision_buttons) LinearLayout mOtherButtons;
    @BindView(R.id.pulse_multiplier_view) TextView mPulseMultiplierView;
//    @BindView(R.id.odd_adView) AdView mAdView;
    private boolean mMultiplierSelected;

    private List<Integer> mSubdivisionsList;
    private List<View> mSubdivisionViews;
    @BindView(R.id.subdivision_layout) FlexboxLayout mSubdivisionLayout;
//    private LinearLayout mSubdivisionLayout;

    @BindView(R.id.help_overlay) FrameLayout mInstructionsLayout;

    private GestureDetectorCompat mDetector;

    public static Fragment newInstance() {
        return new OddMeterMetronomeFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        mMetronome = new Metronome(getActivity());
        mMetronome.setMetronomeStartStopListener(this);

//        mHasWearDevice = PrefUtils.wearPresent(getContext());
//        if(mHasWearDevice) {
//            createAndRegisterBroadcastReceiver();
//        }

        mDetector = new GestureDetectorCompat(getContext(), new MetronomeGestureListener());

        mSubdivisionsList = new ArrayList<>();
        mSubdivisionViews = new ArrayList<>();

    }

//    private void updateWearNotif() {
//        if (mHasWearDevice) {
//            mWearNotification = new WearNotification(getContext(),
//                    getString(R.string.unformatted_bpm, (int) mBPM), getSubdivisionsListAsString());
//            mWearNotification.sendStartStop();
//        }
//    }

//    private String getSubdivisionsListAsString() {
//        StringBuilder sb = new StringBuilder();
//        for(Integer i : mSubdivisionsList) {
//            sb.append(i).append("   ");
//        }
//        return sb.toString();
//    }

    private void createAndRegisterBroadcastReceiver() {
        if(mBroadcastReceiver == null) {
            mBroadcastReceiver = new MetronomeBroadcastReceiver(this);
        }
        IntentFilter filter = new IntentFilter(Metronome.ACTION_METRONOME_START_STOP);
        getActivity().registerReceiver(mBroadcastReceiver, filter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.oddmeter_metronome_layout, container, false);
        ButterKnife.bind(this, view);

//        AdRequest.Builder adRequest = new AdRequest.Builder();
//        if (BuildConfig.DEBUG) {
//            adRequest.addTestDevice(getString(R.string.test_device_code));
//        }
//        mAdView.loadAd(adRequest.build());

        // use the "naked" listener to catch ACTION_UP (release) to reset tempo
        // defer to GestureDetector to handle scrolling/changing tempo
        mTempoSetting.setOnTouchListener( (View v, MotionEvent event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_UP) {
                if (mMetronomeRunning) {
                    // stop the met
                    metronomeStartStop();
                    // restart at new tempo
                    metronomeStartStop();
                }
            } else {
                mDetector.onTouchEvent(event);
            }
            return true;
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        mBPM = prefs.getFloat(PREF_KEY_BPM, 120f);
        mMultiplier = prefs.getInt(PREF_KEY_MULTIPLIER, 2);
        boolean subdivCheck = prefs.getBoolean(PREF_KEY_INCLUDE_SUBDIVISIONS, false);
        mSubdivisionsCheckbox.setChecked(subdivCheck);
        int listlength = prefs.getInt(PREF_LIST_LENGTH, 0);
        for (int i = 0; i < listlength; i++) {
            int subdiv = prefs.getInt(PREF_KEY_LIST + i, 2);
            mSubdivisionsList.add(subdiv);
            mSubdivisionViews.add(getNewSubdivisionView(subdiv));
        }
    
        mInstructionsLayout.setSoundEffectsEnabled(false);
    
        updateTempoDisplay();
        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Timber.d("menu item caught: %s", item.getTitle());
        if (item.getItemId() == R.id.help_menu_item) {
            makeInstructionsVisible();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void makeInstructionsVisible() {
        mInstructionsLayout.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.help_cancel_button)
    public void makeInstructionsInvisible() {
        mInstructionsLayout.setVisibility(View.INVISIBLE);
    }

    @OnClick( {R.id.one_subs_button, R.id.two_subs_button, R.id.three_subs_button,
            R.id.four_subs_button, R.id.five_subs_button, R.id.six_subs_button,
            R.id.seven_subs_button, R.id.eight_subs_button, R.id.nine_subs_button,
            R.id.ten_subs_button} )
    public void addSubdivision(TextView button) {
        boolean wasRunning = false;
        if (mMetronomeRunning) {
            metronomeStartStop();
            wasRunning = true;
        }
        int beat = Integer.parseInt(button.getText().toString());
        if (mMultiplierSelected) {
            mMultiplier = beat;
            mPulseMultiplierView.setText(getString(R.string.pulse_equals, beat));
            multiplierSelected();
            return;
        }
        mSubdivisionsList.add(beat);
        mSubdivisionViews.add(getNewSubdivisionView(beat));

        if (mOtherButtons.isShown()) mOtherButtons.setVisibility(View.GONE);

        if (wasRunning) metronomeStartStop();
    }

    @OnClick(R.id.other_subs_button)
    public void addUnusualSubdivision() {
        Timber.d("add a different length of subdivision");
        if (mOtherButtons.isShown()) {
            mOtherButtons.setVisibility(View.GONE);
        } else {
            mOtherButtons.setVisibility(View.VISIBLE);
        }
    }

    @OnClick(R.id.delete_button)
    public void deleteSubdivision() {
        Timber.d("remove a subdivision");
        if (mSubdivisionsList.size() == 0) return;
        boolean wasRunning = false;
        if (mMetronomeRunning) {
            wasRunning = true;
            metronomeStartStop();
        }
        mSubdivisionsList.remove(mSubdivisionsList.size() - 1);
        mSubdivisionLayout.removeView(mSubdivisionViews.get(mSubdivisionViews.size() - 1));
        mSubdivisionViews.remove(mSubdivisionViews.size() - 1);

        if (wasRunning && mSubdivisionsList.size() > 0) metronomeStartStop();
    }

    @OnClick(R.id.pulse_multiplier_view)
    public void multiplierSelected() {
        if (mMultiplierSelected) {
            mMultiplierSelected = false;
            mPulseMultiplierView.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.roundcorner_light));
            if (mOtherButtons.isShown()) mOtherButtons.setVisibility(View.GONE);
        } else {
            mPulseMultiplierView.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.roundcorner_accent));
            mMultiplierSelected = true;
        }
    }
    
    @SuppressWarnings("EmptyMethod")
    @OnClick(R.id.help_overlay)
    public void ignoreClicks() {
        // catch and ignore click on the help screen, so other buttons aren't functional0
    }

    @Override
    public void onPause() {

        if(mMetronomeRunning) {
            metronomeStartStop();
        }
//        if (mWearNotification != null) {
//            mWearNotification.cancel();
//        }

        getActivity().unregisterReceiver(mBroadcastReceiver);

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        createAndRegisterBroadcastReceiver();
//        updateWearNotif();
    }

    @Override
    public void onDestroy() {
        SharedPreferences.Editor prefs = PreferenceManager
                .getDefaultSharedPreferences(getContext()).edit();
        prefs.putFloat(PREF_KEY_BPM, mBPM);
        prefs.putInt(PREF_KEY_MULTIPLIER, mMultiplier);
        prefs.putInt(PREF_LIST_LENGTH, mSubdivisionsList.size());
        prefs.putBoolean(PREF_KEY_INCLUDE_SUBDIVISIONS, mSubdivisionsCheckbox.isChecked());
        for (int i = 0; i < mSubdivisionsList.size(); i++) {
            prefs.remove(PREF_KEY_LIST + i);
            prefs.putInt(PREF_KEY_LIST + i, mSubdivisionsList.get(i));
        }
        prefs.apply();

        super.onDestroy();
    }

    @OnClick(R.id.include_subdivisions_checkBox)
    public void subdivisionsOnOff() {
        if (mMetronome.isRunning()) {
            metronomeStartStop();
            metronomeStartStop();
        }
    }

    @Override
    @OnClick(R.id.oddmeter_start_stop_fab)
    public void metronomeStartStop() {
        Timber.d("Loop length: " + mSubdivisionsList.size() + ", view size: "
                + mSubdivisionViews.size());
        if (mMetronomeRunning) {
            mMetronome.stop();
            mMetronomeRunning = false;
            mStartStopFab.setImageResource(android.R.drawable.ic_media_play);
        } else {
            if (mSubdivisionsList.size() == 0) {
                Toast.makeText(getContext(), R.string.need_subdivs_to_click_subdivs,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            mMetronomeRunning = true;
            mStartStopFab.setImageResource(android.R.drawable.ic_media_pause);
            mMetronome.play(((int) mBPM * mMultiplier), mSubdivisionsList,
                    mSubdivisionsCheckbox.isChecked());
        }
//        if (mHasWearDevice) mWearNotification.sendStartStop();
    }

    private void changeTempo(float tempoChange) {
        mBPM += tempoChange;
        if (mBPM > MAX_TEMPO_BPM) mBPM = MAX_TEMPO_BPM;
        else if (mBPM < MIN_TEMPO_BPM) mBPM = MIN_TEMPO_BPM;
        updateTempoDisplay();
    }

    private void updateTempoDisplay() {
        mTempoSetting.setText(String.valueOf((int) mBPM));
        mPulseMultiplierView.setText(getString(R.string.pulse_equals, mMultiplier));
//        updateWearNotif();
    }

    private View getNewSubdivisionView(int value) {
        TextView view = new TextView(getContext());
        view.setText(String.valueOf(value));
        view.setTextSize(SUBDIVISION_DISPLAY_SIZE);
        view.setBackground(ContextCompat.getDrawable(getActivity(),
                R.drawable.roundcorner_parchment));
        view.setPadding(MARGIN, 0, MARGIN, 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);

        view.setLayoutParams(params);
        mSubdivisionLayout.addView(view);
        return view;
    }

    class MetronomeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final float MINIMUM_Y_FOR_FAST_CHANGE = 10;

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (Math.abs(distanceY) > MINIMUM_Y_FOR_FAST_CHANGE) {
                changeTempo(distanceY / 10);
            } else {
                changeTempo(-distanceX / 100);
            }
            return true;
        }
    }
}
