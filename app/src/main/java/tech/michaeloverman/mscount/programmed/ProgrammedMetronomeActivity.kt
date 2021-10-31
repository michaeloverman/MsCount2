///* Copyright (C) 2017 Michael Overman - All Rights Reserved */
//package tech.michaeloverman.mscount.programmed
//
//import android.app.AlertDialog
//import android.content.DialogInterface
//import android.content.Intent
//import android.os.Bundle
//import android.transition.Fade
//import android.transition.TransitionInflater
//import android.view.Menu
//import android.view.MenuItem
//import android.widget.Toast
//import com.firebase.ui.auth.AuthUI
//import com.firebase.ui.auth.AuthUI.IdpConfig.EmailBuilder
//import com.firebase.ui.auth.ErrorCodes
//import com.firebase.ui.auth.IdpResponse
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.FirebaseAuth.AuthStateListener
//import tech.michaeloverman.mscount.BuildConfig
//import tech.michaeloverman.mscount.R
//import tech.michaeloverman.mscount.dataentry.MetaDataEntryFragment
//import tech.michaeloverman.mscount.utils.MetronomeActivity
//import tech.michaeloverman.mscount.utils.PrefUtils
//import timber.log.Timber
//
///**
// * This activity manages the various frgaments involved in the programmed metronome. Particularly
// * the local vs cloud database options menu item, and Firebase signin.
// *
// * Created by Michael on 3/24/2017.
// */
//class ProgrammedMetronomeActivity : MetronomeActivity() {
////    override fun createFragment(): Fragment? {
////        return ProgrammedMetronomeFragment.newInstance()
////    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setupWindowAnimations()
//
////		Intent intent = getIntent();
////		if (intent.hasExtra(PROGRAM_ID_EXTRA)) {
////			Timber.d("PROGRAM ID FROM WIDGET DETECTED: GO, GO, GO!!!");
////			int id = intent.getIntExtra(PROGRAM_ID_EXTRA, 999);
////			PrefUtils.saveWidgetSelectedPieceToPrefs(this, id);
////		}
//    }
//
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        Timber.d("onCreateOptionsMenu")
//        super.onCreateOptionsMenu(menu)
//        val inflater = menuInflater
//        return true
//    }
//
//    public override fun onStart() {
//        super.onStart()
//        Timber.d("onStart()")
//    }
//
//    override fun onResume() {
//        super.onResume()
//        Timber.d("onResume() - firebase: %s", useFirebase)
//    }
//
//    public override fun onStop() {
//        super.onStop()
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        Timber.d("ACTIVITY: onOptionsItemSelected()")
//        return if (item.itemId == R.id.firebase_local_database) {
//            useFirebase = !useFirebase
//            if (useFirebase) {
//                if (mAuth!!.currentUser == null) {
//                    signInToFirebase()
//                }
//            }
//            updateDatabaseOptionMenuItem()
//            true
//        } else {
//            super.onOptionsItemSelected(item)
//        }
//    }
//
//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//    }
//
//    override fun onBackPressed() {
//    }
//
//    private fun setupWindowAnimations() {
//        val slide = TransitionInflater.from(this).inflateTransition(R.transition.activity_fade_enter) as Fade
//        window.enterTransition = slide
//        window.allowEnterTransitionOverlap = true
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        Timber.d("ACTIVITY: onActivityResult()")
//    }
//
//
//}