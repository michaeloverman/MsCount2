/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.utils

import androidx.appcompat.app.AppCompatActivity

/**
 * Created by Michael on 4/4/2017.
 */
class SettingsActivity : AppCompatActivity() {


    override fun onResume() {
        super.onResume()
//        setContentView(R.layout.settings_frament)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

}