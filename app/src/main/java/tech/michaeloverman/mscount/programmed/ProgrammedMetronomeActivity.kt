/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.programmed

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.transition.Fade
import android.transition.TransitionInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig.EmailBuilder
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import tech.michaeloverman.mscount.BuildConfig
import tech.michaeloverman.mscount.R
import tech.michaeloverman.mscount.dataentry.MetaDataEntryFragment
import tech.michaeloverman.mscount.utils.MetronomeActivity
import tech.michaeloverman.mscount.utils.PrefUtils
import timber.log.Timber

/**
 * This activity manages the various frgaments involved in the programmed metronome. Particularly
 * the local vs cloud database options menu item, and Firebase signin.
 *
 * Created by Michael on 3/24/2017.
 */
class ProgrammedMetronomeActivity : MetronomeActivity() {
    private var mAuth: FirebaseAuth? = null
    private var mAuthListener: AuthStateListener? = null
	var useFirebase = false
    private var databaseMenuItem: MenuItem? = null
    override fun createFragment(): Fragment? {
        return ProgrammedMetronomeFragment.newInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowAnimations()
        useFirebase = PrefUtils.usingFirebase(this)
        mAuth = FirebaseAuth.getInstance()
        mAuthListener = AuthStateListener { firebaseAuth: FirebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User is signed in
                Timber.d("onAuthStateChanged:signed_in: %s", user.uid)
                //                    useFirebase = true;
            } else {
                // User is signed out
                Timber.d("onAuthStateChanged:signed_out")
                goLocal()
            }
        }

//		Intent intent = getIntent();
//		if (intent.hasExtra(PROGRAM_ID_EXTRA)) {
//			Timber.d("PROGRAM ID FROM WIDGET DETECTED: GO, GO, GO!!!");
//			int id = intent.getIntExtra(PROGRAM_ID_EXTRA, 999);
//			PrefUtils.saveWidgetSelectedPieceToPrefs(this, id);
//		}
        if (mAuth!!.currentUser == null) {
            signInToFirebase()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Timber.d("onCreateOptionsMenu")
        super.onCreateOptionsMenu(menu)
        val inflater = menuInflater
        inflater.inflate(R.menu.programmed_global_menu, menu)
        databaseMenuItem = menu.findItem(R.id.firebase_local_database)
        updateDatabaseOptionMenuItem()
        return true
    }

    public override fun onStart() {
        super.onStart()
        Timber.d("onStart()")
        mAuth!!.addAuthStateListener(mAuthListener!!)
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume() - firebase: %s", useFirebase)
        useFirebase = PrefUtils.usingFirebase(this)
        if (databaseMenuItem != null) {
            updateDatabaseOptionMenuItem()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            mAuth!!.removeAuthStateListener(mAuthListener!!)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("ACTIVITY: onOptionsItemSelected()")
        return if (item.itemId == R.id.firebase_local_database) {
            useFirebase = !useFirebase
            if (useFirebase) {
                if (mAuth!!.currentUser == null) {
                    signInToFirebase()
                }
            }
            updateDatabaseOptionMenuItem()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_USE_FIREBASE, useFirebase)
    }

    override fun onBackPressed() {
        val f = this.supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (f is MetaDataEntryFragment) {
            losingDataAlertDialog()
        } else {
            actuallyGoBack()
        }
    }

    private fun actuallyGoBack() {
        super.onBackPressed()
    }

    private fun setupWindowAnimations() {
        val slide = TransitionInflater.from(this).inflateTransition(R.transition.activity_fade_enter) as Fade
        window.enterTransition = slide
        window.allowEnterTransitionOverlap = true
    }

    private fun signInToFirebase() {
        val providers = listOf(
                EmailBuilder().build() //				new AuthUI.IdpConfig.PhoneBuilder().build(),
                //				new AuthUI.IdpConfig.GoogleBuilder().build()
        )
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(!BuildConfig.DEBUG)
                        .setTheme(R.style.AppTheme_FirebaseSignIn)
                        .setAvailableProviders(providers)
                        .build(),
                FIREBASE_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.d("ACTIVITY: onActivityResult()")
        if (requestCode == FIREBASE_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            // Successfully signed in
            if (resultCode == RESULT_OK) {
//                startActivity(SignedInActivity.createIntent(this, response));
//                finish();
                Timber.d("signed into Firebase")
            } else {
                // Sign in failed
                if (response == null) {
                    // User pressed back button
                    showToast(R.string.sign_in_cancelled)
                } else if (response.error!!.errorCode == ErrorCodes.NO_NETWORK) {
                    showToast(R.string.no_internet_connection)
                } else if (response.error!!.errorCode == ErrorCodes.UNKNOWN_ERROR) {
                    showToast(R.string.unknown_error)
                } else {
                    showToast(R.string.unknown_sign_in_response)
                }
                goLocal()
            }
        }
    }

    private fun updateDatabaseOptionMenuItem() {
        Timber.d("updateDatabaseOptionMenuItem")
        PrefUtils.saveFirebaseStatus(this, useFirebase)
        databaseMenuItem?.setTitle(if (useFirebase) R.string.use_local_database else R.string.use_cloud_database)
    }

    private fun goLocal() {
        useFirebase = false
        updateDatabaseOptionMenuItem()
    }

    private fun showToast(message: Int) {
        val m = getString(message)
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
        Timber.d(m)
    }

    private fun losingDataAlertDialog() {
        AlertDialog.Builder(this)
                .setTitle(R.string.erase_data)
                .setMessage(R.string.leave_without_save)
                .setPositiveButton(R.string.leave) { _: DialogInterface?, _: Int -> actuallyGoBack() }
                .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .show()
    }

    companion object {
        private const val FIREBASE_SIGN_IN = 456
        private const val KEY_USE_FIREBASE = "use_firebase_key"
        const val PROGRAM_ID_EXTRA = "program_id_extra_from_widget"
    }
}