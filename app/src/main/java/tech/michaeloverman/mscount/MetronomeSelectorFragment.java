/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import tech.michaeloverman.mscount.programmed.ProgrammedMetronomeActivity;
import timber.log.Timber;

/**
 * Fragment handles selection of & transition to the different metronomes.
 * Created by Michael on 2/24/2017.
 */

public class MetronomeSelectorFragment extends Fragment {
	
	private static final String TAG = MetronomeSelectorFragment.class.getSimpleName();
	
	@BindView(R.id.big_round_button) ImageView mBigRoundButton;
//	@BindView(R.id.normal_metronome_button) Button mNormalMetButton;
//	@BindView(R.id.preprogrammed_metronome_button) Button mPreprogrammedMetButton;
	private Unbinder mUnbinder;
	
	public static Fragment newInstance() {
		return new MetronomeSelectorFragment();
	}
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.met_selector_fragment, container, false);
		mUnbinder = ButterKnife.bind(this, view);
		
		return view;
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mUnbinder.unbind();
	}
	
	@OnClick( { R.id.normal_metronome_button, R.id.preprogrammed_metronome_button,
			R.id.odd_meter_metronome_button})
	public void buttonClicked(View button) {
		Timber.d(TAG, "buttonClicked()");
		Intent intent;
		switch(button.getId()) {
			case R.id.normal_metronome_button:
				intent = new Intent(getActivity(), NormalMetronomeActivity.class);
				break;
			case R.id.odd_meter_metronome_button:
				intent = new Intent(getActivity(), OddMeterMetronomeActivity.class);
				break;
			case R.id.preprogrammed_metronome_button:
				intent = new Intent(getActivity(), ProgrammedMetronomeActivity.class);
				break;
			default:
				intent = null;
		}
		
		View sharedView = mBigRoundButton;
		String transitionName = getString(R.string.round_button_transition);
		ActivityOptionsCompat transitionOptions = ActivityOptionsCompat
				.makeSceneTransitionAnimation(getActivity(), sharedView, transitionName);
		
		if (intent != null) {
			startActivity(intent, transitionOptions.toBundle());
		}
	}
	
}
