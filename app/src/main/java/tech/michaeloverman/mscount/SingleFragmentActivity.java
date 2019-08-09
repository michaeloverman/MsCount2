/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import timber.log.Timber;

/**
 * Created by Michael on 4/21/2016.
 * Adapted from SingleFragmentActivity from the Big Nerd Ranch Guide
 */
public abstract class SingleFragmentActivity extends AppCompatActivity {
	
	protected abstract Fragment createFragment();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (BuildConfig.DEBUG) {
			Timber.uprootAll();
			Timber.plant(new Timber.DebugTree());
		}
		Timber.d("SingleFragmentActivity onCreate()");
		Timber.d("SingleFragment test Timber log statement");
		
		setContentView(R.layout.activity_fragment);
		
		FragmentManager fm = getSupportFragmentManager();
		Fragment fragment = fm.findFragmentById(R.id.fragment_container);
		
		if (fragment == null) {
			fragment = createFragment();
			fm.beginTransaction()
					.add(R.id.fragment_container, fragment)
					.commit();
		}
	}
	
}

