/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import timber.log.Timber
import timber.log.Timber.DebugTree

/**
 * Created by Michael on 4/21/2016.
 * Adapted from SingleFragmentActivity from the Big Nerd Ranch Guide
 */
abstract class SingleFragmentActivity : AppCompatActivity() {
    protected abstract fun createFragment(): Fragment?
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            Timber.uprootAll()
            Timber.plant(DebugTree())
        }
        Timber.d("SingleFragmentActivity onCreate()")
        Timber.d("SingleFragment test Timber log statement")
        setContentView(R.layout.activity_fragment)
        val fm = supportFragmentManager
        var fragment = fm.findFragmentById(R.id.fragment_container)
        if (fragment == null) {
            fragment = createFragment()
        }
        fm.beginTransaction()
            .add(R.id.fragment_container, fragment!!)
            .commit()
    }
}