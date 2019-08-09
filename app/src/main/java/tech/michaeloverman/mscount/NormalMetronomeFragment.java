/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import tech.michaeloverman.mscount.utils.Metronome;
import tech.michaeloverman.mscount.utils.MetronomeBroadcastReceiver;
import tech.michaeloverman.mscount.utils.MetronomeStartStopListener;
import timber.log.Timber;

import static android.view.MotionEvent.ACTION_UP;

/**
 * Normal metronome. Handle UI, input, etc. Requests clicks from a Metronome.
 *
 * Created by Michael on 2/24/2017.
 */

public class NormalMetronomeFragment extends Fragment implements MetronomeStartStopListener {
	
	private static final int MAX_SUBDIVISIONS = Metronome.MAX_SUBDIVISIONS;
	private static final int MAX_TEMPO_BPM_INT = Metronome.MAX_TEMPO;
	private static final float MAX_TEMPO_BPM_FLOAT = (float) MAX_TEMPO_BPM_INT;
	private static final int MIN_TEMPO_BPM_INT = Metronome.MIN_TEMPO;
	private static final float MIN_TEMPO_BPM_FLOAT = (float) MIN_TEMPO_BPM_INT;
	
	private static final float MAX_FLOAT_VOLUME = 300.0f;
	private static final float MIN_FLOAT_VOLUME = 0.0f;
	private static final int FLOAT_VOLUME_DIVIDER = 30;
	private static final String PREF_KEY_SUBDIVISIONS = "pref_subdivisions";
	private static final String PREF_KEY_BPM = "pref_bpm";
	private static final String PREF_WHOLE_NUMBERS = "pref_whole_numbers";
	
	private Metronome mMetronome;
	private boolean mMetronomeRunning;
	
//	private WearNotification mWearNotification;
	private BroadcastReceiver mBroadcastReceiver;
//	private boolean mHasWearDevice;
	
	@BindView(R.id.normal_start_stop_fab) FloatingActionButton mStartStopFab;
	@BindView(R.id.current_tempo) TextView mTempoSetting;
	@BindView(R.id.tempo_down_button) ImageButton mTempoDownButton;
	@BindView(R.id.tempo_up_button) ImageButton mTempoUpButton;
	
	@BindView(R.id.add_subdivisions_fab) FloatingActionButton mAddSubdivisionFAB;
	@BindView(R.id.expanded_add_subdivisions_fab) FloatingActionButton mExpandedAddSubFab;
	@BindView(R.id.expanded_subtract_subdivisions_fab) FloatingActionButton mSubtractSubFab;
	@BindView(R.id.subdivision_indicator1) FloatingActionButton sub1;
	@BindView(R.id.subdivision_indicator2) FloatingActionButton sub2;
	@BindView(R.id.subdivision_indicator3) FloatingActionButton sub3;
	@BindView(R.id.subdivision_indicator4) FloatingActionButton sub4;
	@BindView(R.id.subdivision_indicator5) FloatingActionButton sub5;
	@BindView(R.id.subdivision_indicator6) FloatingActionButton sub6;
	@BindView(R.id.subdivision_indicator7) FloatingActionButton sub7;
	@BindView(R.id.subdivision_indicator8) FloatingActionButton sub8;
	@BindView(R.id.subdivision_indicator9) FloatingActionButton sub9;
	@BindView(R.id.subdivision_indicator10) FloatingActionButton sub10;
//    @BindView(R.id.normal_adView) AdView mAdView;
	
	@BindView(R.id.help_overlay) FrameLayout mInstructionsLayout;
	
	private FloatingActionButton[] mSubdivisionIndicators;
	private int[] mSubdivisionFabColors;
	
	private Animation expandingAddFabAnim;
	private Animation expandingSubFabAnim;
	private Animation collapsingAddFabAnim;
	private Animation collapsingSubFabAnim;
//    Animation fadingFabAnim, unFadingFabAnim;
	
	private float mBPM;
	private boolean mWholeNumbersSelected = true;
	private int mNumSubdivisions;
	private float[] mSubdivisionFloatVolumes;
	private int[] mSubdivisionVolumes;
	
	private GestureDetectorCompat mDetector;
	
	public static Fragment newInstance() {
		return new NormalMetronomeFragment();
	}
	
	private FirebaseAnalytics mFirebaseAnalytics;
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setHasOptionsMenu(true);
		
		mMetronome = new Metronome(getActivity());
		mMetronome.setMetronomeStartStopListener(this);
		
//		mHasWearDevice = PrefUtils.wearPresent(getContext());
//		if (mHasWearDevice) {
//			createAndRegisterBroadcastReceiver();
//		}
		
		mDetector = new GestureDetectorCompat(this.getContext(), new MetronomeGestureListener());
		
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
		mNumSubdivisions = pref.getInt(PREF_KEY_SUBDIVISIONS, 1);
		mBPM = pref.getFloat(PREF_KEY_BPM, 123.4f);
		mWholeNumbersSelected = pref.getBoolean(PREF_WHOLE_NUMBERS, true);
		
		mSubdivisionVolumes = new int[MAX_SUBDIVISIONS];
		mSubdivisionFloatVolumes = new float[MAX_SUBDIVISIONS];
		for (int i = 0; i < MAX_SUBDIVISIONS; i++) {
			mSubdivisionVolumes[i] = 10;
			mSubdivisionFloatVolumes[i] = MAX_FLOAT_VOLUME;
		}
		mSubdivisionFabColors = getContext().getResources().getIntArray(R.array.subdivision_colors);
		
		mFirebaseAnalytics = FirebaseAnalytics.getInstance(getContext());
	}
	
	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.normal_metronome_fragment, container, false);
		ButterKnife.bind(this, view);
		
		//        AdRequest adRequest = new AdRequest.Builder().build();
//        AdRequest.Builder adRequest = new AdRequest.Builder();
//        if (BuildConfig.DEBUG) {
//            adRequest.addTestDevice(getString(R.string.test_device_code));
//        }
//        mAdView.loadAd(adRequest.build());
		
		// use the "naked" listener to catch ACTION_UP (release) for resetting tempo
		// otherwise defer to GestureDetector to handle scrolling
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
		
		expandingAddFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.expanding_add_fab);
		expandingSubFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.expanding_sub_fab);
		collapsingAddFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.collapsing_add_fab);
		collapsingSubFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.collapsing_sub_fab);
		collapsingAddFabAnim.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				mAddSubdivisionFAB.show();
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
			
			}
		});
		
		mSubdivisionIndicators = new FloatingActionButton[]
				{sub1, sub2, sub3, sub4, sub5, sub6, sub7, sub8, sub9, sub10};
		addSubdivisionVolumeChangeListeners();
		
		if (mNumSubdivisions > 1) {
			expandFabs();
			for (int i = 1; i < mNumSubdivisions; i++) {
				mSubdivisionIndicators[i].show();
			}
		}
		
		if (!mWholeNumbersSelected) {
			RadioButton b = view.findViewById(R.id.decimals);
			b.setChecked(true);
		}
		
		mInstructionsLayout.setSoundEffectsEnabled(false);
		
		updateDisplay();
		return view;
	}
	
	@Override
	public void onPause() {
//        if (mAdView != null) {
//            mAdView.pause();
//        }
		
		if(mMetronomeRunning) {
			metronomeStartStop();
		}
		
//		if (mWearNotification != null) {
//			mWearNotification.cancel();
//		}
		
		getActivity().unregisterReceiver(mBroadcastReceiver);
		
		super.onPause();
	}
	
	@Override
	public void onResume() {
		super.onResume();
//        if (mAdView != null) {
//            mAdView.resume();
//        }
		
		createAndRegisterBroadcastReceiver();
//		updateWearNotif();
	}
	
	@Override
	public void onDestroy() {
		SharedPreferences.Editor prefs = PreferenceManager
				.getDefaultSharedPreferences(getContext()).edit();
		prefs.putFloat(PREF_KEY_BPM, mBPM);
		prefs.putInt(PREF_KEY_SUBDIVISIONS, mNumSubdivisions);
		prefs.putBoolean(PREF_WHOLE_NUMBERS, mWholeNumbersSelected);
		prefs.apply();

//        if (mAdView != null) {
//            mAdView.destroy();
//        }
		
		super.onDestroy();
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
	public void instructionsCancelled() {
		mInstructionsLayout.setVisibility(View.INVISIBLE);
	}
	
//	private void updateWearNotif() {
//		if (mHasWearDevice) {
//			mWearNotification = new WearNotification(getContext(),
//					getString(R.string.app_name), getString(R.string.unformatted_bpm, (int) mBPM));
//			mWearNotification.sendStartStop();
//		}
//	}
	
	private void createAndRegisterBroadcastReceiver() {
		if(mBroadcastReceiver == null) {
			mBroadcastReceiver = new MetronomeBroadcastReceiver(this);
		}
		IntentFilter filter = new IntentFilter(Metronome.ACTION_METRONOME_START_STOP);
		getActivity().registerReceiver(mBroadcastReceiver, filter);
	}
	
	@OnClick({R.id.add_subdivisions_fab, R.id.expanded_add_subdivisions_fab})
	public void addASubdivision() {
		boolean restart = false;
		if (mMetronomeRunning) {
			metronomeStartStop();
			restart = true;
		}
		
		if (mNumSubdivisions == 1) {
			expandFabs();
		} else if (mNumSubdivisions == MAX_SUBDIVISIONS) {
			Toast.makeText(getActivity(), R.string.too_many_subdivisions,
					Toast.LENGTH_SHORT).show();
			return;
		}
		mSubdivisionIndicators[mNumSubdivisions].show();
		
		mNumSubdivisions++;
		
		if (restart) metronomeStartStop();
	}
	
	@OnClick(R.id.expanded_subtract_subdivisions_fab)
	public void subtractASubdivision() {
		Bundle bundle = new Bundle();
		bundle.putString("time", Calendar.getInstance().toString());
		bundle.putString("presubtract_subdivision_count", ":"+mNumSubdivisions);
		mFirebaseAnalytics.logEvent("subtractASubdivision", bundle);
		if(mNumSubdivisions == 1) return;
		boolean restart = false;
		if (mMetronomeRunning) {
			metronomeStartStop();
			restart = true;
		}
		mNumSubdivisions--;
		mSubdivisionIndicators[mNumSubdivisions].hide();
		if (mNumSubdivisions == 1) {
			collapseFabs();
		}
		
		if (restart) metronomeStartStop();
	}
	
	private void expandFabs() {
		mSubtractSubFab.show();
		mExpandedAddSubFab.show();
		mAddSubdivisionFAB.hide();
		mExpandedAddSubFab.startAnimation(expandingAddFabAnim);
		mSubtractSubFab.startAnimation(expandingSubFabAnim);
	}
	
	private void collapseFabs() {
		mExpandedAddSubFab.startAnimation(collapsingAddFabAnim);
		mSubtractSubFab.startAnimation(collapsingSubFabAnim);
		mSubtractSubFab.hide();
		mExpandedAddSubFab.hide();
	}
	
	@Override
	@OnClick(R.id.normal_start_stop_fab)
	public void metronomeStartStop() {
		if (mMetronomeRunning) {
			mMetronome.stop();
			mMetronomeRunning = false;
			mStartStopFab.setImageResource(android.R.drawable.ic_media_play);
		} else {
			mMetronomeRunning = true;
			if (mWholeNumbersSelected) {
				mMetronome.play((int) mBPM, mNumSubdivisions);
			} else {
				mMetronome.play(mBPM, mNumSubdivisions);
			}
			mStartStopFab.setImageResource(android.R.drawable.ic_media_pause);
		}
//		if (mHasWearDevice) mWearNotification.sendStartStop();
	}
	
	@OnClick({R.id.whole_numbers, R.id.decimals})
	public void onRadioButtonClicked(View view) {
		boolean checked = ((RadioButton) view).isChecked();
		
		switch (view.getId()) {
			case R.id.whole_numbers:
				if (checked) {
					mWholeNumbersSelected = true;
					mTempoDownButton.setVisibility(View.GONE);
					mTempoUpButton.setVisibility(View.GONE);
				}
				break;
			case R.id.decimals:
				if (checked) {
					mWholeNumbersSelected = false;
					mTempoDownButton.setVisibility(View.VISIBLE);
					mTempoUpButton.setVisibility(View.VISIBLE);
				}
				break;
		}
		updateDisplay();
	}
	
	@OnClick(R.id.tempo_down_button)
	public void onDownButtonClick() {
		changeTempo(-0.1f);
	}
	
	@OnClick(R.id.tempo_up_button)
	public void onUpButtonClick() {
		changeTempo(0.1f);
	}
	
	@SuppressWarnings("EmptyMethod")
	@OnClick(R.id.help_overlay)
	public void ignoreClicks() {
		// catch and ignore click on the help screen, so other buttons aren't functional
	}
	
	private void changeTempo(float tempoChange) {
		mBPM += tempoChange;
		if (mBPM > MAX_TEMPO_BPM_INT) mBPM = MAX_TEMPO_BPM_FLOAT;
		else if (mBPM < MIN_TEMPO_BPM_INT) mBPM = MIN_TEMPO_BPM_FLOAT;
		updateDisplay();
	}
	
	private void updateDisplay() {
		if (mWholeNumbersSelected) {
			mTempoSetting.setText(String.valueOf((int) mBPM));
		} else {
			mTempoSetting.setText(String.valueOf((float) ((int) (mBPM * 10)) / 10));
		}
//		updateWearNotif();
	}
	
	private void addSubdivisionVolumeChangeListeners() {
//        mSubdivisionDetector = new GestureDetectorCompat[MAX_SUBDIVISIONS];
		
		for (int i = 0; i < MAX_SUBDIVISIONS; i++) {
			final int subdivisionID = i;
//            mSubdivisionDetector[subdivisionID] = new GestureDetectorCompat(this.getContext(),
//                    new SubdivisionGestureListener(subdivisionID));
			mSubdivisionIndicators[subdivisionID].setOnTouchListener(new View.OnTouchListener() {
				float firstY;
				
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					int action = event.getAction();
					switch (action) {
						case (MotionEvent.ACTION_DOWN): {
							displayVolumeSub(subdivisionID);
							firstY = event.getY();
							break;
						}
						case (MotionEvent.ACTION_MOVE):
							final float y = event.getY();
							float distanceY = y - firstY;
							firstY = y;
							changeSubdivisionVolume(subdivisionID, -distanceY);
//                            mSubdivisionDetector[subdivisionID].onTouchEvent(event);
							break;
						case (ACTION_UP):
							v.performClick();
							updateDisplay();
							break;
						default:
							return false;
					}
					return true;
				}
				
			});
		}
	}
	
	private void displayVolumeSub(int subdiv) {
		mTempoSetting.setText(getString(R.string.vol_abbrev_colon, mSubdivisionVolumes[subdiv]));
	}
	
	private void changeSubdivisionVolume(int id, float volumeChange) {
		mSubdivisionFloatVolumes[id] += volumeChange;
		if (mSubdivisionFloatVolumes[id] > MAX_FLOAT_VOLUME)
			mSubdivisionFloatVolumes[id] = MAX_FLOAT_VOLUME;
		else if (mSubdivisionFloatVolumes[id] < MIN_FLOAT_VOLUME)
			mSubdivisionFloatVolumes[id] = MIN_FLOAT_VOLUME;
		Timber.d("float volume measured: %f", mSubdivisionFloatVolumes[id]);
		
		mSubdivisionVolumes[id] = (int) (mSubdivisionFloatVolumes[id] / FLOAT_VOLUME_DIVIDER);
		
		mTempoSetting.setText(getString(R.string.vol_abbrev_colon, mSubdivisionVolumes[id]));
		setFabAppearance(mSubdivisionIndicators[id], mSubdivisionVolumes[id]);
		mMetronome.setClickVolumes(mSubdivisionVolumes);
	}
	
	private void setFabAppearance(FloatingActionButton fab, int level) {
		fab.setBackgroundTintList(ColorStateList.valueOf(mSubdivisionFabColors[level]));
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
